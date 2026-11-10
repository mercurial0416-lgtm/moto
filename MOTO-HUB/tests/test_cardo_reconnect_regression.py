from pathlib import Path
root=Path(__file__).resolve().parents[1]/"app/src/main/java/com/motohub/next"
ride=(root/"RideService.java").read_text()
starter=(root/"CardoRideStarter.java").read_text()
settings=(root/"BikeObdSettingsUi.java").read_text()
picker=settings.split("void pickCardo(String bikeId)",1)[1].split("private void confirmOdo(",1)[0]
selector=picker.split('setTitle("카르도 연결 감지 기기 선택")',1)[1].split(".setItems(names",1)[0]
checks={
 "picker shows actual paired devices":".setItems(names" in picker and ".setMessage(" not in selector,
 "no background startService on Cardo ACL event":"c.startService(new Intent(c,RideService.class)" not in starter,
 "active Cardo reconnect is delivered to FGS":"RideService.cardoPresenceChanged" in starter,
 "headset reconnect resets disconnect countdown":"s.handler.removeCallbacks(s.cardoDisconnectGrace)" in ride,
 "transient disconnect allows 10 second grace":"s.handler.postDelayed(s.cardoDisconnectGrace,10_000L)" in ride,
 "moving ride GPS protected":"if(!running)" in ride and
  "주행 중일 수 있어 GPS 기록 유지" in ride,
 "ride cleanup drops pending disconnect callback":"handler.removeCallbacks(cardoDisconnectGrace)" in ride,
 "Cardo association scoped to correct garage bike":"bikeId.equals(activeRideBikeId)" in ride,
}
for k,v in checks.items():print(("PASS" if v else "FAIL")+": "+k)
assert all(checks.values()),"Cardo reconnect/picker regression"
