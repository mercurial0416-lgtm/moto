#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
out="$(mktemp -d)"; trap 'rm -rf "$out"' EXIT
javac -encoding UTF-8 -d "$out" \
  app/src/main/java/com/motohub/next/ObdRideAutoGate.java \
  app/src/main/java/com/motohub/next/ObdReadOnlyProtocol.java \
  tests/ObdRideAutoGateTest.java tests/ObdReadOnlyProtocolTest.java
java -cp "$out" com.motohub.next.ObdRideAutoGateTest
java -cp "$out" com.motohub.next.ObdReadOnlyProtocolTest
