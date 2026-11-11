#!/usr/bin/env bash
set -euo pipefail
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT
P="app/src/main/java/com/motohub/next"
javac -encoding UTF-8 -d "$WORK" \
 "$P/ObdRideAutoGate.java" \
 "$P/ObdOdoPollPolicy.java" \
 "$P/ObdPidCapabilities.java" \
 "$P/ObdReadOnlyProtocol.java" \
 "$P/ObdOdometer.java" \
 "$P/RideMotionGate.java" \
 "$P/RideReplayTimeline.java" \
 "$P/MapPathDetailPolicy.java" \
 tests/Moto30CoreRegressionTest.java \
 tests/MapPathDetailPolicyTest.java
java -cp "$WORK" com.motohub.next.Moto30CoreRegressionTest
java -cp "$WORK" com.motohub.next.MapPathDetailPolicyTest
