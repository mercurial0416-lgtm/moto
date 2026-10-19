#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
out="$(mktemp -d)"
trap 'rm -rf "$out"' EXIT
javac -encoding UTF-8 -d "$out" \
 app/src/main/java/com/motohub/next/TrackFile.java \
 app/src/main/java/com/motohub/next/RideHistoryLabel.java \
 app/src/main/java/com/motohub/next/RideHistoryFilter.java \
 app/src/main/java/com/motohub/next/RideHistorySort.java \
 tests/RideHistorySortTest.java
java -cp "$out" com.motohub.next.RideHistorySortTest
python3 - <<'PY'
from pathlib import Path
ui=Path('app/src/main/java/com/motohub/next/RideUi.java').read_text()
activity=Path('app/src/main/java/com/motohub/next/MainActivity.java').read_text()
assert 'RideHistorySort.order(RideHistoryFilter.select(' in ui
assert 'setSingleChoiceItems(options,checked,' in ui
assert 'a.selectRideHistorySort(options[which]);' in ui
assert 'sortButton.setContentDescription(' in ui
assert 'void selectRideHistorySort(String value)' in activity
assert 'out.putString("rideHistorySort",rideHistorySort)' in activity
assert 'rideHistorySort=RideHistorySort.restore(getSharedPreferences("ride_history_ui",MODE_PRIVATE).getString("sort",RideHistorySort.NEWEST),state==null?null:state.getString("rideHistorySort"))' in activity
assert 'getSharedPreferences("ride_history_ui",MODE_PRIVATE).edit().putString("sort",normalized).apply()' in activity
assert 'updateRideHistoryControls(rideHistoryFilter,normalized)' in activity
assert 'view.scrollTo(0,restoreY)' in activity
assert 'Sharing.start(' not in ui
print('PASS: sorting UI, rotation, scroll, accessibility, no implicit location sharing')
PY
