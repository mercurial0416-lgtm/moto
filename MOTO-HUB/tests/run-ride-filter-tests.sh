#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
out="$(mktemp -d)"
trap 'rm -rf "$out"' EXIT
javac -encoding UTF-8 -d "$out" app/src/main/java/com/motohub/next/RideHistoryFilter.java tests/RideHistoryFilterTest.java
java -cp "$out" com.motohub.next.RideHistoryFilterTest
python3 - <<'PY'
from pathlib import Path
ui=Path('app/src/main/java/com/motohub/next/RideUi.java').read_text()
activity=Path('app/src/main/java/com/motohub/next/MainActivity.java').read_text()
assert 'HorizontalScrollView strip=new HorizontalScrollView(a);' in ui
assert 'RideHistoryFilter.select(rides,selected,activeName,recording)' in ui
assert 'RideHistoryFilter.status(file,activeName,recording)' in ui
assert 'a.selectRideHistoryFilter(option);' in ui
assert 'void selectRideHistoryFilter(String value)' in activity
assert 'view.scrollTo(0,restoreY)' in activity
assert 'total==0?' in ui and 'entries.isEmpty()?' in ui and 'showHistoryResults(results,entries,rides.size(),selected,summary)' in ui
assert 'chip.setContentDescription(' in ui
assert 'rideHistoryFilter=RideHistoryFilter.restore(getSharedPreferences("ride_history_ui",MODE_PRIVATE).getString("filter",RideHistoryFilter.ALL),state==null?null:state.getString("rideHistoryFilter"))' in activity
assert 'getSharedPreferences("ride_history_ui",MODE_PRIVATE).edit().putString("filter",normalized).apply()' in activity
assert 'updateRideHistoryControls(normalized,rideHistorySort)' in activity
assert 'out.putString("rideHistoryFilter",rideHistoryFilter)' in activity
assert 'Sharing.start(' not in ui
print('PASS: filter UI, empty state, accessibility, persisted filter and no implicit sharing')
PY
