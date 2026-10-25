#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
out="$(mktemp -d)"
trap 'rm -rf "$out"' EXIT
javac -encoding UTF-8 -d "$out" \
  app/src/main/java/com/motohub/next/TrackFile.java \
  app/src/main/java/com/motohub/next/RideBikeBinding.java \
  app/src/main/java/com/motohub/next/RideHistoryBikeFilter.java \
  tests/RideHistoryBikeFilterTest.java
java -cp "$out" com.motohub.next.RideHistoryBikeFilterTest
python3 - <<'PY'
from pathlib import Path
java=Path('app/src/main/java/com/motohub/next')
ui=(java/'RideUi.java').read_text()
main=(java/'MainActivity.java').read_text()
assert 'RideHistoryBikeFilter.select(rides,scope)' in ui
assert 'RideHistoryFilter.counts(bikeRides,activeName,recording)' in ui
assert 'RideHistoryFilter.select(bikeRides,selected,activeName,recording)' in ui
assert 'a.selectRideHistoryBike(ids.get(which))' in ui
assert 'RideHistoryBikeFilter.damaged(rides)' in ui
assert 'a::resetRideHistoryControls' in ui
assert 'rideHistoryBike=RideHistoryBikeFilter.restore(' in main
assert 'out.putString("rideHistoryBike",rideHistoryBike)' in main
assert 'getSharedPreferences("ride_history_ui",MODE_PRIVATE).edit().putString("bike",rideHistoryBike).apply()' in main
assert 'Sharing.start(' not in ui
print('PASS: ride-bike filter UI, state persistence, reset and sharing guards')
PY
