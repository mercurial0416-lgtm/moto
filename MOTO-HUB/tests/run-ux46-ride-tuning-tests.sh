#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
javac -encoding UTF-8 -d "$tmp" \
 app/src/main/java/com/motohub/next/ObdRideAutoGate.java \
 app/src/main/java/com/motohub/next/RideMotionGate.java \
 app/src/main/java/com/motohub/next/TrackFile.java \
 app/src/main/java/com/motohub/next/RideInsights.java \
 app/src/main/java/com/motohub/next/RidePaceSegments.java \
 tests/ObdFuelRestGraceTest.java \
 tests/RidePaceSegmentsTest.java
java -cp "$tmp" com.motohub.next.ObdFuelRestGraceTest
java -cp "$tmp" com.motohub.next.RidePaceSegmentsTest
python3 - <<'PY'
from pathlib import Path
root=Path("app/src/main/java/com/motohub/next")
get=lambda n:(root/n).read_text(encoding="utf-8")
service=get("RideService.java");ride=get("RideUi.java")
home=get("GarageHomeUi.java");gate=get("ObdRideAutoGate.java")
monitor=get("ObdAutoSpeedMonitor.java");analyze=get("RidePaceSegments.java")
main=get("MainActivity.java")
checks={
 "explicit only rest permission":'AUTO_BREAK.equals(action)' in service and 'autoArmed&&running&&!finishPending' in service,
 "rest requires fresh stopped OBD":'ObdTelemetryFreshness.speedFresh(obdLastSampleElapsed,now)' in service and 'obdSpeedKmh<=2' in service,
 "ride only rest override":'extendStopForBreak(600_000L)' in service and 'configuredStopAfterMs' in gate,
 "safe rest button":'주유·휴식 중 기록 유지 (10분)' in ride,
 "rest button only stopped":'rest.setVisibility(stopped?View.VISIBLE:View.GONE)' in ride,
 "manual GPS remains possible":'GPS 기록 시작' in ride,
 "one touch complete GPS review":'최근 완료 라이딩 · 주행거리·정비 확인' in ride,
 "review performed off UI thread":'moto-last-ride-review' in ride and 'a.openRideInsights(latest)' in ride,
 "no automatic odo mutation":'target.put("odo"' not in service,
 "maintenance still after explicit apply":'write(next);DailyMaintenanceJob.checkSoon(this)' in main,
 "per-ride 15m analysis on demand":'15분 구간별 속도·이동 분석' in main and 'new Thread(()->' in main,
 "GPS gaps not counted as stop":'MAX_CONTINUOUS_FIX_MS' in analyze and 'GPS 미수신 시간은 정차로 계산하지 않아요' in analyze,
 "aux poll adaptive and read-only":'auxiliaryEvery=speed<=0?1:4' in monitor and 'ObdReadOnlyProtocol.query(pid)' in monitor,
 "old-ride scan off main":'HOME_RIDE_SCAN.execute(' in home and 'a.runOnUiThread(' in home,
 "stale home results discarded":'generation!=HOME_SCAN_GEN.get()' in home,
 "responsive live GPS boxes":'if(stackLive)live.setOrientation(LinearLayout.VERTICAL)' in ride,
 "optional landscape OBD screen":'SCREEN_ORIENTATION_SENSOR_LANDSCAPE' in get("ObdDashboardActivity.java"),
 "screen-on only while user opted/OBD armed":'visible&&RideService.autoArmed&&keepScreenPreference()' in get("ObdDashboardActivity.java"),
 "screen-on cleared on pause":'visible=false;handler.removeCallbacks(update);syncScreenAwake()' in get("ObdDashboardActivity.java"),
 "No GPS resume on app relaunch":'AUTO_ARM.equals(action)' in service and 'AUTO_BREAK.equals(action)' in service,
}
for desc,success in checks.items():
 assert success,desc
print(f"PASS UX46 source checks: {len(checks)}")
PY
