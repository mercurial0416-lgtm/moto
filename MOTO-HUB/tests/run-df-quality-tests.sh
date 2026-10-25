#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
BIN=$(mktemp -d)
trap 'rm -rf "$BIN"' EXIT
javac -encoding UTF-8 -d "$BIN" \
 app/src/main/java/com/motohub/next/{ReceiptSlots,BackupFormat,TrackFile,BackupBundle,BackupIntegrity,BackupReadiness,RestoreArchive,RestoreCapacity}.java \
 tests/{BackupTenThousandTest,BackupReadinessTest}.java
java -Xmx768m -cp "$BIN" com.motohub.next.BackupTenThousandTest
java -cp "$BIN" com.motohub.next.BackupReadinessTest
python3 - <<'PY'
from pathlib import Path
root=Path('app/src/main/java/com/motohub/next')
backup=(root/'BackupBundle.java').read_text()
restore=(root/'RestoreArchive.java').read_text()
format=(root/'BackupFormat.java').read_text()
assert 'BackupFormat.MAX_TRACKS' in backup and 'BackupFormat.MAX_TRACKS' in restore
assert 'BackupFormat.MAX_ENTRIES' in restore and 'BackupFormat.MAX_ENTRIES' in (root/'BackupIntegrity.java').read_text()
assert '전체 ZIP 백업 사전 점검' in (root/'HubScreens.java').read_text()
assert 'Arrays.sort(tracks' in backup and 'Arrays.sort(files' in backup
assert 'versionCode 518' in Path('app/build.gradle').read_text()
assert 'Sharing.start(' not in (root/'RideService.java').read_text()
assert 'ObdReadOnlyProtocol.query("010D")' in (root/'ObdAutoSpeedMonitor.java').read_text()
print('PASS DF archive, version and privacy wiring (7 checks)')
PY
