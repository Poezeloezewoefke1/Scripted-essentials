#!/usr/bin/env bash
#
# Type-checks src/main/java against the local API stubs.
#
# This is a development aid, not the build. The real build is `mvn package`, which compiles
# against the actual paper-api. Use this when the Paper repository is unreachable.
set -euo pipefail

# javac needs no network; drop the proxy/truststore banner for readable output.
export JAVA_TOOL_OPTIONS=""

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
OUT="$ROOT/tools/apicheck/build"

rm -rf "$OUT"
mkdir -p "$OUT/classes"

CP="$(find "$ROOT/tools/apicheck/lib" -name '*.jar' | tr '\n' ':')"

find "$ROOT/src/main/java" "$ROOT/tools/apicheck/stubs" -name '*.java' > "$OUT/sources.txt"

javac -nowarn -proc:none \
      -d "$OUT/classes" \
      -cp "$CP" \
      @"$OUT/sources.txt"

echo "OK: $(wc -l < "$OUT/sources.txt") sources type-checked."
