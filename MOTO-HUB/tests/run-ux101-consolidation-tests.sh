#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
bin="$(mktemp -d)"
trap 'rm -rf "$bin"' EXIT
javac -encoding UTF-8 -d "$bin" \
 app/src/main/java/com/motohub/next/{ReceiptSlots,BackupFormat,TrackFile,RideTrash,BatchArchiveExport,BackupIntegrity}.java \
 tests/Ux101ConsolidationTest.java
java -cp "$bin" com.motohub.next.Ux101ConsolidationTest
python3 - <<'PY'
from pathlib import Path
root=Path("app/src/main/java/com/motohub/next")
bundle=(root/"BatchArchiveExport.java").read_text()
trash=(root/"RideTrash.java").read_text()
activity=(root/"MainActivity.java").read_text()
manual=(root/"VerifiedDocumentExport.java").read_text()
assert "File[] found=dir.listFiles(selector);" in bundle
assert "if(found==null)throw new IOException" in bundle
assert "if(folders==null)throw new IOException" in trash
assert "static void verifySources(Part part,InputStream saved)" in bundle
assert "MessageDigest.isEqual(archiveHash.digest(),localHash.digest())" in bundle
assert "BatchArchiveExport.verifySources(part,proof)" in activity
assert "restoreCheckpoint=LocalSafetyBackup.checkpoint(this,validated)" in activity
assert "VerifiedDocumentExport.save(getContentResolver(),uri,BackupFormat.MAX_GPX_BYTES" in activity
assert "VerifiedDocumentExport.save(getContentResolver(),uri,BackupFormat.MAX_JSON_BYTES" in activity
assert "new Thread(()->{" in activity
assert "MessageDigest.isEqual(expected.digest(),actual.digest())" in manual
assert "DocumentsContract.deleteDocument(resolver,uri)" in manual
print("PASS UX101 source/UI/background/SAF guards")
PY
