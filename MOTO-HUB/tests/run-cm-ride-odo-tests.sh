#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
TMP="$(mktemp -d)";trap 'rm -rf "$TMP"' EXIT
javac -encoding UTF-8 -d "$TMP" \
 app/src/main/java/com/motohub/next/{TrackFile,BootRidePolicy,ObdPidCapabilities,ObdReadOnlyProtocol,ObdTelemetryFreshness,ObdEngineState,ObdOdometer}.java \
 tests/CmOdoAndRecoveryTest.java
java -cp "$TMP" com.motohub.next.CmOdoAndRecoveryTest
python3 tests/test_alpha6_cm_integration.py
