#!/usr/bin/env bash
#
# Fallback build for environments that cannot reach repo.papermc.io.
#
# `mvn package` is the normal way to build this plugin and you should use it. This script exists
# for networks where the Paper Maven repository is blocked but GitHub and Maven Central are not:
# it compiles against Paper's API *sources* from GitHub instead of the published paper-api jar,
# and produces the same jar.
#
# It is slower and needs ~1GB of scratch space. The output is byte-for-byte equivalent in intent:
# the same sources, the same API, the same Java release target.
set -euo pipefail

export JAVA_TOOL_OPTIONS=""

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
WORK="${BUILD_WORK_DIR:-$ROOT/.build-from-source}"
PAPER_BRANCH="${PAPER_BRANCH:-ver/1.21.4}"
VERSION="$(sed -n 's|.*<version>\(1\.[^<]*\)</version>.*|\1|p' "$ROOT/pom.xml" | head -1)"

mkdir -p "$WORK"

if [ ! -d "$WORK/Paper" ]; then
    echo "==> Fetching Paper API sources ($PAPER_BRANCH)"
    git clone --depth 1 --branch "$PAPER_BRANCH" --filter=blob:none --sparse \
        https://github.com/PaperMC/Paper "$WORK/Paper"
    git -C "$WORK/Paper" sparse-checkout set paper-api/src/main/java
fi

if [ ! -d "$WORK/brigadier" ]; then
    echo "==> Fetching Brigadier sources (not published to Maven Central)"
    git clone --depth 1 https://github.com/Mojang/brigadier "$WORK/brigadier"
fi

echo "==> Resolving the API's dependencies from Maven Central"
mvn -q -f "$ROOT/tools/paper-api-deps.xml" dependency:copy-dependencies \
    -DoutputDirectory="$WORK/lib"

echo "==> Compiling"
CLASSES="$WORK/classes"
rm -rf "$CLASSES"
mkdir -p "$CLASSES"
CP="$(find "$WORK/lib" -name '*.jar' | tr '\n' ':')"
find "$ROOT/src/main/java" -name '*.java' > "$WORK/sources.txt"
javac -nowarn -proc:none --release 21 \
      -d "$CLASSES" -cp "$CP" \
      -sourcepath "$WORK/Paper/paper-api/src/main/java:$WORK/brigadier/src/main/java" \
      @"$WORK/sources.txt"

echo "==> Packaging"
STAGE="$WORK/stage"
rm -rf "$STAGE"
mkdir -p "$STAGE"
# Only this plugin's classes. javac also writes the API classes it had to compile from source
# into the output directory; shipping those would shadow the server's own copies.
cp -r "$CLASSES/dev" "$STAGE/"
cp "$ROOT/src/main/resources/config.yml" "$ROOT/src/main/resources/messages.yml" "$STAGE/"
sed "s|\${project\.version}|$VERSION|" "$ROOT/src/main/resources/plugin.yml" > "$STAGE/plugin.yml"

mkdir -p "$ROOT/target"
JAR="$ROOT/target/ScriptedEssentials-$VERSION.jar"
jar --create --file "$JAR" -C "$STAGE" .

echo "==> $JAR"
