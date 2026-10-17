#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
out="$(mktemp -d)"
trap 'rm -rf "$out"' EXIT
javac -encoding UTF-8 -d "$out" app/src/main/java/com/motohub/next/RideHistorySearch.java tests/RideHistorySearchTest.java
java -cp "$out" com.motohub.next.RideHistorySearchTest
python3 - <<'PY'
from pathlib import Path
ui=Path('app/src/main/java/com/motohub/next/RideUi.java').read_text()
activity=Path('app/src/main/java/com/motohub/next/MainActivity.java').read_text()
hub=Path('app/src/main/java/com/motohub/next/HubScreens.java').read_text()
assert 'search.addTextChangedListener(' in ui
assert 'showHistoryResults(results,entries,rides.size(),selected,summary)' in ui
assert 'new HubScreens(a).line(results,date,detail,' in ui
assert 'RideHistorySearch.matches(entry.date,entry.status,query)' in ui
assert 'RideHistorySearch.MAX_LENGTH' in ui
assert 'setContentDescription("라이딩 기록 검색")' in ui
assert 'setContentDescription("라이딩 검색어 지우기")' in ui
assert 'rideHistoryQuery=RideHistorySearch.limit(state.getString("rideHistoryQuery",""))' in activity
assert 'out.putString("rideHistoryQuery",rideHistoryQuery)' in activity
assert 'void line(LinearLayout target,String title,String detail,String value,Runnable click)' in hub
assert 'target.addView(block)' in hub
assert 'Sharing.start(' not in ui and 'SocialClient' not in ui
print('PASS: live search, clear button, list container, rotation and sharing guard wiring')
PY
