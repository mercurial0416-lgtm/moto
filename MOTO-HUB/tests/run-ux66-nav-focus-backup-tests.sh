#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
python3 - <<'PY'
from pathlib import Path
p=Path("app/src/main/java/com/motohub/next")
ui=(p/"RideUi.java").read_text()
policy=(p/"RideScreenRefreshPolicy.java").read_text()
settings=(p/"HubScreens.java").read_text()
nav=(p/"NavigationApps.java").read_text()
service=(p/"RideService.java").read_text()
assert "BACKGROUND_UI_POLL_MS=10000L" in policy
assert "if(!a.hasWindowFocus())" in ui
assert "hero.postDelayed(this,RideScreenRefreshPolicy.BACKGROUND_UI_POLL_MS);" in ui
assert ui.index("if(!a.hasWindowFocus())")<ui.index("RideStopSuggestion.shouldSuggest(")
assert "new AlertDialog.Builder(a).setTitle(\"라이딩을 종료할까요?\")" in ui
assert 'a.renderPreservingPosition();return;' in ui
assert "startForegroundService(new Intent(a,RideService.class)" in ui
assert "output.getFD().sync()" in service
assert 'a.action(p,"전체 ZIP 백업 만들기"' in settings
assert 'a.action(p,"전체 ZIP 백업 복원"' in settings
section=settings[settings.index("void basicBackup()"):settings.index("void backupMore()")]
assert '암호화 전체 백업 만들기' not in section
assert '다른 백업 방식 · 검사' in section
assert 'void backupMore()' in settings
assert 'a::exportEncryptedBundle' in settings and 'a::importEncryptedBundle' in settings
assert 'this::advancedBackup' in settings
assert "if(installed.isEmpty())" in nav and 'setTitle("내비 앱 설치 필요")' in nav
assert 'openStore(activity,preferred)' in nav
assert 'GPS 기록은 중단되지 않습니다.' in nav
assert 'NavigationApps.openPreferred(a)' in ui
assert 'rideStartConsent.approve()' in ui
print("PASS UX66 background UI power, app-resume safety, navigator no-app rescue, compact backup and GPS invariants")
PY