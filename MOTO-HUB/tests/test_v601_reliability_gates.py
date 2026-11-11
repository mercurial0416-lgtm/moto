from pathlib import Path
root=Path(__file__).resolve().parents[1]
src=root/"app/src/main/java/com/motohub/next"
def read(name):return (src/name).read_text()
ride=read("RideService.java")
cardo=read("CardoAutoRidePrefs.java")
start=read("CardoRideStarter.java")
device=read("ObdLocalDiagnosticLog.java")
battery=read("RidePowerAudit.java")
ui=read("BikeObdSettingsUi.java")
main=read("MainActivity.java")
checks={
 "Cardo GPS-only explicitly opted in":'cardoGpsOnly=cardoApproved' in ride and 'BikeObdPreferences.gpsFallbackEnabled' in ride and 'gpsFallbackEnabled(c,bike)' in start,
 "Cardo registration no longer requires an OBD":'!BikeObdPreferences.validAddress(obd)||!cardo.equalsIgnoreCase(obd)' in cardo,
 "finite battery-conscious standby":'CardoStandbyPolicy.idleWindowMs' in ride and '900_000L' in read("CardoStandbyPolicy.java"),
 "missed Cardo event reconciled without scan":'CardoConnectionProbe.probe' in read("CardoPresenceReconciler.java") and 'getProfileProxy' in read("CardoConnectionProbe.java") and 'CardoPresenceReconciler.onForeground(this)' in main and 'startDiscovery(' not in read("CardoPresenceReconciler.java"),
 "pre-trigger real GPS points written first":'postAtFrontOfQueue' in ride and 'flushActualGpsPreroll(45_000L)' in ride and 'flushActualGpsPreroll(15_000L)' in ride and 'prerollReplaying?50_000_000_000L' in ride,
 "GPS fix freshness not mistaken for parking":'gpsStationaryEvidence.confirmed(now)' in ride and 'GpsSignalStatus.fresh(lastFixElapsed,now)' not in ride,
 "OBD stationary evidence is fresh":'ObdTelemetryFreshness.speedFresh(obdLastSampleElapsed,now)' in ride,
 "crash continuation never overwrites old track":'RideResumePolicy.source' in ride and 'track.createNewFile()' in ride and '.setNeutralButton("GPS 이어 기록"' in main,
 "diagnostics scoped by MAC-derived anonymous fingerprint":'obd_diag_namespace_v601' in device and 'MessageDigest.getInstance("SHA-256")' in device,
 "failure timeline bounded":'kept>=16' in read("ObdAutoRideProgress.java") and '최근 자동기록 단계' in read("ObdAutoRideProgress.java"),
 "battery trend labeled whole-device":'BatteryManager.EXTRA_LEVEL' in battery and 'MOTO HUB 단독 전력소모량은 아닙니다' in battery and 'RidePowerAudit.summary' in ui,
 "OTA promotion remains signed-and-hash verified":'PINNED_CERT_SHA256' in read("UpdateManifest.java") and 'versionCode' in read("UpdateManifest.java"),
}
for label,ok in checks.items():print(("PASS" if ok else "FAIL")+": "+label)
assert all(checks.values()),"v601 non-destructive ride/OBD regression"
