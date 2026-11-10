from pathlib import Path
root=Path(__file__).resolve().parents[1]/"app/src/main/java/com/motohub/next"
ride=(root/"RideService.java").read_text()
pref=(root/"BikeObdPreferences.java").read_text()
ui=(root/"BikeObdSettingsUi.java").read_text()
policy=(root/"CardoGpsFallbackGate.java").read_text()
checks={
 "separate explicit per-bike GPS consent":'getBoolean(key(bikeId,"gpsFallback"),false)' in pref,
 "cardo binding required for GPS fallback":'CardoAutoRidePrefs.forBike(context,bikeId)' in pref,
 "GPS fallback is off by default":'getBoolean(key(bikeId,"gpsFallback"),false)' in pref,
 "automatic night-long GPS excluded":'handler.postDelayed(cardoIdleStop,300_000L)' in ride,
 "gps never samples without approved Cardo":'if(cardoSession&&BikeObdPreferences.gpsFallbackEnabled(this,pendingBikeId))' in ride,
 "mock provider excluded":'location.isFromMockProvider()' in ride,
 "gps position age limited":'age>10_000_000_000L' in ride,
 "gps timestamps validated":'GpsFixPolicy.acceptTime(location.getTime()' in ride,
 "gps accuracy <=20m":'location.getAccuracy()>20' in ride,
 "minimum sustained 70m progress":'segments>=3&&metres>=70&&spanMs>=10000' in policy,
 "live ECU speed blocks GPS fallback":'ObdTelemetryFreshness.speedFresh(obdLastSampleElapsed' in ride,
 "GPS cannot start amid restore":'RestoreRideGate.beginRideStart()' in ride,
 "no automatic ECU odometer overwrite":'OdometerAutoSyncInbox' not in policy,
 "GPS fallback is not silently permanent":'cardoGpsStopWatch' in ride,
 "close after fresh GPS stationary":'stationaryGps=GpsSignalStatus.fresh' in ride,
 "close after actually stopped ECU":'stationaryEcu=obdSpeedKmh>=0' in ride,
 "parked ECU loss retains finalization":'handler.postDelayed(finishParkedObdLoss' in ride,
 "GPS energy cost disclosed":'GPS 센서는 추가 전력을 사용' in ui,
 "user can turn it off":'GPS 자동기록 안전망 끄기' in ui,
 "invalid Cardo binding cannot keep GPS":'CardoAutoRidePrefs.forBike(context,bikeId)' in pref,
}
for key,ok in checks.items():print(("PASS" if ok else "FAIL")+": "+key)
assert all(checks.values()),"Cardo GPS fallback safety regression"
