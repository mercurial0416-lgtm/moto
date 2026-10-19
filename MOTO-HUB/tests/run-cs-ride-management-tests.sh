#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
javac -encoding UTF-8 -d "$work" \
 app/src/main/java/com/motohub/next/{TrackFile,TrackCompletion,RideTrackMerge,RideBikeBinding,RideCategory,RideMotionGate,RideInsights,BackupFormat,BackupBundle,RestoreArchive,RestoreCapacity}.java \
 tests/RideCategoryAndUndoTest.java
java -cp "$work" com.motohub.next.RideCategoryAndUndoTest
python3 - <<'PY'
from pathlib import Path
source=Path('app/src/main/java/com/motohub/next')
ride=(source/'RideUi.java').read_text()
activity=(source/'MainActivity.java').read_text()
checks={
 'ride merge action implemented':'void chooseMerge()' in ride,
 'merge undo action in insights':'RideTrackMerge.undo(getFilesDir(),track)' in activity,
 'category selector attached to insights':'RideCategory.write(track,categories[which])' in activity,
 'stats exclude archived merge':'RideTrackMerge.archived(track))continue' in ride,
 'existing GPS records still opened':'a.openRideInsights(f)' in ride,
 'privacy of rides in history':'기기에 GPS 경로를 기록' in ride,
}
for name,ok in checks.items():print(('PASS' if ok else 'FAIL')+': '+name)
assert all(checks.values())
PY
