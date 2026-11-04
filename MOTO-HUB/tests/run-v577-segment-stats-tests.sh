#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
javac -encoding UTF-8 -d "$tmp" \
 app/src/main/java/com/motohub/next/TrackFile.java \
 app/src/main/java/com/motohub/next/RideHistoryLabel.java \
 app/src/main/java/com/motohub/next/RideMonthlyReport.java \
 app/src/main/java/com/motohub/next/RideStatsSeries.java \
 tests/Ux123SegmentStatsTest.java
java -cp "$tmp" com.motohub.next.Ux123SegmentStatsTest
