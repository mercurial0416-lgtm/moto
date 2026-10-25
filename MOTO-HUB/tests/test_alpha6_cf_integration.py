from pathlib import Path
from version_consistency import version_declaration_ok, debug_alignment_ok, signed_alignment_ok
root=Path(__file__).resolve().parents[1]
p=lambda name:(root/'app/src/main/java/com/motohub/next'/f'{name}.java').read_text()
service=p('RideService');screen=p('DeveloperObdScreen');gate=p('ObdRideAutoGate')
manifest=(root/'app/src/main/AndroidManifest.xml').read_text()
workflow=(root/'.github/workflows/android-debug.yml').read_text()
checks={
 'explicit developer gate': 'developer_experiments' in service and 'developer_experiments' in screen,
 'ODOMETRY not guessed': '계기판 누적 주행거리' in screen and 'ODE' not in service and 'ObdReadOnlyProtocol.query(pid)' in p('ObdAutoSpeedMonitor'),
 'opt-in foreground GPS': '동의하고 자동 감지 시작' in screen and 'prepareForeground(true)' in service,
 'GPS-only failover': all(token in service.split('private void onObdDisconnected')[1].split('Notification notification')[0] for token in ('autoArmed=false','autoGate.reset();','obdMonitor=null;')) and 'if(running)' in service.split('private void onObdDisconnected')[1].split('Notification notification')[0] and 'GPS 단독 기록 유지' in service,
 'stop control in notification':'PendingIntent.getService' in service and 'setAction(STOP)' in service,
 'no boot sharing': 'Sharing.stop(); // Automatic local rides' in service and 'Sharing.stop();\n  if(running)' in service,
 'session-scoped reconnect': 'listener.reconnecting' in p('ObdAutoSpeedMonitor') and 'MAX_RETRIES=8' in p('ObdConnectionPolicy'),
 'variable rest period': 'setStopAfterMs' in gate and 'obd_stop_after_ms' in screen and 'obd_stop_after_ms' in service,
 'stale OBD readings masked': 'speedFresh' in p('ObdDashboardActivity') and 'ObdTelemetryFreshness' in service,
 'ECU read-only': 'ObdReadOnlyProtocol.query(pid)' in p('ObdAutoSpeedMonitor') and 'ObdReadOnlyProtocol.query("010D")' in p('ObdAutoSpeedMonitor'),
 'Kakao standalone': 'KakaoNaviLaunch.open(activity)' in screen and 'KakaoNaviLaunch.open(a)' in p('RideUi'),
 'idle poll less frequent':'ObdSpeedResponsePolicy.pollDelayMs(speed)' in p('ObdAutoSpeedMonitor') and 'speed==0?4_000L:1_100L' in p('ObdSpeedResponsePolicy'),
 'trip moving and stopped': '총 경과 ' in p('MainActivity') and 'GPS 추정 이동 시간' in p('MainActivity'),
 'model context secrecy': 'obdLastError' in service and 'obdLastError' in p('ObdDashboardActivity'),
 'nonexported dashboard': 'android:name=".ObdDashboardActivity" android:exported="false"' in manifest,
 'version and CI':version_declaration_ok() and 'run-cf-telemetry-tests.sh' in workflow and debug_alignment_ok(),
}
for name,ok in checks.items():print(('PASS' if ok else 'FAIL')+': '+name)
assert all(checks.values()),'CF source wiring failed'
print('CF source wiring checks:',len(checks),'(not an Android runtime test)')
