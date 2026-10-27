#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
python3 - <<'PY'
from pathlib import Path
from xml.etree import ElementTree
src=Path("app/src/main/java/com/motohub/next")
read=lambda n:(src/n).read_text(encoding="utf-8")
insights=read("RideInsights.java")
widget=read("MotoWidgetProvider.java")
ride=read("RideUi.java")
obd=read("ObdAutoSpeedMonitor.java")
service=read("RideService.java")
dashboard=read("ObdDashboardActivity.java")
settings=read("HubScreens.java")
mapui=read("RideMapActivity.java")
updater=read("AppUpdater.java")
assert "gapPending[0]" in insights
assert "if(gapPending[0]||gap>RideMotionGate.MAX_CONTINUOUS_FIX_MS)gaps[0]++" in insights
assert "GPS 수신률" in insights and "stationaryConfidence" in insights
assert 'views.setTextViewText(R.id.widget_ride,"▶ 기록")' in widget
assert 'getBoolean("hide_details",false)' in widget
assert 'getSharedPreferences("ride_display",0)' in ride
assert 'if(focus&&RideService.running)return;' in ride
assert 'focus?32:24' in ride
assert "ObdLinkQuality.score(" in dashboard
assert "obdConnectionSuccesses++" in service
assert "linkQuality(latency.speedAverageMs()" in obd
assert "BackupDeepCheck.check(" in settings
assert "BackupReadiness.inspect(" in settings
assert "MapCachePolicy.scan(" in mapui
assert "setUseDataConnection(false)" in mapui
assert "setTileFileSystemCacheMaxBytes" in mapui
assert "MapCachePolicy.target(getCacheDir())" in mapui and "MapCachePolicy.target(getFilesDir())" not in mapui
assert "중단된 다운로드 재시도" in updater
assert "12*60_000L" in updater
ElementTree.parse("app/src/main/AndroidManifest.xml")
print("PASS UX57 privacy, offline GPS quality, focus mode, OBD, backup, tile-cache and retry checks")
PY
