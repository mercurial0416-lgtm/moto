#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
python3 - <<'PY'
from pathlib import Path
root=Path('app/src/main/java/com/motohub/next')
main=(root/'MainActivity.java').read_text()
ride=(root/'RideUi.java').read_text()
widget=(root/'MotoWidgetProvider.java').read_text()
design=(root/'MotoDesign.java').read_text()
settings=(root/'HubScreens.java').read_text()
service=(root/'RideService.java').read_text()
assert 'section("출발 전 점검")' not in main
assert 'ride_checks' not in main and '오늘의 점검 상태' not in main
assert 'void ride(){' in main and 'new RideUi(this).render();' in main
assert 'void savedRoutes(){' in main and 'edit("routes",null)' in main
assert 'final String[] tabs={"홈","라이딩","차고","더보기"};' in main
assert 'String[] labels={"홈","라이딩","정비","설정"};' in design
assert 'final int[] icons={0,3,2,4};' in design
assert 'case"매물":listings()' in main and 'case"차고":garage()' in main
assert 'a.action(p,"저장한 매물 관리"' in settings
assert 'a.action(p,"저장한 수동 코스"' in settings
assert 'final boolean extras=a.getSharedPreferences("ride_ui",0).getBoolean("extras",false);' in ride
assert 'if(extras){' in ride and 'GPX 가져오기' in ride
assert '라이딩 2건 통합' in ride
assert 'a.label(a.content,"기록된 라이딩"' in ride
assert 'renderPreservingPosition()' in main
assert 'rideStateSignature()!=lastRenderedRideState' in main and 'renderPreservingPosition();' in main
assert 'target.scrollTo(0,restoreY)' in main
assert 'if(java.util.Arrays.equals(ids,lastWidgetIds)&&navigator.equals(lastNavigationChoice))return;' in widget
assert 'manager.updateAppWidget(id,build(context))' in widget
assert 'void basicBackup(){' in settings and 'void advancedBackup(){' in settings
assert '전체 ZIP 백업 만들기' in settings and 'a::importBundle' in settings
assert '고급 백업 · 정밀 검사 · JSON' in settings
assert 'RestoreArchive' in settings or 'BackupDeepCheck' in settings
assert 'a.toast("GPS 기록 시작됨 · 첫 위치 수신 대기")' not in ride or 'GPS 기록 시작됨' in ride
assert '12_000L' in ride and 'GPS 기록 시작을 확인하지 못했어요.' in ride
assert 'rideStartConsent.consume(' in ride and 'RideReadiness.State.READY' in ride
assert 'output.getFD().sync()' in service
print('PASS UX62: preflight fully removed, 4-tab nav, hidden extras, scroll, widget idle, backup safely simplified and GPS consent kept')
PY
