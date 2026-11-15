#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
javac -encoding UTF-8 -d "$tmp" \
 app/src/main/java/com/motohub/next/ObdAutoStartPreflight.java \
 tests/ObdAutoStartPreflightTest.java
java -cp "$tmp" com.motohub.next.ObdAutoStartPreflightTest
python3 - <<'PY'
from pathlib import Path
s=Path('app/src/main/java/com/motohub/next/RideService.java').read_text(encoding='utf-8')
start=s.split('private void handleAutoSpeed(int speedKmh){',1)[1].split('/** A full-track readback',1)[0]
assert 'ObdAutoStartPreflight.decide(' in start
assert 'ObdAutoStartPreflight.Decision.WAIT_GPS' in start
assert 'pauseObdForGpsOff();' in start
assert 'ObdHandsFreeSession.disable(this);' in start, 'failed GPS startup must not silently leave consent armed'
assert 'GPS_PROVIDER' in start
assert 'openRideTrack();requestGpsUpdatesChecked();' in start
request=s.split('private void requestGpsUpdatesChecked(){',1)[1].split('private void prepareForeground(',1)[0]
assert 'isProviderEnabled(LocationManager.GPS_PROVIDER)' in request
print('PASS auto GPS start provider and fail-closed guards')
PY
