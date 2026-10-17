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
assert 'stopWith("기록 종료",true)' in service
assert 'cleanup(false);instance=null' in service
assert 'stopWith("GPS가 꺼져 기록을 중지했습니다.",false)' in service
assert 'stopWith("저장 오류로 기록 중지: "+e.getMessage(),false)' in service
assert 'startService(new Intent(a,RideService.class).setAction(RideService.STOP))' in ui
assert 'TrackRecovery.recover(f)' in ui
assert '복구 없이 분석' in ui
assert '".recovered"' in backup
assert '.recovered' in restore
assert '.recovered' in main
print('PASS: explicit stop, interrupted-state UI, backup/restore, deletion wiring')
PY
