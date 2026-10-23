#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
javac -encoding UTF-8 -d "$tmp" \
 app/src/main/java/com/motohub/next/{ReceiptSlots,BackupFormat,TrackFile,RestoreArchive,RestoreCapacity}.java \
 tests/RestoreCrashJournalTest.java
java -cp "$tmp" com.motohub.next.RestoreCrashJournalTest
python3 - <<'PY'
from pathlib import Path
s=Path('app/src/main/java/com/motohub/next/MainActivity.java').read_text()
method=s.split('void confirmImport(JSONObject next){',1)[1].split('@Override public void onBackPressed()',1)[0]
assert 'Store.parse(next.toString())' in method
assert method.index('atomic(rollback,previous)') < method.index('atomic(disk,validated.toString(2)')
assert method.index('atomic(disk,validated.toString(2)') < method.index('LocalSafetyBackup.checkpoint(this,validated)')
assert 'try{write(next);}' not in method
assert 'db=validated;readOnly=false;' in method
r=Path('app/src/main/java/com/motohub/next/RestoreArchive.java').read_text()
chunk=r.split('static void recoverOne(',1)[1].split('private static void validate(',1)[0]
assert chunk.index('for(String name:fresh)safePath(') < chunk.index('for(String name:fresh)if(!original.contains(name))')
print('PASS UX32 JSON restore committed before side effects; journal target preflight')
PY
