#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
out="$(mktemp -d)";trap 'rm -rf "$out"' EXIT
javac -encoding UTF-8 -d "$out" \
 app/src/main/java/com/motohub/next/ObdHandsFreePolicy.java \
 app/src/main/java/com/motohub/next/ObdPowerModePolicy.java \
 tests/ObdPowerModePolicyTest.java
java -cp "$out" com.motohub.next.ObdPowerModePolicyTest
