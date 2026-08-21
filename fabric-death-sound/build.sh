#!/usr/bin/env bash
#
# Builds the Scripted Death Sound Fabric mod jar.
#
# The mod is resources only - there is no Java to compile - so the build is a
# plain deterministic zip and needs no Gradle, no Loom and no network access.
set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SRC="$HERE/src"
NAME="ScriptedDeathSound"
VERSION="$(sed -n 's/.*"version"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' "$SRC/fabric.mod.json" | head -1)"
OUT="$HERE/dist/$NAME-$VERSION.jar"

command -v zip >/dev/null || { echo "error: 'zip' is required" >&2; exit 1; }

# Every declared sound must actually exist, or the game logs a missing-sound
# warning at load and falls back to silence.
python3 - "$SRC" <<'PY'
import json, os, sys
src = sys.argv[1]
with open(os.path.join(src, "assets/minecraft/sounds.json")) as fh:
    events = json.load(fh)
for event, body in events.items():
    for entry in body["sounds"]:
        name = entry["name"] if isinstance(entry, dict) else entry
        namespace, _, path = name.partition(":")
        if not path:
            namespace, path = "minecraft", namespace
        ogg = os.path.join(src, "assets", namespace, "sounds", path + ".ogg")
        if not os.path.isfile(ogg):
            sys.exit("error: %s references missing sound file %s" % (event, ogg))
        with open(ogg, "rb") as fh:
            if fh.read(4) != b"OggS":
                sys.exit("error: %s is not an Ogg file" % ogg)
print("sounds.json: %d event(s) verified" % len(events))
PY

rm -rf "$HERE/dist"
mkdir -p "$HERE/dist"

# Fixed timestamps + sorted entry order make the jar byte-for-byte reproducible.
find "$SRC" -exec touch -t 202601010000.00 {} +
(cd "$SRC" && find . -mindepth 1 | LC_ALL=C sort | zip -q -X -9 "$OUT" -@)

echo "built $OUT"
unzip -l "$OUT"
