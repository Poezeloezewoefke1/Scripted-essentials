#!/usr/bin/env python3
"""Static checks that run without Minecraft.

    python3 forgotten/tools/validate.py --vanilla REGISTRIES_JSON [--yarn YARN_MAPPINGS_DIR] [--fabric FABRIC_SRC_DIR]

* Resources: every model, texture, blockstate and item definition reference resolves; every
  registered item and block has a name in en_us.json; recipes, loot tables and Paradox recipes
  only name items that exist (vanilla, from a registries dump such as misode/mcmeta's
  "summary/registries/data.json" for 1.21.11, or this mod's own).
* Java (optional): every imported net.minecraft class exists in the given Yarn mappings, and
  every imported net.fabricmc.fabric.api class exists in the given Fabric API source tree.

These catch the mistakes that would otherwise only show up as purple-and-black textures,
missing names or a recipe silently failing to load.
"""
import argparse
import glob
import json
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(HERE, '..', 'mod', 'src', 'main', 'resources')
JAVA = [os.path.join(HERE, '..', 'mod', 'src', 'main', 'java'), os.path.join(HERE, '..', 'mod', 'src', 'client', 'java')]
NS = 'forgotten'
errors = []


def err(msg):
    errors.append(msg)


def load(path):
    with open(path) as f:
        return json.load(f)


def mod_registered():
    """Item and block ids registered in Java: item("x", ...), block("x", ...), concept items."""
    items, blocks = set(), set()
    for root in JAVA:
        for path in glob.glob(os.path.join(root, '**', '*.java'), recursive=True):
            src = open(path).read()
            for m in re.finditer(r'\bitem\("([a-z0-9_]+)"', src):
                items.add(m.group(1))
            for m in re.finditer(r'\bblock\("([a-z0-9_]+)"', src):
                blocks.add(m.group(1))
            for m in re.finditer(r'\bspawnEgg\("([a-z0-9_]+)"', src):
                items.add(m.group(1) + '_spawn_egg')
    concept_src = open(os.path.join(HERE, '..', 'logic', 'src', 'main', 'java', 'dev', 'scripted', 'forgotten', 'logic',
                                    'concept', 'Concept.java')).read()
    for m in re.finditer(r'^\s{4}([A-Z_]+)\(', concept_src, re.M):
        items.add('concept_' + m.group(1).lower())
    return items, blocks


def resource_exists(kind, ident, ext):
    ns, path = ident.split(':', 1) if ':' in ident else ('minecraft', ident)
    if ns == 'minecraft':
        return True  # vanilla assets are not in this jar
    return os.path.exists(os.path.join(RES, 'assets', ns, kind, path + ext))


def check_assets(items, blocks):
    lang = load(os.path.join(RES, 'assets', NS, 'lang', 'en_us.json'))
    for i in items:
        if i in blocks:
            continue
        if f'item.{NS}.{i}' not in lang:
            err(f'no English name for item {NS}:{i}')
        if not os.path.exists(os.path.join(RES, 'assets', NS, 'items', i + '.json')):
            err(f'no item definition for {NS}:{i}')
    for b in blocks:
        if f'block.{NS}.{b}' not in lang:
            err(f'no English name for block {NS}:{b}')
        if not os.path.exists(os.path.join(RES, 'assets', NS, 'blockstates', b + '.json')):
            err(f'no blockstate for {NS}:{b}')
    for path in glob.glob(os.path.join(RES, 'assets', NS, 'blockstates', '*.json')):
        for model in re.findall(r'"model":\s*"([^"]+)"', open(path).read()):
            if not resource_exists('models', model, '.json'):
                err(f'{os.path.basename(path)} -> missing model {model}')
    for path in glob.glob(os.path.join(RES, 'assets', NS, 'items', '*.json')):
        for model in re.findall(r'"model":\s*"([^"]+)"', open(path).read()):
            if not resource_exists('models', model, '.json'):
                err(f'items/{os.path.basename(path)} -> missing model {model}')
    for path in glob.glob(os.path.join(RES, 'assets', NS, 'models', '**', '*.json'), recursive=True):
        m = load(path)
        parent = m.get('parent')
        if parent and not resource_exists('models', parent, '.json'):
            err(f'{path} -> missing parent {parent}')
        for tex in (m.get('textures') or {}).values():
            if tex.startswith('#'):
                continue
            if not resource_exists('textures', tex, '.png'):
                err(f'{os.path.relpath(path, RES)} -> missing texture {tex}')


