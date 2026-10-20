#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
out="$(mktemp -d)"
trap 'rm -rf "$out"' EXIT
javac -encoding UTF-8 -d "$out" app/src/main/java/com/motohub/next/TrackFile.java app/src/main/java/com/motohub/next/TrackRecovery.java tests/TrackRecoveryTest.java
java -cp "$out" com.motohub.next.TrackRecoveryTest
python3 - <<'PY'
from pathlib import Path
service=Path('app/src/main/java/com/motohub/next/RideService.java').read_text()
ui=Path('app/src/main/java/com/motohub/next/RideUi.java').read_text()
backup=Path('app/src/main/java/com/motohub/next/BackupBundle.java').read_text()
restore=Path('app/src/main/java/com/motohub/next/RestoreArchive.java').read_text()
main=Path('app/src/main/java/com/motohub/next/MainActivity.java').read_text()
assert 'stopWith("사용자가 라이딩 기록을 종료했습니다.",running)' in service and 'if(AUTO_DISARM.equals(action))' in service
assert 'cleanup(false);' in service and 'gpsIoThread.quitSafely()' in service and 'instance=null;super.onDestroy();' in service
assert 'status="GPS 일시 중단 · 켜면 기록 재개 (그룹 공유 중지)"' in service and 'Sharing.stop();status=' in service
assert 'stopWith("저장 오류로 기록 중지: "+e.getMessage(),false)' in service
assert 'startService(new Intent(a,RideService.class).setAction(RideService.STOP))' in ui
assert 'TrackRecovery.recover(f)' in ui
assert '복구 없이 분석' in ui
assert '".recovered"' in backup
assert '.recovered' in restore
assert '.recovered' in main
print('PASS: explicit stop, interrupted-state UI, backup/restore, deletion wiring')
PY
