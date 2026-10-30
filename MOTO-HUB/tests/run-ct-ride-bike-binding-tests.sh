#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
javac -encoding UTF-8 -d "$work" \
 app/src/main/java/com/motohub/next/{TrackFile,TrackCompletion,RideTrackMerge,RideBikeBinding,ReceiptSlots,BackupFormat,BackupBundle,RestoreArchive,RestoreCapacity}.java \
 tests/RideBikeBindingTest.java
java -cp "$work" com.motohub.next.RideBikeBindingTest
python3 - <<'PY'
from pathlib import Path
root=Path('app/src/main/java/com/motohub/next')
r=(root/'RideService.java').read_text();a=(root/'MainActivity.java').read_text()
u=(root/'RideUi.java').read_text();d=(root/'DeveloperObdScreen.java').read_text()
checks={
 'GPS ride start carries consent-fixed bike': 'putExtra(RideService.EXTRA_BIKE_ID,fixedBikeId)' in u and 'a.approvedRideBikeId.equals(liveBike)' in u,
 'OBD auto ride carries chosen bike': 'putExtra(RideService.EXTRA_BIKE_ID,activity.bike()' in d,
 'ride start stores local binding': 'RideBikeBinding.write(track,pendingBikeId)' in r,
 'history shows bike label': '연결된 바이크:' in a,
 'applied ODO verifies bike association': 'latestRideBike=RideBikeBinding.readStrict(track)' in a,
 'ride quarantine retains association for restore': 'RideStorageManager.confirm(this,track,()->close())' in a and '".bike"' in (root/'RideTrash.java').read_text() and 'RideTrash.move(a.getFilesDir(),track)' in (root/'RideStorageManager.java').read_text(),
}
for title,passed in checks.items():
 print(('PASS' if passed else 'FAIL')+': '+title)
assert all(checks.values())
PY
