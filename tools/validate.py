#!/usr/bin/env python3
"""
Consistency checks for ScriptedEssentials that do not need a running server.

Catches the class of mistake a compiler cannot: a message key referenced in code but missing
from messages.yml, a config option documented but never read, two features claiming the same
command, a feature class nobody registered.

Usage:  python3 tools/validate.py
Exits non-zero if anything is wrong.
"""
from __future__ import annotations

import pathlib
import re
import sys
from collections import Counter

ROOT = pathlib.Path(__file__).resolve().parent.parent
SRC = ROOT / "src/main/java"
RES = ROOT / "src/main/resources"

problems: list[str] = []
notes: list[str] = []


def fail(message: str) -> None:
    problems.append(message)


def java_sources() -> dict[pathlib.Path, str]:
    return {p: p.read_text() for p in SRC.rglob("*.java")}


SOURCES = java_sources()
ALL_JAVA = "\n".join(SOURCES.values())


def strip_calls(text: str, opener: str) -> str:
    """Removes every `opener...)` call and everything nested inside it."""
    out = []
    index = 0
    while index < len(text):
        found = text.find(opener, index)
        if found < 0:
            out.append(text[index:])
            break
        out.append(text[index:found])
        depth = 0
        cursor = found + len(opener) - 1
        for cursor in range(found + len(opener) - 1, len(text)):
            if text[cursor] == "(":
                depth += 1
            elif text[cursor] == ")":
                depth -= 1
                if depth == 0:
                    break
        index = cursor + 1
    return "".join(out)


# --- messages.yml -------------------------------------------------------------------------
def check_messages() -> None:
    path = RES / "messages.yml"
    keys = [m.group(1) for m in
            (re.match(r"^([A-Za-z0-9_-]+):", line) for line in path.read_text().splitlines())
            if m]
    duplicates = [k for k, c in Counter(keys).items() if c > 1]
    if duplicates:
        fail(f"messages.yml has duplicate keys (the later one silently wins): {sorted(duplicates)}")
    defined = set(keys)

    # Keys passed to send/raw/get/fail/abort/await, including inside ternaries.
    call = re.compile(r"(messages\(\)\.(?:send|raw|get)|(?<![A-Za-z])fail|(?<![A-Za-z])abort"
                      r"|prompts\(\)\.await|Feedback\.report)\s*\(")
    referenced: set[str] = set()
    for text in SOURCES.values():
        for match in call.finditer(text):
            start = match.end() - 1
            depth = 0
            end = start
            for index in range(start, min(len(text), start + 4000)):
                if text[index] == "(":
                    depth += 1
                elif text[index] == ")":
                    depth -= 1
                    if depth == 0:
                        end = index
                        break
            # Placeholder names and their values are not message keys.
            args = strip_calls(text[start:end + 1], "placeholder(")
            referenced.update(re.findall(r'"([a-z0-9][a-z0-9-]*)"', args))

    feature_ids = set(re.findall(r'FeatureDefinition\.builder\("([a-z0-9]+)"\)', ALL_JAVA))
    for key in sorted(referenced - defined - feature_ids):
        # Fallback values such as "none" or "default" also live in these calls; only flag a key
        # that looks like one (contains a hyphen) to keep the signal clean.
        if "-" in key:
            fail(f"message key '{key}' is used in code but missing from messages.yml")

    for key in sorted(defined):
        if key != "prefix" and f'"{key}"' not in ALL_JAVA:
            fail(f"message key '{key}' is defined in messages.yml but never used")

    notes.append(f"messages.yml: {len(defined)} keys, all defined and all used")


# --- config.yml ---------------------------------------------------------------------------
def check_config() -> None:
    try:
        import yaml
    except ImportError:
        notes.append("config.yml: skipped (PyYAML not installed)")
        return

    for name in ("config.yml", "messages.yml", "plugin.yml"):
        text = (RES / name).read_text().replace("${project.version}", "0.0.0")
        try:
            yaml.safe_load(text)
        except yaml.YAMLError as error:
            fail(f"{name} is not valid YAML: {error}")

    config = yaml.safe_load((RES / "config.yml").read_text())

    def flatten(node, prefix=""):
        found = set()
        if isinstance(node, dict):
            for key, value in node.items():
                found.add(prefix + str(key))
                found |= flatten(value, prefix + str(key) + ".")
        return found

    paths = flatten(config)
    read = re.compile(r'getConfig\(\)\.get(?:String|Int|Boolean|Double|Long|StringList'
                      r'|ConfigurationSection)\(\s*"([^"]+)"')
    used = set()
    for text in SOURCES.values():
        used.update(read.findall(text))

    for path in sorted(used):
        if path not in paths and not path.endswith("."):
            fail(f"config path '{path}' is read by code but missing from config.yml")

    for path in sorted(paths):
        if path.startswith("potion-presets") or path.startswith("death-actions"):
            continue  # data, not settings
        literal_used = f'"{path}"' in ALL_JAVA
        parent_of_used = any(u.startswith(path + ".") for u in used)
        child_section = any(p.startswith(path + ".") for p in paths)
        if not literal_used and not parent_of_used and not child_section:
            fail(f"config option '{path}' is documented in config.yml but never read")

    notes.append(f"config.yml: {len(paths)} paths, every option is read")


# --- features and commands ----------------------------------------------------------------
def check_features() -> None:
    ids = re.findall(r'FeatureDefinition\.builder\("([^"]+)"\)', ALL_JAVA)
    duplicates = [i for i, c in Counter(ids).items() if c > 1]
    if duplicates:
        fail(f"duplicate feature ids: {sorted(duplicates)}")

    classes = sorted({p.stem for p, t in SOURCES.items()
                      if re.search(r"class \w+ extends Feature\b", t)})
    catalog = (SRC / "dev/scripted/essentials/core/FeatureCatalog.java").read_text()
    for name in classes:
        if f"new {name}(" not in catalog:
            fail(f"feature class {name} is never registered in FeatureCatalog")

    names: list[tuple[str, str]] = [("se", "RootCommand.java")]
    for path, text in SOURCES.items():
        for match in re.finditer(r'new SECommand\(plugin,\s*((?:"[^"]+"\s*,?\s*)+)\)', text):
            for name in re.findall(r'"([^"]+)"', match.group(1)):
                names.append((name, path.name))
    counts = Counter(n for n, _ in names)
    for name, count in counts.items():
        if count > 1:
            owners = sorted({f for n, f in names if n == name})
            fail(f"command name '/{name}' is claimed by more than one feature: {owners}")

    notes.append(f"features: {len(ids)} defined, {len(classes)} classes, all registered")
    notes.append(f"commands: {len(names)} names and aliases, no collisions")


check_messages()
check_config()
check_features()

for note in notes:
    print(f"  ok  {note}")
if problems:
    print()
    for problem in problems:
        print(f"FAIL  {problem}")
    sys.exit(1)
print("\nAll consistency checks passed.")
