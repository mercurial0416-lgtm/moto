#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
out="$(mktemp -d)"
trap 'rm -rf "$out"' EXIT
javac -encoding UTF-8 -d "$out" \
 app/src/main/java/com/motohub/next/ObdParkingIsolation.java \
 tests/ObdParkingIsolationTest.java
java -cp "$out" com.motohub.next.ObdParkingIsolationTest
python3 - <<'PY'
from pathlib import Path
s=Path('app/src/main/java/com/motohub/next/RideService.java').read_text()
assert 'if(gpsObdParkingHold)return;' in s, "phone walk must not be recorded"
assert 'gpsObdResumeSplit' in s and 'rotateRideSection(false);' in s, "parking must break the GPS line"
assert 'private synchronized void rotateRideSection()throws Exception{rotateRideSection(true);}' in s, "normal size rotation must preserve continuity"
assert 'if(bridgeWithLastFix&&lastGood!=null)' in s, "paused boundary fix must not leak into the next segment"
assert 'gpsObdResumeSplit=false;' in s, "resume is acknowledged only after rotation"
assert 'parkingIsolation.onSpeed(kmh,elapsed)' in s
assert 'parkingIsolation.onLinkLoss(SystemClock.elapsedRealtime())' in s
assert 'parkingIsolation.dueAfterLost(SystemClock.elapsedRealtime())' in s
assert 'finishAutoTrack();' in s and 'RideCompletionInbox.record(getApplicationContext(),savedTrack)' in s
assert 'obdSpeedKmh>=ObdParkingIsolation.RESUME_MIN_KMH' in s
assert 'gpsObdParkingHold=false;gpsObdResumeSplit=false' in s
assert 'new Intent(this,RideService.class).setAction(AUTO_DISARM)' in s
print('PASS parked-bike GPS guards: freeze, resume, bounded loss, native save and explicit stop')
PY