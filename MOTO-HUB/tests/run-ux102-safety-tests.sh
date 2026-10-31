#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
scratch="$(mktemp -d)"
trap 'rm -rf "$scratch"' EXIT
javac -encoding UTF-8 -d "$scratch" \
 app/src/main/java/com/motohub/next/TrackFile.java \
 app/src/main/java/com/motohub/next/BackupPickerTransfer.java \
 tests/Ux102SafetyRegressionTest.java
java -cp "$scratch" com.motohub.next.Ux102SafetyRegressionTest
python3 - <<'PY'
from pathlib import Path
root=Path("app/src/main/java/com/motohub/next")
activity=(root/"MainActivity.java").read_text()
tracks=(root/"TrackFile.java").read_text()
picker=(root/"BackupPickerTransfer.java").read_text()
assert "if(found==null)throw new IOException" in tracks
assert "GPS 기록 항목이 안전하지 않습니다" in tracks
assert "BackupPickerTransfer.begin(48,\"\",password)" in activity
assert "BackupPickerTransfer.take(48)" in activity
assert "BackupPickerTransfer.begin(request,snapshot,password)" in activity
assert "BackupPickerTransfer.take(request)" in activity
assert "BackupPickerTransfer.take(41)" in activity
assert "BackupPickerTransfer.cancel(request)" in activity
assert "pendingSecurePassword" not in activity
assert "pendingBundleJson" not in activity
assert "pendingExport" not in activity
assert "jsonImportRunning=true" in activity
assert "RestoreRideGate.isRestoring()" in activity
assert '"moto-json-import-check").start()' in activity
assert '"moto-json-import-apply").start()' in activity
assert "refreshJsonImportAfterRecreation()" in activity
assert "AtomicFile rollback=new AtomicFile" in activity
assert "atomic(disk,validated.toString(2)" in activity
assert "Arrays.fill(password,'\\0')" in picker
assert "class Entry" in picker and "MAX_AGE_NANOS" in picker
assert "}else if(request==42)attempt(" not in activity
print("PASS UX102 native lifecycle, async import and data-safety wiring")
PY
