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
assert 'RideReadiness.State readiness=readiness();' in ui
assert 'RideReadiness.title(readiness)' in ui
assert 'RideReadiness.guidance(readiness)' in ui
assert 'RideReadiness.action(readiness)' in ui
assert 'Settings.ACTION_LOCATION_SOURCE_SETTINGS' in ui
assert 'Settings.ACTION_APPLICATION_DETAILS_SETTINGS' in ui
assert 'RideService.status' in ui
assert 'if(state==RideReadiness.State.GPS_DISABLED||state==RideReadiness.State.NOTIFICATIONS_DISABLED)' in ui
assert 'setPositiveButton("동의하고 시작"' in ui
activity=Path('app/src/main/java/com/motohub/next/MainActivity.java').read_text()
assert 'onResume(){super.onResume();' in activity
assert '"라이딩".equals(tab)' in activity
print('PASS: GPS readiness card, settings links, explicit consent and error status wired')
PY
