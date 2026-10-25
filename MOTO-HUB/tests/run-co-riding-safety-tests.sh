#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
javac -encoding UTF-8 -d "$TMP" \
 app/src/main/java/com/motohub/next/{ObdReadOnlyProtocol,ObdSensorReading,ObdPidCapabilities,BootRidePolicy,TrackFile,RideMotionGate,RideNotificationRate}.java \
 tests/CoRidingSafetyTest.java
java -cp "$TMP" com.motohub.next.CoRidingSafetyTest
