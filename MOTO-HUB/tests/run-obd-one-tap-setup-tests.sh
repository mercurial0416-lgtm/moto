#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
out="$(mktemp -d)"
trap 'rm -rf "$out"' EXIT
javac -encoding UTF-8 -d "$out" \
 app/src/main/java/com/motohub/next/ObdHandsFreePolicy.java \
 app/src/main/java/com/motohub/next/ObdPowerModePolicy.java \
 app/src/main/java/com/motohub/next/ObdTroubleshootingPolicy.java \
 tests/ObdPowerModePolicyTest.java \
 tests/ObdTroubleshootingPolicyTest.java
java -cp "$out" com.motohub.next.ObdPowerModePolicyTest
java -cp "$out" com.motohub.next.ObdTroubleshootingPolicyTest
python3 - <<'PY'
from pathlib import Path
root=Path("app/src/main/java/com/motohub/next")
read=lambda name:(root/name).read_text(encoding="utf-8")
service=read("RideService.java")
settings=read("DeveloperObdScreen.java")
activity=read("MainActivity.java")
garage=read("HubScreens.java")
ride=read("RideUi.java")
log=read("ObdLocalDiagnosticLog.java")
checks={
 "GPS disabled pauses only once":'if(!autoArmed||autoGpsPaused)return;' in service and 'pauseObdForGpsOff()' in service,
 "GPS restoration resumes radio":'if(autoArmed&&gpsEnabled&&autoGpsPaused)resumeObdAfterGpsOn()' in service,
 "paused reader callbacks cannot race":'ObdAutoSpeedMonitor monitor=obdMonitor;obdMonitor=null;' in service,
 "resume reconnects with known adapter":'handler.post(reconnectObd);' in service,
 "GPS track kept on provider loss":'gpsParkedEvidence.reset();' in service and 'GPS 꺼짐 · 기존 기록 보존' in service,
 "idle power saving only":'ObdPowerModePolicy.delayMs(++obdReconnectRounds,powerSaving,running)' in service,
 "one click from garage":'developerObd.showSimple()' in garage,
 "one click from ride tab":'developerObd.showSimple()' in ride,
 "optional advanced controls":'고급 설정 · 센서/ODO/정차 시간' in settings,
 "request Bluetooth, location, notifications sequentially":all(x in settings for x in
  ('AUTO_CONNECT_PERMISSION_REQUEST','QUICK_LOCATION_PERMISSION_REQUEST','QUICK_NOTIFICATION_PERMISSION_REQUEST')),
 "permission callback continues setup":'developerObd.quickStart()' in activity,
 "explicit user consent preserved":'.setPositiveButton("동의하고 자동 감지 시작"' in settings,
 "unique OBD candidate auto selection":'if(likely.size()==1){confirmAuto(likely.get(0));return;}' in settings,
 "observed milestone local only":'last_GPS_STARTED' in log and 'getAddress()' not in log and 'getLatitude()' not in log,
 "missing ride actionable diagnosis":'ObdLocalDiagnosticLog.readableTroubleshooting(activity)' in settings,
 "ODO not silently edited":'target.put("odo"' not in service,
}
for title,ok in checks.items():
 assert ok,title
print('PASS easy setup, paused-recovery and privacy/source checks:',len(checks))
PY
