#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT
javac -d "$WORK" app/src/main/java/com/motohub/next/{CostTrend,FuelEntryMath,RideMetrics}.java tests/BwImprovementsTest.java
java -cp "$WORK" com.motohub.next.BwImprovementsTest
