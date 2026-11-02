#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
python3 - <<'PY'
from pathlib import Path
s=Path('app/src/main/java/com/motohub/next')
home=(s/'GarageHomeUi.java').read_text()
nav=(s/'NavigationApps.java').read_text()
assert 'boolean running=RideService.running,armed=RideService.autoArmed;' in home
assert 'if(!RideService.running&&!RideService.autoArmed)new RideUi(a).requestStart(true)' in home
assert 'String label=running?' in home and '라이딩 진행 중 · 상태 보기' in home
ride=(s/'RideUi.java').read_text()
assert 'setPositiveButton("저장하고 종료"' in ride and 'void finishWithSummary()' in ride
assert 'RideService.STOP' in ride
assert 'setNeutralButton("다른 내비 실행"' in nav
assert 'NavigationChoice.values()' in nav
assert 'installedLaunchIntent(activity,option)==null' in nav
assert 'MotoWidgetProvider.refresh(activity)' in nav
assert 'putString(KEY,target.id).commit()' in nav
assert 'getLastKnownLocation' not in nav and 'EXTRA_LATITUDE' not in nav
print('PASS UX64 one idle ride start button, running stop preserved and installed navigation failover')
PY
