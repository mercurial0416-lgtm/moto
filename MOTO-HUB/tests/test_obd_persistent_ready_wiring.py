from pathlib import Path
root=Path(__file__).resolve().parents[1]/'app/src/main/java/com/motohub/next'
read=lambda name:(root/name).read_text(encoding='utf-8')
service=read('RideService.java')
monitor=read('ObdAutoSpeedMonitor.java')
settings=read('BikeObdSettingsUi.java')
screen=read('DeveloperObdScreen.java')
activity=read('MainActivity.java')
receiver=read('BootRideReminder.java')
policy=read('ObdConnectionPolicy.java')
checks={
 'explicit per-bike opt-in remains required': 'BikeObdPreferences.setAutoRideEnabled(a,bikeId,true);' in settings,
 'ready uses the opted-in bike only': '},bikeApproved);' in service,
 'one shot bounded reconnect still exists': 'if(!persistentStandby&&' in monitor and 'ObdConnectionPolicy.shouldRetry' in monitor,
 'foreground ready reconnect backs off': 'ObdConnectionPolicy.standbyDelayMs' in monitor and 'STANDBY_COOLDOWN_MS=60_000L' in policy,
 'parked ride saves without disarming persistent watcher': 'GPS 저장 확인 · 다음 시동 OBD 연결 대기' in service and 'if(!BikeObdPreferences.autoRideEnabled(RideService.this,pendingBikeId))' in service,
 'next ride gate reset after completed file': 'autoGate.reset();parkingIsolation.reset();' in service,
 'user disarm revokes future auto resume': 'BikeObdPreferences.setAutoRideEnabled(this,pendingBikeId,false);' in service,
 'foreground reopen re-arms opted in bike': 'root.postDelayed(this::resumeApprovedObdReady' in activity and 'BikeObdPreferences.autoRideEnabled(this,id)' in activity,
 'no reboot-time automatic GPS launch': 'startForegroundService' not in receiver and 'startService(' not in receiver,
 'no ECU write or diagnostic clear': 'ATSH' not in monitor and 'CLEAR' not in monitor,
 'continuous battery cost disclosed': '배터리' in settings and '60초 간격' in settings,
}
for name,good in checks.items():print(('PASS' if good else 'FAIL')+': '+name)
assert all(checks.values()),'OBD auto-ready source/consent regression'
