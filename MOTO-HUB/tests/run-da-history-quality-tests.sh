#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
out="$(mktemp -d)"
trap 'rm -rf "$out"' EXIT
javac -encoding UTF-8 -d "$out" \
 app/src/main/java/com/motohub/next/TrackFile.java \
 app/src/main/java/com/motohub/next/RideHistoryLabel.java \
 app/src/main/java/com/motohub/next/RideHistoryPeriod.java \
 app/src/main/java/com/motohub/next/RideHistoryPage.java \
 app/src/main/java/com/motohub/next/RideBikeBinding.java \
 app/src/main/java/com/motohub/next/RideHistoryBikeLabel.java \
 tests/RideHistoryPeriodTest.java tests/RideHistoryPageTest.java tests/RideHistoryBikeLabelTest.java
java -cp "$out" com.motohub.next.RideHistoryPeriodTest
java -cp "$out" com.motohub.next.RideHistoryPageTest
java -cp "$out" com.motohub.next.RideHistoryBikeLabelTest
python3 - <<'PY'
from pathlib import Path
ui=Path('app/src/main/java/com/motohub/next/RideUi.java').read_text()
assert 'RideHistoryBikeLabel.label(file,bikeNames)' in ui
assert 'RideHistoryPeriod.OPTIONS' in ui
assert 'Sharing.start(' not in ui
print('PASS: local-only UI caption, date picker, no automatic sharing')
PY
