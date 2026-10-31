#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
python3 - <<'PY'
from pathlib import Path
p=Path("app/src/main/java/com/motohub/next")
backup=(p/"BackupGpsProof.java").read_text()
screens=(p/"HubScreens.java").read_text()
service=(p/"RideService.java").read_text()
model=(p/"MotorcyclePhoto.java").read_text()
garage=(p/"GarageHomeUi.java").read_text()
assert "full_track_details" in backup and "file.lastModified()" in backup
assert 'GPS 원본이 삭제됨' in backup and "fullGpsDeepStatus" in backup
assert 'MessageDigest.getInstance("SHA-256")' in backup
assert "GPS 백업 내용 정밀 검사 (SHA-256)" in screens
assert "LOCAL_BACKUP_IO.execute" in screens and "BackupGpsProof.fullGpsDeepStatus(a,snapshot)" in screens
assert "storageWatchdog=new Runnable()" in service
assert "gpsWriteHandler.postDelayed(storageWatchdog,60_000L)" in service
assert "gpsWriteHandler.removeCallbacks(storageWatchdog)" in service
assert "private synchronized boolean checkRuntimeStorage()" in service
assert "target.getParent()==null" not in model
assert "!iv.isAttachedToWindow()" not in garage
assert "PhotoRequestGuard.isCurrent(target,requestToken)" in model
assert "!key.equals(iv.getTag())" in garage
print("PASS UX96: verified ZIP GPS modifications/deletion; standalone storage watchdog; preattach photo integrity")
PY
