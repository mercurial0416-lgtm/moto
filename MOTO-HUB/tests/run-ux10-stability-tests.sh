#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
out="$(mktemp -d)"
trap 'rm -rf "$out"' EXIT
javac -encoding UTF-8 -d "$out" \
 app/src/main/java/com/motohub/next/LocalBackupFilePolicy.java \
 tests/Ux10BackupFilePolicyTest.java
java -cp "$out" com.motohub.next.Ux10BackupFilePolicyTest
python3 - <<'PY'
from pathlib import Path
src=Path('app/src/main/java/com/motohub/next')
service=(src/'RideService.java').read_text()
backup=(src/'LocalSafetyBackup.java').read_text()
auto=service.split('private void completeAutoTrack(')[1].split('private void onObdDisconnected(')[0]
disconnect=service.split('private void onObdDisconnected(')[1].split('Notification notification(')[0]
assert 'if(!autoArmed){' in auto and 'stopWith(verified?' in auto, "Stop orphan OBD foreground service after finalizing"
assert 'if(autoFinishPending){' in disconnect and 'GPS 저장 검증 중' in disconnect, "Disconnect must not close a track while saving"
assert disconnect.index('if(autoFinishPending){')<disconnect.index('if(running){'), "Check pending close first"
assert 'if(finishPending){' in auto and auto.index('if(finishPending){')<auto.index('if(!autoArmed){'), "User STOP takes priority"
assert 'LocalBackupFilePolicy.requireReadable(file);' in backup
assert backup.count('LocalBackupFilePolicy.safeFolder(folder)')>=3
assert backup.count('verified(new File(dir,name))')>=2, 'local restore picker must verify full backup schema'
assert 'destination.exists()||java.nio.file.Files.isSymbolicLink' in backup
print('PASS UX10 OBD pending-close and local recovery file wiring')
PY
