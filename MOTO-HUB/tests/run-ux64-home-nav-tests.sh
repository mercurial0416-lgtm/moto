#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
python3 - <<'PY'
from pathlib import Path
s=Path('app/src/main/java/com/motohub/next')
home=(s/'GarageHomeUi.java').read_text()
nav=(s/'NavigationApps.java').read_text()
assert 'if(recording||armed){' in home
assert 'if(!recording&&!armed){' in home
assert '원터치 출발 · GPS + 기본 내비' in home
assert 'new RideUi(a).requestStart(true)' in home
assert 'if(RideService.running)' in home
assert 'setNeutralButton("다른 내비 실행"' in nav
assert 'NavigationChoice.values()' in nav
assert 'installedLaunchIntent(activity,option)==null' in nav
assert 'MotoWidgetProvider.refresh(activity)' in nav
assert 'putString(KEY,target.id).commit()' in nav
assert 'getLastKnownLocation' not in nav and 'EXTRA_LATITUDE' not in nav
print('PASS UX64 one idle ride start button, running stop preserved and installed navigation failover')
PY
