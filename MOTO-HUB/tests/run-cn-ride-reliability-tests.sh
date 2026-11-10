#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
TMP="$(mktemp -d)"; trap 'rm -rf "$TMP"' EXIT
javac -encoding UTF-8 -d "$TMP" \
 app/src/main/java/com/motohub/next/{ObdPidCapabilities,ObdSpeedResponsePolicy,ObdOdometer,TrackFile,TrackRecovery}.java \
 tests/CnRideReliabilityTest.java
java -cp "$TMP" com.motohub.next.CnRideReliabilityTest
python3 tests/test_alpha6_cn_integration.py
python3 tests/test_obd_persistent_ready_wiring.py
python3 tests/test_cardo_autoride_integration.py
python3 tests/test_cardo_reconnect_regression.py
python3 tests/test_v599_gps_fallback.py
