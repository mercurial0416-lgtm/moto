#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
dir="$(mktemp -d)"
trap 'rm -rf "$dir"' EXIT
javac -encoding UTF-8 -d "$dir" \
 app/src/main/java/com/motohub/next/{ReceiptSlots,BackupFormat,TrackFile,RestoreArchive,RestoreCapacity}.java \
 tests/RestoreCrashJournalTest.java
java -cp "$dir" com.motohub.next.RestoreCrashJournalTest
python3 - <<'PY'
from pathlib import Path
s=Path("app/src/main/java/com/motohub/next/MainActivity.java").read_text()
r=Path("app/src/main/java/com/motohub/next/RestoreArchive.java").read_text()
assert 'RestoreArchive.CrashJournal.recoverAll(getFilesDir(),bytes->atomic(disk,bytes))' in s
assert s.index('CrashJournal.recoverAll(') < s.index('try{db=disk.')
assert 'catch(Exception e){readOnly=true;crashedRestoreNeedsHelp=true;}' in s
assert 'ready.install(getFilesDir(),previous,json->{' in s
assert 'CrashJournal.begin(internal,backup,old,fresh,previousJson);' in r
assert r.index('CrashJournal.begin(internal,backup,old,fresh,previousJson)') < r.index('for(String name:fresh){',r.index('public void install('))
assert r.index('CrashJournal.markCommitted(backup)') > r.index('writer.write(json);')
print("PASS crash restore startup/integrity wiring")
PY
