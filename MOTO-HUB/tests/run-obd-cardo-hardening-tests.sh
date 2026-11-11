#!/usr/bin/env bash
# Deterministic pure-Java ECU and parking recovery guards, no Android device needed.
set -euo pipefail
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
SRC="app/src/main/java/com/motohub/next"
javac -encoding UTF-8 -d "$TMP" \
  "$SRC/ObdParkingIsolation.java" \
  "$SRC/ObdSpeedResponsePolicy.java" \
  "$SRC/ObdPidCapabilities.java" \
  "$SRC/ObdSpeedSanityPolicy.java" \
  tests/ObdParkingIsolationTest.java \
  tests/ObdSpeedResponseRecoveryTest.java \
  tests/ObdSpeedSanityPolicyTest.java
java -cp "$TMP" com.motohub.next.ObdParkingIsolationTest
java -cp "$TMP" com.motohub.next.ObdSpeedResponseRecoveryTest
java -cp "$TMP" com.motohub.next.ObdSpeedSanityPolicyTest
