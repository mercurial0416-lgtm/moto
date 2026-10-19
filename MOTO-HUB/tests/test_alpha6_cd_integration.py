from pathlib import Path
from version_consistency import version_declaration_ok, debug_alignment_ok, signed_alignment_ok
root=Path(__file__).resolve().parents[1]
src=root/'app/src/main/java/com/motohub/next'
text=lambda n:(src/f'{n}.java').read_text()
manifest=(root/'app/src/main/AndroidManifest.xml').read_text()
workflow=(root/'.github/workflows/android-debug.yml').read_text()
checks={
 'developer-only entry and 7 taps': 'if(++taps[0]>=7)' in text('HubScreens') and '개발자 · OBD2 실험실' in text('DeveloperObdScreen'),
 'auto recording requires separate explicit consent': '동의하고 자동 감지 시작' in text('DeveloperObdScreen') and 'AUTO_ARM' in text('DeveloperObdScreen'),
 'auto recording disabled by default': 'getSharedPreferences("developer_experiments",0).getBoolean("enabled",false)' in text('RideService') and 'AUTO_ARM.equals(action)' in text('RideService'),
 'manual developer device pairing only': 'getBondedDevices()' in text('DeveloperObdScreen') and 'getBondedDevices()' in text('RideService'),
 'start only with foreground service': 'startForegroundService(start)' in text('DeveloperObdScreen') and 'FOREGROUND_SERVICE_TYPE_LOCATION|ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE' in text('RideService'),
 'continuous notification can stop auto mode': '"OBD 감지만 중지"' in text('RideService') and 'setAction(AUTO_DISARM)' in text('RideService') and 'AUTO_DISARM' in text('RideUi') and 'setAction(STOP)' in text('RideService'),
 'auto stops on developer disable': 'if(RideService.autoArmed)a.startService' in text('HubScreens') and 'AUTO_DISARM' in text('HubScreens'),
 'no auto group sharing or server write': 'Sharing.start(' not in text('RideService') and 'SocialClient.' not in text('ObdAutoSpeedMonitor'),
 'auto meter uses GPS points, existing storage': 'openRideTrack();manager.requestLocationUpdates' in text('RideService') and 'TrackFile.point(' in text('RideService'),
 'safe disconnect / recovery': 'onObdDisconnected' in text('RideService') and all(token in text('RideService').split('private void onObdDisconnected')[1].split('Notification notification')[0] for token in ('autoArmed=false','autoGate.reset();','obdMonitor=null;')) and 'GPS 단독 기록 유지' in text('RideService'),
 'read-only PID whitelist unchanged': 'ObdReadOnlyProtocol.query("010D")' in text('ObdAutoSpeedMonitor') and '010D' in text('ObdReadOnlyProtocol') and '0400' not in text('ObdAutoSpeedMonitor'),
 'connected-device foreground permission and type': 'android.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE' in manifest and 'location|connectedDevice' in manifest,
 'version aligned with CI': version_declaration_ok() and debug_alignment_ok() and debug_alignment_ok(),
 'CI gate test': 'run-obd-auto-ride-tests.sh' in workflow,
}
for label,ok in checks.items():print(('PASS' if ok else 'FAIL'),label)
assert all(checks.values()),'CD source wiring regression'
print(f'PASS {len(checks)} auto ride integration static checks (not Android runtime)')
