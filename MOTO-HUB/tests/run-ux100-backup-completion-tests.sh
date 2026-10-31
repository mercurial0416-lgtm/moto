#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
python3 - <<'PY'
from pathlib import Path
src=Path("app/src/main/java/com/motohub/next")
activity=(src/"MainActivity.java").read_text()
bundle=(src/"BackupBundle.java").read_text()
readiness=(src/"BackupReadiness.java").read_text()
safety=(src/"LocalSafetyBackup.java").read_text()
screens=(src/"HubScreens.java").read_text()
assert "static boolean checkpoint(Context context,JSONObject data)" in safety
assert "confirmed=true;" in safety and "return confirmed;" in safety
assert "checkpointOk=LocalSafetyBackup.checkpoint(this,db)" in activity
assert "boolean exportVerified=false;" in activity
assert "DocumentsContract.deleteDocument(getContentResolver(),uri)" in activity
assert "불완전 ZIP 파일을 자동 삭제하지 못했으니" in activity
assert "actualCount=checkedBackup.entryNames.size()" in activity
assert "File[] tracks=gps?listIfPresent(rides," in bundle
assert "File[] selectedModels=media?listIfPresent(modelFolder," in bundle
assert "File[] receipts=media?listIfPresent(internal," in bundle
assert 'zip.closeEntry();total+=estimatedGpx;count++;' in bundle
assert "trashRides,selectedModels" in readiness
assert 'File trashFolder=new File(internal,"rides-trash")' in readiness
assert 'File modelFolder=new File(internal,"moto-year-photos")' in readiness
assert "recoveryAvailabilityMessage(Context context)" in safety
assert screens.count("LocalSafetyBackup.recoveryAvailabilityMessage(a)")>=2
assert (Path("app/src/test/java/com/motohub/next")/
        "UX100BackupCompletionTest.java").is_file()
print("UX100 ZIP safety, reporting, partition and restore source checks PASS")
PY
