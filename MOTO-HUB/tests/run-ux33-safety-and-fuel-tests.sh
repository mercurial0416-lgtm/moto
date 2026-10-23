#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
javac -encoding UTF-8 -d "$tmp" \
 app/src/main/java/com/motohub/next/{ReceiptSlots,BackupFormat,TrackFile,RestoreArchive,RestoreCapacity,ServiceCycle,FuelAutoAmount}.java \
 tests/{RestoreArchiveTest,FuelAutoAmountTest}.java
java -cp "$tmp" com.motohub.next.RestoreArchiveTest
java -cp "$tmp" com.motohub.next.FuelAutoAmountTest
python3 - <<'PY'
from pathlib import Path
root=Path('app/src/main/java/com/motohub/next')
quick=(root/'QuickEntryUi.java').read_text()
widget=(root/'MotoWidgetProvider.java').read_text()
restore=(root/'RestoreArchive.java').read_text()
assert 'fuelAuto.noteAmountEdited(value.toString())' in quick
assert 'fuelAuto.update(' in quick and 'amount.setText(nextAmount)' in quick
assert 'new android.util.AtomicFile(file).readFully()' in widget
assert 'java.io.File rollback=new java.io.File(file.getPath()+".bak");' in widget
assert 'Files.isDirectory(path.toPath(),LinkOption.NOFOLLOW_LINKS)' in restore
assert '!Files.isSymbolicLink(path.toPath())' in restore
print('PASS UX33 quick fuel override, atomic widget reads, symlink-safe restore cleanup')
PY
