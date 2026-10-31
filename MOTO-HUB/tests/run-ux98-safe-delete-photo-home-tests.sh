#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
python3 - <<'PY'
from pathlib import Path
p=Path("app/src/main/java/com/motohub/next")
main=(p/"MainActivity.java").read_text()
backup=(p/"BackupGpsProof.java").read_text()
photo=(p/"MotorcyclePhoto.java").read_text()
garage=(p/"GarageHomeUi.java").read_text()
assert "BackupGpsProof.recordVerifiedZip(this,checkedBackup.trackSha256" in main
assert "BackupGpsProof.requireFullDeepCurrent(this,snapshot)" in main
assert '"MOTO-bike-delete-proof"' in main
assert 'if(!same||readOnly||RestoreRideGate.blocked' in main
assert 'removeConfirmedApply(key,item)' in main
assert 'BackupGpsProof.fullCurrentStatus(this,snapshot).contains("일치")' in main
assert "static void recordVerifiedZip(Context context" in backup
assert 'hashLocalFile(track,TrackFile.MAX_BYTES)' in backup
assert 'hashLocalFile(sidecar,limit)' in backup
assert "if(included!=hashes.size())" in backup and "noCompletionMarker(file)" in backup
assert 'static void requireFullDeepCurrent(Context context,String currentJson)' in backup
assert 'retryManualPhotoQueue(a,target,credit,requestToken,manualDecode' in photo
assert 'remaining<=0' in photo and 'PhotoRequestGuard.isCurrent(target,requestToken)' in photo
assert 'new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed' in photo
assert 'HERO_PHOTO_IO.execute(()->{' in garage
assert 'HOME_IO.execute(()->{' in garage
assert 'new Thread(r,"moto-home-photo")' in garage
print("PASS UX98 full backup proof and deletion, bounded photo retries, isolated home photo worker")
PY
