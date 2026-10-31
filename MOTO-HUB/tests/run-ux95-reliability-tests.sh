#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
python3 - <<'PY'
from pathlib import Path
p=Path('app/src/main/java/com/motohub/next')
b=(p/'BackupGpsProof.java').read_text()
g=(p/'GpsStorageSpacePolicy.java').read_text()
ride=(p/'RideService.java').read_text()
photo=(p/'MotorcyclePhoto.java').read_text()
assert 'putString("full_track_names",json.toString())' in b
assert 'JSONObject exported=new JSONObject(raw)' in b and 'full_track_details' in b
assert '전체 ZIP 이후 새 GPS 기록 있음' in b
assert 'WARNING_FREE_BYTES=32L*1024L*1024L' in g
assert 'CRITICAL_FREE_BYTES=8L*1024L*1024L' in g
assert 'checkRuntimeStorage()' in ride
assert 'GpsStorageSpacePolicy.critical(free)' in ride
assert 'stopWith("GPS 저장공간 8MB 미만' in ride
assert 'PHOTO_DISK.execute' in photo
assert '!PhotoRequestGuard.isCurrent(target,requestToken)' in photo
assert 'skipModelCache' in photo
print('PASS UX95: new GPS backup freshness, low-space warning and stop, async cached-photo decoding')
PY
