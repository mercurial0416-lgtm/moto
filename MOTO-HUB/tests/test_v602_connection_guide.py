from pathlib import Path

root = Path(__file__).resolve().parents[1] / "app/src/main/java/com/motohub/next"
ui = (root/"BikeObdSettingsUi.java").read_text()
obd = (root/"DeveloperObdScreen.java").read_text()
guide = ui.split(" void open(String bikeId){",1)[1].split(" void openAdvanced(String bikeId){",1)[0]
advanced = ui.split(" void openAdvanced(String bikeId){",1)[1]
quick = obd.split(" void showQuick(){",1)[1].split(" void showQuickAdvanced(){",1)[0]
mode = (root/"ConnectionSetupMode.java").read_text()
checks = {
  "guided default, advanced secondary": "void open(String bikeId)" in ui and "void openAdvanced(String bikeId)" in ui and '고급 설정 · ODO / 센서 / 로그 / 해제' in guide,
  "registered bike is checked": 'for(org.json.JSONObject item:a.items("vehicles"))' in guide and "bike==null" in guide,
  "real ECU data isn't conflated with Bluetooth pairing": 'ObdTelemetryFreshness.speedFresh' in guide and "등록 완료 · 실제 ECU 연결은 미확인" in guide,
  "GPS recording status requires real fresh fix": "RideService.gpsFresh()" in guide,
  "four onboarding stages": all(k in guide for k in ['step(p,"01"', 'step(p,"02"', 'step(p,"03"', 'step(p,"04"']),
  "mode-first and only necessary hardware": "ConnectionSetupMode.get(a,bikeId)" in guide and "wantsObd&&!obd" in guide and "wantsCardo&&!cardo" in guide and "enum Mode" in mode,
  "Cardo GPS requires explicit separate consent": "changeGpsFallback(bikeId,false)" in guide and "동의하고 켜기" in guide,
  "Android background limitation disclosed": "Android가 막으면 알림" in guide,
  "guided help and stage-aware permissions": "showConnectionHelp" in guide and "else if(!permissions)" in guide and "wantsObd&&!obd" in guide,
  "advanced controls remain available": all(s in advanced for s in ["OBD 읽기 전용 실시간 계기판","ECU 누적거리 자동 동기화","센서 선택"]),
  "simplified ECU screen": '연결 확인은 딱 2단계' in quick and '실제 ECU 데이터 확인' in quick and '고급 PID/ODO 검사' in quick,
  "quick test does not auto-arm": "loadPairedAuto()" not in quick and "AUTO_ARM" not in quick,
  "quick test never deletes ECU fault codes": "고장코드를 지우지 않아" in quick,
  "quick test avoids a redundant entry dialog": 'new AlertDialog.Builder(a).setTitle("OBD 연결 검사")' not in obd.split("static void launchQuick(MainActivity a){",1)[1].split("void showQuick()",1)[0],
  "verified device registration is gated by sensor values": 'showVerifiedRegistration(device,selectedBike)' in obd and 'if(!loggedSensor)' in obd and 'verifiedRegisterButton.setVisibility(android.view.View.GONE)' in obd,
  "dongle replacement warns about consent revocation": "boolean hadCardo=changing" in guide and "해제 내용 확인 · 등록" in guide,
  "live exact headset and ECU status": "CardoConnectionProbe.probe" in guide and "obdSpeedKmh" in guide,
  "first automatic trip verification is separate": "FirstRideSetupProof.summary(a,bikeId)" in guide,
}
for label, good in checks.items():
 print(("PASS" if good else "FAIL")+": "+label)
assert all(checks.values()), "v603 onboarding regression"
