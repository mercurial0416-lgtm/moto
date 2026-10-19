#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
out="$(mktemp -d)"
trap 'rm -rf "$out"' EXIT
javac -encoding UTF-8 -d "$out" \
 app/src/main/java/com/motohub/next/TrackFile.java \
 app/src/main/java/com/motohub/next/RideHistoryLabel.java \
 app/src/main/java/com/motohub/next/RideBikeBinding.java \
 app/src/main/java/com/motohub/next/RideReportScope.java \
 app/src/main/java/com/motohub/next/RideMonthlyReport.java \
 tests/RideReportScopeTest.java
java -cp "$out" com.motohub.next.RideReportScopeTest
python3 - <<'PYTEST'
from pathlib import Path
s=Path('app/src/main/java/com/motohub/next/RideUi.java').read_text()
assert 'rideReport(scope,choices[which])' in s
assert 'RideHistorySort.order(TrackFile.tracks' in s
assert 'RideReportScope.include(track,bikeScope)' in s
assert '분석 제한(최신 150건)' in s
assert 'Sharing.start(' not in s
print('PASS: report UI wiring, bounded analysis and no automatic sharing')
PYTEST
