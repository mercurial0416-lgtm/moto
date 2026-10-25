#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
DIR="$(mktemp -d)"
trap 'rm -rf "$DIR"' EXIT
javac -encoding UTF-8 -d "$DIR" app/src/main/java/com/motohub/next/RideSessionInterlock.java tests/RideSessionInterlockTest.java
java -cp "$DIR" com.motohub.next.RideSessionInterlockTest
python3 tests/test_alpha6_cl_integration.py
