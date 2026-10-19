#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
out="$(mktemp -d)";trap 'rm -rf "$out"' EXIT
javac -encoding UTF-8 -d "$out" app/src/main/java/com/motohub/next/RestoreRideGate.java tests/RestoreRideGateTest.java
java -cp "$out" com.motohub.next.RestoreRideGateTest
python3 tests/test_alpha6_ch_restore_integration.py
