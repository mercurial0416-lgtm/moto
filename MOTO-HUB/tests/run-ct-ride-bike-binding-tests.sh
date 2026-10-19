#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
javac -encoding UTF-8 -d "$work" \
 app/src/main/java/com/motohub/next/{TrackFile,TrackCompletion,RideTrackMerge,RideBikeBinding,BackupFormat,BackupBundle,RestoreArchive,RestoreCapacity}.java \
 tests/RideBikeBindingTest.java
java -cp "$work" com.motohub.next.RideBikeBindingTest
python3 - <<'PY'
from pathlib import Path
root=Path('app/src/main/java/com/motohub/next')
r=(root/'RideService.java').read_text();a=(root/'MainActivity.java').read_text()
u=(root/'RideUi.java').read_text();d=(root/'DeveloperObdScreen.java').read_text()
checks={
 'GPS ride start carries chosen bike': 'putExtra(RideService.EXTRA_BIKE_ID,a.bike()' in u,
 'OBD auto ride carries chosen bike': 'putExtra(RideService.EXTRA_BIKE_ID,activity.bike()' in d,
 'ride start stores local binding': 'RideBikeBinding.write(track,pendingBikeId)' in r,
 'history shows bike label': '연결된 바이크:' in a,
 'applied ODO verifies bike association': 'latestRideBike=RideBikeBinding.readStrict(track)' in a,
 'ride deletion removes association': 'new File(track.getPath()+".bike").delete()' in a,
}
for title,passed in checks.items():
 print(('PASS' if passed else 'FAIL')+': '+title)
assert all(checks.values())
PY
