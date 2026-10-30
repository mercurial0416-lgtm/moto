#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
out="$(mktemp -d)"
trap 'rm -rf "$out"' EXIT
javac -encoding UTF-8 -d "$out" app/src/main/java/com/motohub/next/RideReadiness.java tests/RideReadinessTest.java
java -cp "$out" com.motohub.next.RideReadinessTest
python3 - <<'PY'
from pathlib import Path
ui=Path('app/src/main/java/com/motohub/next/RideUi.java').read_text()
assert 'RideReadiness.State state=readiness();' in ui
assert 'LinearLayout warning=a.col()' not in ui
assert '라이딩 사전 점검' not in ui
assert 'if(readiness()!=RideReadiness.State.READY)' in ui
assert 'Settings.ACTION_LOCATION_SOURCE_SETTINGS' in ui
assert 'Settings.ACTION_APPLICATION_DETAILS_SETTINGS' in ui
assert 'RideService.status' in ui
assert 'if(state==RideReadiness.State.GPS_DISABLED||state==RideReadiness.State.NOTIFICATIONS_DISABLED)' in ui
assert 'setPositiveButton("이 바이크로 기록 시작"' in ui
assert 'String liveBike=a.bike()==null?"":a.bike().optString("id");' in ui
assert 'RideReadiness.State state=readiness();' in ui
activity=Path('app/src/main/java/com/motohub/next/MainActivity.java').read_text()
assert 'onResume(){super.onResume();' in activity
assert '"라이딩".equals(tab)' in activity
print('PASS: required GPS readiness enforcement, explicit consent and error status; no redundant checklist')
PY
