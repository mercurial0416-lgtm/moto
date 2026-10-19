from pathlib import Path
from version_consistency import version_declaration_ok, debug_alignment_ok, signed_alignment_ok
root=Path(__file__).resolve().parents[1]
src=root/'app/src/main/java/com/motohub/next'
read=lambda name:(src/(name+'.java')).read_text(encoding='utf-8')
manifest=(root/'app/src/main/AndroidManifest.xml').read_text()
workflow=(root/'.github/workflows/android-debug.yml').read_text()
ui=read('HubScreens')
impl=read('DeveloperObdScreen')
proto=read('ObdReadOnlyProtocol')
checks={
 'developer-only entry hidden until 7 taps': 'if(++taps[0]>=7)' in ui and 'developer_experiments' in ui and 'if(a.getSharedPreferences("developer_experiments",0).getBoolean("enabled",false))' in ui,
 'OBD screen gated even when called directly': 'if(!activity.getSharedPreferences("developer_experiments",0).getBoolean("enabled",false))return;' in impl,
 'OBD connection requires confirmation': '동의하고 연결' in impl and 'setPositiveButton' in impl,
 'only bonded devices': 'getBondedDevices()' in impl and 'startDiscovery' not in impl,
 'no auto-connect at startup': 'DeveloperObdScreen developerObd;' in read('MainActivity') and 'developerObd.loadPaired()' in read('MainActivity') and 'developerObd.show()' not in read('MainActivity'),
 'permission requested only when connecting': 'Manifest.permission.BLUETOOTH_CONNECT' in impl and 'CONNECT_PERMISSION_REQUEST' in read('MainActivity'),
 'SPP bluetooth transport': 'createRfcommSocketToServiceRecord(SPP)' in impl,
 'explicit safe ECU PID whitelist': '010C","010D","0105","0142' in proto and 'throw new IllegalArgumentException' in proto,
 'no DTC erase or active vehicle control': '0400' not in proto and '04\r' not in impl and 'ATSH' not in impl,
 'on dismiss and destroy socket closes': 'owner.setOnDismissListener' in impl and 'developerObd.stop()' in read('MainActivity'),
 'BT hardware optional': 'android.hardware.bluetooth' in manifest and 'android:required="false"' in manifest,
 'BT permissions declared': 'android.permission.BLUETOOTH_CONNECT' in manifest and 'android.permission.BLUETOOTH"' in manifest,
 'version/code consistent': version_declaration_ok() and debug_alignment_ok(),
 'workflow includes OBD tests': './tests/run-obd-readonly-tests.sh' in workflow,
}
for k,ok in checks.items():print(('PASS ' if ok else 'FAIL ')+k)
assert all(checks.values()),'CC wiring regression'
print('PASS: %d developer-only integration checks (static)'%len(checks))
