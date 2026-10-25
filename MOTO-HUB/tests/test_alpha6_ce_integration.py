from pathlib import Path
from version_consistency import version_declaration_ok, debug_alignment_ok, signed_alignment_ok
import re
root=Path(__file__).resolve().parents[1]
p=lambda name:(root/'app/src/main/java/com/motohub/next'/f'{name}.java').read_text()
manifest=(root/'app/src/main/AndroidManifest.xml').read_text()
workflow=(root/'.github/workflows/android-debug.yml').read_text()
checks={
 'dev-only OBD and no auto boot':'getSharedPreferences("developer_experiments",0).getBoolean("enabled",false)' in p('RideService') and 'AUTO_ARM.equals(action)' in p('RideService') and 'getBondedDevices()' in p('RideService'),
 'auto ride explicit consent':'동의하고 자동 감지 시작' in p('DeveloperObdScreen'),
 'bounded foreground reconnect':'MAX_RETRIES=8' in p('ObdConnectionPolicy') and 'listener.reconnecting' in p('ObdAutoSpeedMonitor') and 'connectionLost()' in p('ObdRideAutoGate'),
 'loss keeps existing GPS track':'GPS 단독 기록 유지' in p('RideService') and 'if(running)' in p('RideService').split('private void onObdDisconnected')[1].split('Notification notification')[0],
 'sensor writes prohibited':'04FF' not in p('ObdAutoSpeedMonitor') and 'ObdReadOnlyProtocol.query("010D")' in p('ObdAutoSpeedMonitor') and 'ObdReadOnlyProtocol.query(pid)' in p('ObdAutoSpeedMonitor'),
 'OBD and ODO separated':'계기판 누적 주행거리' in p('DeveloperObdScreen') and 'GPS 오차가 있으므로 실제 계기판을 먼저 확인하세요' in p('MainActivity'),
 'maint due from confirmed ride': 'DailyMaintenanceJob.checkSoon(this)' in p('MainActivity'),
 'telemetry local and limited': 'static volatile double obdRpm' in p('RideService') and 'obdCoolantC' in p('RideService'),
 'digital dashboard developer only':'developer_experiments' in p('ObdDashboardActivity') and 'android:exported="false"' in manifest,
 'GPS summary completed ride':'RideInsights.analyze(savedTrack)' in p('RideService') and '다시 출발 대기' in p('RideService'),
 'no fuel consumption guesses':'연료 소모량 미검증' in p('DeveloperObdScreen'),
 'GPS route speed segmentation':'RideSpeedSegments.category(' in p('RideMapActivity') and 'getOutlinePaint().setColor' in p('RideMapActivity'),
 'Kakao independent app launch':'com.locnall.KimGiSa' in p('KakaoNaviLaunch') and 'KakaoNaviLaunch.open' in p('RideUi'),
 'no implicit group sharing':'Sharing.stop(); // Automatic local rides' in p('RideService') and 'if(!autoArmed&&location.getAccuracy()<=40)Sharing.offer(location)' in p('RideService'),
 'version and signed build aligned':version_declaration_ok() and debug_alignment_ok() and signed_alignment_ok(),
 'tests wired into CI':'run-ce-telemetry-tests.sh' in workflow and 'test_alpha6_ce_integration.py' in workflow,
}
for k,v in checks.items():print(('PASS' if v else 'FAIL'),k)
assert all(checks.values()),'CE wiring mismatch'
print(f'PASS {len(checks)} source wiring checks (not Android build)')
