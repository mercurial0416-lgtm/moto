#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
OUT="$(mktemp -d)"
trap 'rm -rf "$OUT"' EXIT
javac -encoding UTF-8 -d "$OUT" \
 app/src/main/java/com/motohub/next/{TrackFile,RideMotionGate,RideInsights,BackupBundle,BackupIntegrity,BackupExportGate,RestoreRideGate,UpdateSafety}.java \
 tests/{BackupIntegrityTest,BackupExportInterlockTest,RideGpsCoverageTest}.java
for CLASS in BackupIntegrityTest BackupExportInterlockTest RideGpsCoverageTest; do
 java -cp "$OUT" "com.motohub.next.$CLASS"
done
python3 - <<'PY'
from pathlib import Path
root=Path('app/src/main/java/com/motohub/next')
ui=(root/'MainActivity.java').read_text()
assert 'RestoreRideGate.beginExport(RideService.running,RideService.autoArmed)' in ui
assert 'finally{if(reserved)RestoreRideGate.endExport();}' in ui
assert 'BackupIntegrity.verify(check)' in ui
assert 'UpdateSafety.canInstall' in (root/'AppUpdater.java').read_text()
assert 'GPS 관측 구간' in ui
assert "versionCode 514" in Path('app/build.gradle').read_text()
assert 'Sharing.start(' not in (root/'RideService.java').read_text()
assert 'ObdReadOnlyProtocol.query("010D")' in (root/'ObdAutoSpeedMonitor.java').read_text()
print('PASS DE UI/privacy/version integration (8 checks)')
PY
