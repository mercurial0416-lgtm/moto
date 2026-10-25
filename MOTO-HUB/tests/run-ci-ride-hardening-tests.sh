#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
classes="$(mktemp -d)"
trap 'rm -rf "$classes"' EXIT
javac -encoding UTF-8 -d "$classes" \
  app/src/main/java/com/motohub/next/{TrackFile,InterruptedRideNotice,RideNotificationRate,ObdConnectionPolicy}.java \
  tests/CiRideSafetyTest.java
java -cp "$classes" com.motohub.next.CiRideSafetyTest
python3 tests/test_alpha6_ci_integration.py
