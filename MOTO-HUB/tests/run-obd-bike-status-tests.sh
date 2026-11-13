#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
python3 - <<'PY'
from pathlib import Path
root=Path("app/src/main/java/com/motohub/next")
read=lambda name:(root/name).read_text(encoding="utf-8")
garage=read("HubScreens.java")
settings=read("DeveloperObdScreen.java")
ride=read("RideUi.java")
service=read("RideService.java")
prefs=read("ObdHandsFreeSession.java")
checks={
 "garage status matches configured bike":'currentBikeId.equals(ObdHandsFreeSession.bikeId(a))' in garage,
 "OBD settings distinguish another bike":'다른 바이크의 자동 감지 작동 중' in settings,
 "ride screen distinguishes enabled vs running":'boolean consentedForBike=ObdHandsFreeSession.enabled(a)' in ride and '현재 감지는 중지됨' in ride,
 "confirmed unsupported speed cannot silently retry":'ObdSpeedSupportFailure.confirmedUnsupported(reason)' in service and 'ObdHandsFreeSession.disable(this);' in service,
 "bad track cannot silently open next auto track":'if(!verified){' in service and '자동 저장 검증 실패' in service,
 "active GPS speed untouched by Bluetooth retries":'obdStationaryRemainingMs=-1;liveSpeedKmh=0' not in service,
 "resume preference keyed by vehicle":'ObdHandsFreeSession.bikeId' in settings and 'bike.equals(vehicle.optString("id"))' in prefs,
}
for label,valid in checks.items():
 assert valid,label
print('PASS OBD bike status and recovery source guards:',len(checks))
PY
