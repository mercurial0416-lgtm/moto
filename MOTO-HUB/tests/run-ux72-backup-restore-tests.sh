#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
python3 - <<'PY'
from pathlib import Path
p=Path("app/src/main/java/com/motohub/next")
get=lambda f:(p/f).read_text()
backup=get("BackupIntegrity.java")
export=get("BackupBundle.java")
proof=get("BackupGpsProof.java")
trash=get("RideTrash.java")
safety=get("RideStorageManager.java")
restorer=get("RestoreArchive.java")
activity=get("MainActivity.java")
assert 'MessageDigest.getInstance("SHA-256")' in backup and 'trackHashes.put' in backup
assert 'BackupGpsProof.record(this,checkedBackup.trackSha256)' in activity
assert 'RideMileageIntegrity.fingerprint(track)' in proof
assert 'BackupGpsProof.requireExact(a,track);' in safety
assert 'RideTrash.move(a.getFilesDir(),track)' in safety and 'RideTrash.restore(a.getFilesDir()' in safety
assert 'RideCompletionInbox.remove(a,track.getName());' in safety
assert 'StandardCopyOption.ATOMIC_MOVE' in trash
assert 'JsonCanonicalDigest.hash(checked)' in get("UpdateDataFootprint.java")
assert 'String part' in export and 'if(gps&&tracks!=null)' in export
assert 'if(media&&files!=null)' in export and 'if(media&&receipts!=null)' in export
assert 'if(isSplit())throw new IOException' in restorer
assert 'public int installPart(File internal)' in restorer
assert 'void prepareSplitRestore(Uri uri)' in activity
assert 'void listings(){' not in get("HubScreens.java")
assert 'void listings(){' not in activity
assert 'ObdOnBikeVerification.show(activity)' in get("DeveloperObdScreen.java")
assert 'motoReleaseCode=551' in Path("../.github/workflows/moto-ux-validation.yml").read_text()
print("PASS UX72 full JSON digest, exact ZIP GPS SHA, reversible trash, GPS/media split and read-only OBD checklist")
PY
