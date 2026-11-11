from pathlib import Path

root = Path(__file__).resolve().parents[1] / "app/src/main/java/com/motohub/next"
ui = (root/"BikeObdSettingsUi.java").read_text()
obd = (root/"DeveloperObdScreen.java").read_text()
guide = ui.split(" void open(String bikeId){",1)[1].split(" void openAdvanced(String bikeId){",1)[0]
advanced = ui.split(" void openAdvanced(String bikeId){",1)[1]
quick = obd.split(" void showQuick(){",1)[1].split(" void showQuickAdvanced(){",1)[0]
checks = {
  "guided default, advanced secondary": "void open(String bikeId)" in ui and "void openAdvanced(String bikeId)" in ui and '고급 설정 · ODO / 센서 / 로그 / 해제' in guide,
  "guided default still verifies registered bike": 'for(org.json.JSONObject item:a.items("vehicles"))' in guide and "bike==null" in guide,
  "real data isn't conflated with bluetooth registration": 'ObdTelemetryFreshness.speedFresh' in guide and "등록 완료 · 실제 ECU 연결은 미확인" in guide,
  "GPS recording status uses a real fresh fix": "RideService.gpsFresh()" in guide,
  "all four onboarding steps": all(k in guide for k in ['step(p,"01"', 'step(p,"02"', 'step(p,"03"', 'step(p,"04"']),
  "OBD and Cardo optional paths": "if(!obd&&!cardo)" in guide and "else if(!cardo&&obd)" in guide and "else if(cardo&&!obd&&!fallback)" in guide,
  "GPS fallback requires separate explicit action": "changeGpsFallback(bikeId,false)" in guide and '동의하고 켜기' in guide,
  "warnings for Android background launch": "Android가 차단하면 알림을 눌러 시작" in guide,
  "diagnostic guided help and permissions": "showConnectionHelp" in guide and "showPermissionHelp" in guide,
  "advanced controls still exist": all(s in advanced for s in ["OBD 읽기 전용 실시간 계기판","ECU 누적거리 자동 동기화","센서 선택"]),
  "simplified connection screen": '연결 확인은 딱 2단계' in quick and '실제 ECU 데이터 확인' in quick and '고급 PID/ODO 검사' in quick,
  "quick connection doesn't auto-start ride": "loadPairedAuto()" not in quick and "AUTO_ARM" not in quick,
  "quick connection has no invasive ECU command": "고장코드를 지우지 않아" in quick,
  "quick OBD screenshot no multi-page prompts": 'new AlertDialog.Builder(a).setTitle("OBD 연결 검사")' not in obd.split("static void launchQuick(MainActivity a){",1)[1].split("void showQuick()",1)[0],
}
for label, good in checks.items():
 print(("PASS" if good else "FAIL")+": "+label)
assert all(checks.values()), "v602 guided setup regression"
