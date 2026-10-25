#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
work="$(mktemp -d)";trap 'rm -rf "$work"' EXIT
javac -encoding UTF-8 -d "$work" app/src/main/java/com/motohub/next/{FuelRecordValidation,CostDashboard,RideMonthlyReport,RideHistoryLabel,TrackFile}.java tests/BxHardeningTest.java
java -cp "$work" com.motohub.next.BxHardeningTest
