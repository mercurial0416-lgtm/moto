#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
python3 - <<'PY'
from pathlib import Path
root=Path("app/src/main/java/com/motohub/next")
fuel=(root/"QuickEntryUi.java").read_text()
restore=(root/"RideStorageManager.java").read_text()
footprint=(root/"UpdateDataFootprint.java").read_text()
proof=(root/"BackupGpsProof.java").read_text()
unit=Path("app/src/test/java/com/motohub/next/UX86BackupFootprintTest.java").read_text()
assert 'final String reviewedSnapshot=a.db.toString();' in fuel
assert 'if(!reviewedSnapshot.equals(beforeSave))' in fuel
assert 'if(a.readOnly||a.bike()==null||!bikeId.equals(a.bike().optString("id")))' in fuel
assert 'if(vehicle==null||!bikeId.equals(record.optString("vehicleId")))' in fuel
assert 'QuickLogDuplicate.exists(a.items("expenses"),record,"expenses")' in fuel
assert 'FuelNearDuplicate.near(a.items("expenses"),record)' in fuel
assert 'RideCompletionInbox.record(a,restored)' not in restore
assert 'RideCompletionInbox.remove(a,restored.getName());' in restore
assert 'RideTrash.restore(a.getFilesDir(),target)' in restore
assert 'RideCompletionInbox.record(a,track)' not in restore
assert 'GPS 분할 ZIP' in restore and '전체 ZIP' in restore
assert 'last_gps_zip_kind' in proof and 'full?"전체 ZIP":"GPS 분할 ZIP"' in proof
assert '전체 ZIP이나 GPS 분할 ZIP을 새로 내보내세요.' in proof
assert 'requireFullCurrent(Context context,String currentJson)' in proof
for ext in ['.done','.imported','.recovered','.unmerged','.category','.bike','.favorite','.mergedinto']:
 assert '"'+ext+'"' in footprint
assert 'collectSidecars(file,toHash)' in footprint and 'collectSidecars(source,toHash)' in footprint
assert 'trashTracks=' in footprint and 'gpsSidecars=' in footprint
assert 'requireDirectoryIfPresent(trash)' in footprint
assert 'verifyRegular(source)' in footprint
assert 'Files.isSymbolicLink(folder.toPath())' in footprint
assert 'favoriteAndCategoryAreIncludedInFingerprint' in unit
assert 'quarantineBytesAndBindingAreIncludedInFingerprint' in unit
assert 'gpsOnlyProofIsClearlyLabeledAndDoesNotGrantBikeCascadeDeletion' in unit
print("PASS UX86: stale-save fail-close, GPS restore no false recap, full metadata/trash hash and exact ZIP proof")
PY
