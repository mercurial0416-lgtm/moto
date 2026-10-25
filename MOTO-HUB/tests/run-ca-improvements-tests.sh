#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
out="$(mktemp -d)";trap 'rm -rf "$out"' EXIT
javac -encoding UTF-8 -d "$out" \
 app/src/main/java/com/motohub/next/{GarageBikeStatus,MaintenancePreset,TrackFile,RideInsights,RideMotionGate}.java tests/CaImprovementsTest.java
java -cp "$out" com.motohub.next.CaImprovementsTest
python3 tests/test_alpha6_ca_integration.py
