#!/usr/bin/env bash
#
# Compiles and runs the unit tests against the local API stubs.
#
# The tests cover the pure logic that needs no server: duration parsing, text formatting, the
# chat filter's matching and censoring, command name normalisation. `mvn test` runs the same
# tests against the real paper-api; this script exists for environments that cannot reach
# repo.papermc.io.
set -euo pipefail

export JAVA_TOOL_OPTIONS=""

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
OUT="$ROOT/tools/apicheck/build"
LIB="$ROOT/tools/apicheck/lib"

"$ROOT/tools/apicheck/check.sh" > /dev/null

mkdir -p "$OUT/test-classes"
CP="$(find "$LIB" -name '*.jar' | tr '\n' ':')$OUT/classes"

find "$ROOT/core/src/test/java" -name '*.java' > "$OUT/test-sources.txt"
javac -nowarn -proc:none -d "$OUT/test-classes" -cp "$CP" @"$OUT/test-sources.txt"

java -jar "$(find "$LIB" -name 'junit-platform-console-standalone-*.jar' | head -1)" \
     execute \
     --class-path "$CP:$OUT/test-classes" \
     --scan-class-path "$OUT/test-classes" \
     --details=tree \
     --disable-banner
