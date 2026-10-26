#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
javac -encoding UTF-8 -d "$tmp" \
 app/src/main/java/com/motohub/next/{GpsSignalStatus,MaintenanceHistorySearch,ObdRideAutoGate}.java \
 tests/{GpsSignalStatusTest,MaintenanceHistorySearchTest,ObdIdleStopPreferenceTest}.java
for name in GpsSignalStatusTest MaintenanceHistorySearchTest ObdIdleStopPreferenceTest; do
 java -cp "$tmp" "com.motohub.next.$name"
done
python3 - <<'PY'
from pathlib import Path
src=Path("app/src/main/java/com/motohub/next")
code=lambda n:(src/n).read_text(encoding="utf-8")
nav=code("NavigationApps.java");ride=code("RideUi.java");home=code("GarageHomeUi.java")
service=code("RideService.java");dash=code("ObdDashboardActivity.java")
garage=code("HubScreens.java");fuel=code("QuickEntryUi.java");up=code("AppUpdater.java")
choose=nav.split("static void choose(MainActivity activity)",1)[1].split("static Intent installedLaunchIntent",1)[0]
checks={
 "changing default never launches chosen external app":'open(activity,' not in choose,
 "changing default saves locally before refresh":'putString(KEY,selectedApp.id).commit()' in choose,
 "running or armed ride gets updated notification":'if(running||autoArmed)service.notifySafely(' in service,
 "home direct launch with separate settings":'void quickNavigation()' in home and 'NavigationApps.openPreferred(a)' in home and 'NavigationApps.choose(a)' in home,
 "ride accessible launch without auto sharing":'NavigationApps.openPreferred(a)' in ride and 'GPS 기록은 계속됩니다' in ride,
 "missing navigator offers alternative":'setNeutralButton("다른 내비 선택"' in nav,
 "no GPS/GPX/ECU data transferred":all(s not in nav for s in ("getLastKnownLocation","Sharing.offer","EXTRA_LATITUDE","GPS_COORDS","putExtra(")),
 "recent fuel price only explicit selection":'지난 리터당 가격' in fuel and 'unitPrice.setText(String.valueOf(lastUnitPrice))' in fuel,
 "only completed service searchable":'if(!service.optBoolean("scheduleOnly"))completed.add(service)' in garage,
 "large service history capped at 30 visible":'if(displayed++>=30)continue;' in garage,
 "traffic-friendly default remains user adjustable":'USER_DEFAULT_STOP_AFTER_MS' in code("DeveloperObdScreen.java") and 'values={180_000L,300_000L,600_000L}' in code("DeveloperObdScreen.java"),
 "stale GPS speed cannot masquerade as zero":'GpsSignalStatus.liveSpeedLabel(' in ride and 'RideService.gpsFixElapsed()' in ride,
 "dashboard never presents stale OBD speed":'speed.setText(speedFresh' in dash and 'speed.setTextColor(speedFresh?' in dash,
 "dashboard throttles missing signal UI refresh":'speedFresh?1300:3000' in dash,
 "GPS disk writes remain sync before optional sharing":service.index('output.getFD().sync()')<service.index('RidePostCommit.afterWrite('),
 "GPS session not auto rearmed after OS restart":'START_NOT_STICKY' in service and 'cleanup(false)' in service,
 "play edition offers encrypted full backup":'a::exportEncryptedBundle' in up and 'Google Play에서 MOTO HUB 보기' in up,
 "play edition blocks update during active ride":'RideService.running||RideService.autoArmed||RestoreRideGate.isRestoring()' in up,
 "play edition never sideloads":'if(BuildConfig.STORE_DISTRIBUTION){openGooglePlay();return;}' in up,
}
for title,passed in checks.items():
 assert passed,title
print("PASS UX50-52 source regression",len(checks))
PY