def items_in(obj):
    """Every string that looks like an item id in the places recipes and loot tables put them."""
    out = []
    if isinstance(obj, dict):
        for k, v in obj.items():
            if k in ('item', 'id', 'name') and isinstance(v, str) and ':' in v and not v.startswith('#'):
                out.append(v)
            elif k in ('key',) and isinstance(v, dict):
                out += [x for x in v.values() if isinstance(x, str) and not x.startswith('#')]
            elif k in ('ingredients',) and isinstance(v, list):
                out += [x for x in v if isinstance(x, str) and not x.startswith('#')]
            elif k in ('ingredient', 'subject', 'contradiction', 'catalyst') and isinstance(v, str) and not v.startswith('#'):
                out.append(v)
            else:
                out += items_in(v)
    elif isinstance(obj, list):
        for v in obj:
            out += items_in(v)
    return out


def check_data(items, blocks, vanilla_items):
    known = {f'minecraft:{i}' for i in vanilla_items} | {f'{NS}:{i}' for i in items | blocks}
    entity_keys = {'memory', 'type', 'function', 'condition', 'trigger', 'formula', 'enchantment', 'random_sequence'}
    for kind in ('recipe', 'loot_table', 'forgotten_paradox'):
        for path in glob.glob(os.path.join(RES, 'data', NS, kind, '**', '*.json'), recursive=True):
            for ident in items_in(load(path)):
                if ident.startswith('minecraft:') and ident.split(':')[1] in entity_keys:
                    continue
                if ident not in known and not re.match(r'^minecraft:(crafting|smelting|blasting|block|item|alternatives|set_count|apply_bonus|explosion_decay|uniform|survives_explosion|random_chance|match_tool|ore_drops|fortune|silk_touch)', ident):
                    err(f'{os.path.relpath(path, RES)} names unknown item {ident}')
    for path in glob.glob(os.path.join(RES, 'data', NS, 'advancement', '*.json')):
        adv = load(path)
        icon = adv.get('display', {}).get('icon', {}).get('id')
        if icon and icon not in known:
            err(f'advancement {os.path.basename(path)} icon {icon} unknown')
        parent = adv.get('parent')
        if parent and not os.path.exists(os.path.join(RES, 'data', NS, 'advancement', parent.split(':')[1] + '.json')):
            err(f'advancement {os.path.basename(path)} parent {parent} missing')


