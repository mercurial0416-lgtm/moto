#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
bin=$(mktemp -d)
trap 'rm -rf "$bin"' EXIT
javac -encoding UTF-8 -d "$bin" app/src/main/java/com/motohub/next/TrackFile.java app/src/main/java/com/motohub/next/RideInsights.java app/src/main/java/com/motohub/next/RideMotionGate.java app/src/main/java/com/motohub/next/GpxImporter.java tests/RideAndGpxTest.java
java -cp "$bin" com.motohub.next.RideAndGpxTest
