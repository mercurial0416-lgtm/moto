#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
: "${JSON_JAR:?Set JSON_JAR to the org.json Java dependency (CI provides it)}"
out="$(mktemp -d)"
trap 'rm -rf "$out"' EXIT
javac -encoding UTF-8 -cp "$JSON_JAR" -d "$out" \
 app/src/main/java/com/motohub/next/{Store,LocalBackupFilePolicy}.java \
 tests/Ux12BackupContentTest.java
java -cp "$JSON_JAR:$out" com.motohub.next.Ux12BackupContentTest
python3 - <<'PY'
from pathlib import Path
local=Path('app/src/main/java/com/motohub/next/LocalSafetyBackup.java').read_text()
policy=Path('app/src/main/java/com/motohub/next/LocalBackupFilePolicy.java').read_text()
assert 'static boolean verified(File file)' in local and 'try{read(file);return true;}' in local
assert local.count('&& verified(new File(dir,name))') == 2, 'picker lists must validate real content'
assert local.count('LocalSafetyBackup::verified') == 2, 'both 7-day and 12-point retention must validate content'
assert 'Store.parse(new String(bytes,StandardCharsets.UTF_8));' in local, 'validate daily backup before persisting'
assert 'static File[] eligible(File folder,String namePattern,java.util.function.Predicate<File> validContent)' in policy
assert 'catch(RuntimeException ignored)' in policy
screen=Path('app/src/main/java/com/motohub/next/HubScreens.java').read_text()
assert 'Executors.newSingleThreadExecutor' in screen, 'bounded serial backup worker'
assert screen.count('LOCAL_BACKUP_IO.execute(()->{')>=3, 'snapshot, picker and selected file read off UI'
assert 'final String current=a.db.toString();' in screen, 'capture stable JSON before background work'
assert 'a.confirmImport(data)' in screen and 'a.runOnUiThread(' in screen, 'restore confirmation remains on UI'
assert 'LocalSafetyBackup.read(selected)' in screen, 'read must re-validate at selection time'
print('PASS UX12 restore picker, schema, retention and responsive settings screen wiring')
PY
