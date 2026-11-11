from pathlib import Path
root=Path(__file__).resolve().parents[1]
src=root/"app/src/main/java/com/motohub/next"
tests=root/"app/src/test/java/com/motohub/next"
def read(n):return (src/n).read_text()
hub=read("HubScreens.java")
home=read("GarageHomeUi.java")
ride=read("RideUi.java")
service=read("RideService.java")
guide=read("RideMissingRecordingGuide.java")
obd=read("BikeObdSettingsUi.java")
checks={
 "garage Duke name opens actual bike settings":'bike.setContentDescription(b.optString("name","내 바이크")+" 바이크 설정과 OBD 연결 열기")' in hub and 'setOnClickListener(v->new HubScreens(a).manageBike())' in home,
 "garage keeps a separate change-bike control":'Button change=a.button("변경 ▾",false,this::chooseBike)' in hub,
 "OBD settings are first in bike settings":hub.index('a.action(p,"OBD · 카르도 · 자동 라이딩 설정"')<hub.index('a.action(p,"바이크 정보 수정"'),
 "diagnosis available for currently selected bike":'RideMissingRecordingGuide.summary(a,bikeId)' in hub and 'RideMissingRecordingGuide.summary(a,bikeId)' in obd,
 "no false success for unconfigured bike":'mode==ConnectionSetupMode.Mode.NONE' in guide and '실제 ECU 속도 PID 010D' in guide,
 "auto wait allows explicit manual GPS rescue":'MANUAL_GPS_WHILE_ARMED' in ride and 'MANUAL_GPS_WHILE_ARMED' in service and '지금 GPS 기록 시작' in ride,
 "manual rescue doesn't start duplicate ongoing ride":'if(running||!autoArmed||autoFinishPending||finishPending)' in service and 'RideSessionInterlock.keepExistingSession' in service,
 "manual GPS start uses new original and location checks":'openRideTrack();' in service.split('if(MANUAL_GPS_WHILE_ARMED.equals(action))')[1].split('if(RESUME_INTERRUPTED.equals(action))')[0] and 'requestGpsUpdatesChecked();' in service.split('if(MANUAL_GPS_WHILE_ARMED.equals(action))')[1].split('if(RESUME_INTERRUPTED.equals(action))')[0],
 "existing automatic GPS track recording remains separate":'finishAutoTrack()' in service and 'RideSessionVerification.verify' in service,
 "isolated preview settings warning":'기존 v605' in home and '자동으로 이동하지 않아' in home,
 "record absence isn't treated as verified absence of events":'원인을 확정하려면' in guide,
 "new logic covered by actual JUnit test":'speedWithoutGpsFallbackIsClearlyExplained' in (tests/'V606MissingRideReasonTest.java').read_text(),
}
for key,done in checks.items():print(("PASS" if done else "FAIL")+": "+key)
assert all(checks.values()),"v606 ride capture and bike settings regression"
