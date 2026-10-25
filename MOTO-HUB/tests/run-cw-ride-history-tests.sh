#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
out="$(mktemp -d)"
trap 'rm -rf "$out"' EXIT
javac -encoding UTF-8 -d "$out" \
  app/src/main/java/com/motohub/next/TrackFile.java \
  app/src/main/java/com/motohub/next/RideHistoryLabel.java \
  app/src/main/java/com/motohub/next/RideHistoryPeriod.java \
  tests/RideHistoryPeriodTest.java tests/TrackEmptySessionTest.java
java -cp "$out" com.motohub.next.RideHistoryPeriodTest
java -cp "$out" com.motohub.next.TrackEmptySessionTest
python3 - <<'PY'
from pathlib import Path
java=Path('app/src/main/java/com/motohub/next')
ui=(java/'RideUi.java').read_text()
main=(java/'MainActivity.java').read_text()
service=(java/'RideService.java').read_text()
assert 'bikeRides.removeIf(file->!RideHistoryPeriod.include(file,now,period))' in ui
assert 'selectRideHistoryPeriod(options[which])' in ui
assert 'results.postDelayed(pendingSearch[0],180)' in ui
assert 'results.removeCallbacks(pendingSearch[0])' in ui
assert 'rideHistoryPeriod=RideHistoryPeriod.restore(' in main
assert 'out.putString("rideHistoryPeriod",rideHistoryPeriod)' in main
assert 'TrackFile.discardEmptySession(track)' in service
assert 'Sharing.start(' not in ui
assert 'if(!getSharedPreferences("developer_experiments",0).getBoolean("enabled",false))' in service
print('PASS: period UI, search debounce, zero-byte GPS cleanup and safety wiring')
PY
