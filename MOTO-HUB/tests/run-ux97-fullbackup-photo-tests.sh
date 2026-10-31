#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
python3 - <<'PY'
from pathlib import Path
p=Path("app/src/main/java/com/motohub/next")
screen=(p/"HubScreens.java").read_text()
proof=(p/"BackupGpsProof.java").read_text()
photo=(p/"MotorcyclePhoto.java").read_text()
assert "full_sidecar_sha256" in proof and "savedSidecars.optJSONObject" in proof
assert "for(String suffix:RideTrash.SIDECARS)" in proof
assert "부속 파일 내용이 변경" in proof
assert "BackupGpsProof.fullCurrentStatus(a,settingsSnapshot)" in screen
assert "BackupGpsProof.fullCurrentStatus(a,backupSnapshot)" in screen
assert "BackupGpsProof.fullGpsDeepStatus(a,snapshot)" in screen
assert "try{return a.db.toString(2);}" in screen
assert "catch(org.json.JSONException malformed){return null;}" in screen
assert 'final String settingsSnapshot=backupJsonSnapshot();' in screen
assert 'final String backupSnapshot=backupJsonSnapshot();' in screen
assert 'final String snapshot=backupJsonSnapshot();' in screen
assert 'LOCAL_BACKUP_IO.execute(()->{' in screen
assert 'line("백업 · 복원",BackupGpsProof.fullCurrentStatus(' not in screen
assert 'if(garageMode&&!skipInvalidLocal)' in photo
assert 'PHOTO_DISK.execute(()->{' in photo
assert 'Bitmap user=decodeLocalSampled(manual,1200)' not in photo
assert 'Bitmap loaded=decodeLocalSampled(manual,1200)' in photo
assert 'PhotoRequestGuard.isCurrent(target,requestToken)' in photo
print("PASS UX97 full archive GPS+sidecar proof, pretty JSON, async UI status and manual photo load")
PY
