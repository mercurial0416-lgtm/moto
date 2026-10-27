#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
python3 - <<'PY'
from pathlib import Path
root=Path("app/src/main/java/com/motohub/next")
read=lambda n:(root/n).read_text(encoding="utf-8")
ride=read("RideService.java")
maint=read("DailyMaintenanceJob.java")
prompt=read("AppUpdateNotice.java")
ui=read("RideUi.java")
widget=read("MotoWidgetProvider.java")
obd=read("ObdDashboardActivity.java")
main=read("MainActivity.java")
assert "manager.notify(405,note)" in ride
assert "nm.cancel(405)" in ride
assert "manager.notify(407,notification)" in maint
assert "manager.cancel(407)" in maint and "notificationManager.cancel(407)" in maint
assert "setSmallIcon(R.drawable.ic_notification)" in maint
assert "snoozed_version" in prompt and "snoozed_at" in prompt
assert "UpdatePromptPolicy.shouldPrompt(" in prompt
assert "내일 다시 알림" in prompt
assert "last_seen" not in prompt
assert "RideTimePolicy.eligible(" in ride and "RideTimePolicy.moving(" in ride
assert "liveMovingMillis=0,liveStationaryMillis=0" in ride
assert "GPS 관측 구간" in ui and "RideTimePolicy.movingAverage(" in ui
assert 'values[i]=a.text("—",24,a.fg,true)' in ui
assert 'MotoWidgetProvider.refresh(this)' in ride
assert 'nowForWidget-lastWidgetRefreshElapsed>=30_000' in ride
assert 'RideService.liveDistanceKm' in widget and 'WidgetMaintenanceSummary.nextDistance(' in widget
assert 'ObdSignalAge.label(' in obd
assert 'GPS 관측 정차 추정' in main and '미관측' in main
assert 'write(next);DailyMaintenanceJob.checkSoon(this)' in main
assert "Math.round((old+info.distanceKm)*10)/10.0" in main
print("PASS UX55 21 code invariants: alerts, GPS timing, updates, OBD, widget, maintenance")
PY
