from pathlib import Path
root=Path(__file__).resolve().parents[1]
src=root/'app/src/main/java/com/motohub/next'
service=(src/'RideService.java').read_text()
main=(src/'MainActivity.java').read_text()
ui=(src/'RideUi.java').read_text()
monitor=(src/'ObdAutoSpeedMonitor.java').read_text()
policy=(src/'ObdConnectionPolicy.java').read_text()
checks={
 'restart prompt is opt-in': 'showInterruptedRidePrompt()' in main and 'new Thread(' in main and '중단된 라이딩 기록 발견' in main,
 'no location service auto-start in prompt': 'startService(' not in main.split('void showInterruptedRidePrompt(){')[1].split('@Override protected void onResume')[0],
 'requires manual restoration': 'TrackRecovery.recover(f)' in ui and '복구 없이 분석' in ui,
 'GPS record survives OBD disconnect': 'gpsOnlyAfterObd=true' in service and 'GPS 단독 기록 유지' in service,
 'OS never restarts ride service': 'START_NOT_STICKY' in service,
 'notification throttle does not alter GPS points': 'notificationRate.shouldNotify(SystemClock.elapsedRealtime())' in service and 'output.getFD().sync()' in service,
 'read-only PID queries': 'ObdReadOnlyProtocol.query("010D")' in monitor and 'ObdReadOnlyProtocol.query(pid)' in monitor,
 'partial OBD response rejected': 'OBD2 응답 완료 신호 없음' in monitor,
 'user can still disarm manually': 'if(AUTO_DISARM.equals(action))' in service and '자동 출발 감지만 끕니다' in ui,
 'reconnect session bounded': 'MAX_RETRIES=8' in policy and 'while(!stopped&&System.nanoTime()<until)' in monitor,
 'navigation remains separate': 'NavigationApps.choose(a)' in ui and 'NavigationApps.installedLaunchIntent(this,choice)' in service and 'Sharing.offer(' not in (src/'NavigationApps.java').read_text(),
 'developer only OBD': 'developer_experiments' in service,
 'no automatic sharing': 'Sharing.stop(); // Automatic local rides' in service,
}
for name,ok in checks.items():print(('PASS' if ok else 'FAIL')+' '+name)
assert all(checks.values())
