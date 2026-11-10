from pathlib import Path
import xml.etree.ElementTree as ET
root=Path(__file__).resolve().parents[1]
java=root/"app/src/main/java/com/motohub/next"
read=lambda name:(java/name).read_text(encoding="utf-8")
manifest=ET.parse(root/"app/src/main/AndroidManifest.xml").getroot()
receiver=read("CardoBluetoothReceiver.java")
companion=read("CardoCompanionService.java")
starter=read("CardoRideStarter.java")
prefs=read("CardoAutoRidePrefs.java")
settings=read("BikeObdSettingsUi.java")
service=read("RideService.java")
activity=read("MainActivity.java")
policy=read("CardoAutoRidePolicy.java")
namespace="{http://schemas.android.com/apk/res/android}"
perms={e.get(namespace+"name") for e in manifest.findall("uses-permission")}
rx=[x for x in manifest.findall(".//receiver") if x.get(namespace+"name")==".CardoBluetoothReceiver"]
sv=[x for x in manifest.findall(".//service") if x.get(namespace+"name")==".CardoCompanionService"]
checks={
 "Bluetooth ACL exempt receiver declared":bool(rx) and
    "android.bluetooth.device.action.ACL_CONNECTED" in ET.tostring(rx[0],encoding="unicode"),
 "companion service declared":bool(sv) and
    sv[0].get(namespace+"permission")=="android.permission.BIND_COMPANION_DEVICE_SERVICE",
 "background location explicit grant": "android.permission.ACCESS_BACKGROUND_LOCATION" in perms,
 "observe companion declared": "android.permission.REQUEST_OBSERVE_COMPANION_DEVICE_PRESENCE" in perms,
 "exact paired Cardo MAC only": "CardoAutoRidePrefs.matches(c,address)" in receiver,
 "no generic Bluetooth scan loop": "startScan(" not in receiver and "startScan(" not in companion,
 "Android FGS restriction safe fallback": "notifyTap(c)" in starter and
    "ACCESS_BACKGROUND_LOCATION" in starter,
 "five-minute no-ride cutoff": "300_000L" in service and "cardoIdleStop" in service,
 "ride GPS survives headset disconnect": "카르도 연결 끊김 · 주행 중일 수 있어 GPS 기록 유지" in service,
 "waiter opt-in no nightly poll": "setAutoRideEnabled(c,bikeId,false)" in prefs,
 "independently bound Cardo+OBD": "mac.equalsIgnoreCase(BikeObdPreferences.address(c,bikeId))" in prefs,
 "user can disable and release observation": "CardoCompanionPairing.stopObserving(a)" in settings,
 "notification tap is explicit user intent": "ACTION_TAP" in starter and "applyCardoStartTap" in activity,
 "consent cannot trigger for arbitrary bike": "CardoAutoRidePrefs.forBike(this,pendingBikeId)" in service,
 "reboot does not silently start GPS": "startForegroundService" not in read("BootRideReminder.java"),
 "no DTC erase or ECU programming": "ATSH" not in starter and "04\r" not in starter
}
for name,ok in checks.items():print(("PASS" if ok else "FAIL")+": "+name)
assert all(checks.values()),"Cardo automatic ride power/privacy regression"
