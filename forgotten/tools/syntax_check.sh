#!/usr/bin/env bash
# Parses every Java source in the mod with javac, without Minecraft on the classpath, and fails
# only on syntax errors. Symbol errors are expected here (Minecraft is not available locally)
# and are left to the real build.
set -uo pipefail
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT="$(mktemp -d)"
find "$HERE/mod/src" "$HERE/logic/src/main" -name '*.java' > "$OUT/sources.txt"
javac -proc:none -Xmaxerrs 100000 -d "$OUT/classes" @"$OUT/sources.txt" > "$OUT/log.txt" 2>&1
grep -E "error: (reached end of file|illegal start|';' expected|'\)' expected|'\(' expected|<identifier> expected|class, interface, enum, or record expected|not a statement|unclosed|orphaned|'else' without 'if'|illegal character|'\{' expected|'\}' expected|: expected|-> expected|unexpected type|illegal start of type)" -A2 "$OUT/log.txt" > "$OUT/syntax.txt"
if [ -s "$OUT/syntax.txt" ]; then
    cat "$OUT/syntax.txt"
    echo "syntax errors found"
    exit 1
fi
echo "syntax OK ($(wc -l < "$OUT/sources.txt") files)"
