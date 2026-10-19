from pathlib import Path
import xml.etree.ElementTree as ET
from version_consistency import current_version,debug_alignment_ok,signed_alignment_ok
root=Path(__file__).resolve().parents[1]
def load(x):return (root/x).read_text(encoding='utf-8')
service=load('app/src/main/java/com/motohub/next/RideService.java')
screen=load('app/src/main/java/com/motohub/next/DeveloperObdScreen.java')
receiver=load('app/src/main/java/com/motohub/next/BootRideReminder.java')
manifest=load('app/src/main/AndroidManifest.xml')
checks={
 'metadata':current_version() is not None and debug_alignment_ok() and signed_alignment_ok(),
 'boot receiver only after reboot': 'Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())' in receiver and 'BootRidePolicy.hasRecoverable' in receiver,
 'boot does not auto start GPS': 'startForegroundService' not in receiver and 'startService(' not in receiver and 'Sharing.' not in receiver,
 'boot uses notification permission': 'POST_NOTIFICATIONS' in receiver and 'areNotificationsEnabled' in receiver,
 'boot notification private': 'VISIBILITY_PRIVATE' in receiver and 'PendingIntent.getActivity' in receiver,
 'manual consent ODO probe': 'confirmOdo(BluetoothDevice device)' in screen and 'setPositiveButton("조회 동의"' in screen,
 'ODO support query': 'query("01A0")' in screen and 'Support.YES' in screen,
 'ODO manual verification twice': '1차 확인' in screen and '최종 확인' in screen and '계기판과 일치' in screen,
 'ODO update locked while riding': 'if(RideService.running||RideService.autoArmed)throw new IllegalStateException' in screen,
 'ODO isolated from normal auto telemetry': '01A6' not in load('app/src/main/java/com/motohub/next/ObdAutoSpeedMonitor.java'),
 'ignition is RPM estimate only': 'ObdEngineState.label' in service and '시동 상태 확인 불가' in load('app/src/main/java/com/motohub/next/ObdEngineState.java'),
 'privacy of ongoing notification': 'setVisibility(Notification.VISIBILITY_PRIVATE)' in service,
 'no group sharing upon auto arm': 'Sharing.stop(); // Automatic local rides' in service,
 'android manifest permission': 'android.permission.RECEIVE_BOOT_COMPLETED' in manifest,
}
ET.parse(root/'app/src/main/AndroidManifest.xml')
for name,ok in checks.items():print(('PASS' if ok else 'FAIL')+': '+name)
assert all(checks.values()),'CM integration failed'
