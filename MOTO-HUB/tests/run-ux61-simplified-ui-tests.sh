#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
python3 - <<'PY'
from pathlib import Path
p=Path("app/src/main/java/com/motohub/next")
settings=(p/"HubScreens.java").read_text()
ride=(p/"RideUi.java").read_text()
main=(p/"MainActivity.java").read_text()
assert 'title("설정","자주 사용하는 설정만 표시합니다.")' in settings
assert 'line("앱 업데이트"' in settings
assert settings.index('line("앱 업데이트"')<settings.index('void advancedSettings(){')
assert 'line("기변 계산기"' not in settings
assert 'line("보호장구"' not in settings
assert 'line("개인정보 없는 진단 보고서"' not in settings
assert 'line("친구·그룹 서버 연결"' not in settings
assert 'a.action(page,"라이딩 통합 자가진단"' in settings
assert 'if(Sharing.enabled)line("위치 공유 중지"' in settings
assert 'a.action(p,"보관된 보호장구 기록"' in settings
assert 'a.action(p,"기변 계산기"' in settings
assert 'a.action(p,"개인정보 없는 진단 보고서"' in settings
assert 'a.action(p,"친구·그룹 서버 진단"' in settings
assert 'a.action(p,"듀크 390 · OBD2 읽기 전용 (실험)"' in settings
assert 'ridePreflight' not in settings
assert 'OBD2 동글 연결 · ECU 센서 테스트' not in ride
assert 'OBD2 데이터를 읽는 중' not in ride
assert 'NavigationApps.choose(a)' in ride
assert 'NavigationApps.openPreferred(a)' in ride
assert 'rideStartConsent.approve()' in ride
assert 'RideService.running' in ride and 'RideService.STOP' in ride
assert 'new AppUpdater(a).show()' in settings
assert 'if(Sharing.enabled)' in main
print("PASS UX61: 5 advanced features hidden, consolidated self-check, ride+nav/OBD safety and data preserved")
PY
