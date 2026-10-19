from pathlib import Path
from version_consistency import current_version,debug_alignment_ok,signed_alignment_ok
root=Path(__file__).resolve().parents[1]
source=lambda name:(root/'app/src/main/java/com/motohub/next'/name).read_text(encoding='utf-8')
monitor=source('ObdAutoSpeedMonitor.java')
recovery=source('TrackRecovery.java')
screen=source('DeveloperObdScreen.java')
service=source('RideService.java')
checks={
 'version consistent': current_version() is not None and debug_alignment_ok() and signed_alignment_ok(),
 'invalid OBD2 speed is transient': 'throw new IOException("차량 속도 응답 6회 연속 무효' in monitor,
 'explicit unsupported PID is terminal': 'if(ObdSpeedResponsePolicy.unsupported' in monitor,
 'bounded reconnection preserved': 'ObdConnectionPolicy.shouldRetry' in monitor,
 'no ODO in automatic polling': '01A6' not in monitor,
 'no ECU writes': 'ObdReadOnlyProtocol.query("010D")' in monitor and 'ATSH' not in monitor and 'CLEAR' not in monitor,
 'durable recovery marker': 'markerOut.getFD().sync()' in recovery and 'if(marker.isFile())marker.delete();' in recovery and 'Files.move(originalCopy,track.toPath()' in recovery,
 'opt-in privacy': 'Sharing.stop(); // Automatic local rides' in service,
 'never boot auto record': 'startForegroundService' not in source('BootRideReminder.java'),
 'KakaoNavi separate': 'KakaoNaviLaunch.open(activity)' in screen,
 'developer test checklist': '듀크 실차 확인 체크리스트' in screen,
 'APK version': current_version() is not None and 'versionCode '+str(current_version()[0]) in (root/'app/build.gradle').read_text(),
}
for name,ok in checks.items():print(('PASS' if ok else 'FAIL')+': '+name)
assert all(checks.values()),'CN integration assertions failed'