def check_java(yarn_dir, fabric_dir):
    yarn_classes = set()
    if yarn_dir:
        for path in glob.glob(os.path.join(yarn_dir, '**', '*.mapping'), recursive=True):
            stack = []
            for line in open(path):
                depth = len(line) - len(line.lstrip('\t'))
                parts = line.split()
                if not parts or parts[0] != 'CLASS':
                    continue
                name = parts[2] if len(parts) > 2 else parts[1]
                while stack and stack[-1][0] >= depth:
                    stack.pop()
                full = (stack[-1][1] + '$' + name) if stack else name
                stack.append((depth, full))
                yarn_classes.add(full.replace('/', '.').replace('$', '.'))
    fabric_classes = set()
    if fabric_dir:
        for path in glob.glob(os.path.join(fabric_dir, '**', 'src', '*', 'java', 'net', 'fabricmc', 'fabric', 'api', '**', '*.java'), recursive=True):
            rel = path.split(os.sep + 'java' + os.sep, 1)[1][:-5]
            fabric_classes.add(rel.replace(os.sep, '.'))
            for m in re.finditer(r'^\s*(?:public\s+)?(?:static\s+)?(?:final\s+)?(?:abstract\s+)?(?:interface|class|record|enum)\s+(\w+)', open(path).read(), re.M):
                fabric_classes.add(rel.replace(os.sep, '.').rsplit('.', 1)[0] + '.' + rel.rsplit(os.sep, 1)[-1] + '.' + m.group(1))
    for root in JAVA:
        for path in glob.glob(os.path.join(root, '**', '*.java'), recursive=True):
            for m in re.finditer(r'^import (static )?([\w.]+);', open(path).read(), re.M):
                name = m.group(2)
                if m.group(1):
                    name = name.rsplit('.', 1)[0]
                if yarn_dir and name.startswith('net.minecraft.') and name not in yarn_classes:
                    err(f'{os.path.relpath(path)}: import {name} not in Yarn 1.21.11')
                if fabric_dir and name.startswith('net.fabricmc.fabric.api.') and name not in fabric_classes:
                    err(f'{os.path.relpath(path)}: import {name} not in Fabric API source')


LOGIC = os.path.join(HERE, '..', 'logic', 'src', 'main', 'java')


def check_own_symbols():
    """References like Insight.FOO or BioNodes.bar( to the project's own classes must name
    something that class's source declares. Catches the compile errors no stub-free local
    build can: a missing enum constant, a renamed helper."""
    sources = {}
    for root in JAVA + [LOGIC]:
        for path in glob.glob(os.path.join(root, '**', '*.java'), recursive=True):
            name = os.path.basename(path)[:-5]
            pkg = os.path.relpath(os.path.dirname(path), root).replace(os.sep, '.')
            sources.setdefault(name, []).append((pkg + '.' + name, open(path).read()))
    tokens = {n: set(re.findall(r'\b\w+\b', v[0][1])) for n, v in sources.items() if len(v) == 1}
    fq = {n: v[0][0] for n, v in sources.items() if len(v) == 1}
    for root in JAVA:
        for path in glob.glob(os.path.join(root, '**', '*.java'), recursive=True):
            src = open(path).read()
            pkg = os.path.relpath(os.path.dirname(path), root).replace(os.sep, '.')
            imported = set(re.findall(r'^import ([\w.]+);', src, re.M))
            body = re.sub(r'"(?:\\.|[^"\\])*"', '""', src)
            body = re.sub(r'//[^\n]*|/\*.*?\*/', '', body, flags=re.S)
            for m in re.finditer(r'(?<![\w.])([A-Z]\w*)\.([a-zA-Z_]\w*)\b', body):
                cls, member = m.group(1), m.group(2)
                if cls not in fq or member in ('class', 'values', 'valueOf'):
                    continue
                visible = fq[cls] in imported or fq[cls].rsplit('.', 1)[0] == pkg or re.search(
                    r'\b' + re.escape(fq[cls]) + r'\b', src)
                if visible and member not in tokens[cls]:
                    err(f'{os.path.relpath(path)}: {cls}.{member} is not declared in {fq[cls]}')


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--vanilla', required=True)
    ap.add_argument('--yarn')
    ap.add_argument('--fabric')
    args = ap.parse_args()
    vanilla = load(args.vanilla)
    items, blocks = mod_registered()
    check_assets(items, blocks)
    check_data(items, blocks, set(vanilla['item']))
    if args.yarn or args.fabric:
        check_java(args.yarn, args.fabric)
    check_own_symbols()
    for e in errors:
        print('ERROR', e)
    print(f'{len(items)} items, {len(blocks)} blocks checked; {len(errors)} problem(s)')
    sys.exit(1 if errors else 0)


if __name__ == '__main__':
    main()
