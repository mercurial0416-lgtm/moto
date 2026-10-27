#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
python3 - <<'PY'
from pathlib import Path
from xml.etree import ElementTree
root=Path("app/src/main/java/com/motohub/next")
src=lambda n:(root/n).read_text(encoding="utf-8")
service=src("RideService.java")
activity=src("MainActivity.java")
widget=src("MotoWidgetProvider.java")
boot=src("BootRideReminder.java")
obd=src("ObdAutoSpeedMonitor.java")
mapui=src("RideMapActivity.java")
insights=src("RideInsights.java")
updater=src("AppUpdater.java")
settings=src("HubScreens.java")
assert 'GpsSyncPolicy gpsSync=new GpsSyncPolicy()' in service
assert 'gpsSync.shouldSync(writeAt)' in service and 'postDelayed(syncPendingGps' in service
assert 'output.getFD().sync();output.close()' in service
assert '"com.motohub.next.CONFIRM_RIDE_STOP"' in service
assert 'PendingIntent.getActivity(this,1,confirmStop' in service
assert 'confirmStopFromNotification(intent)' in activity
assert 'setNegativeButton("계속 기록",null)' in activity
assert 'setPositiveButton("저장하고 종료"' in activity
assert 'ObdFailureGuide.classify(' in obd and 'ObdFailureGuide.classify(' in service
assert 'MotoWidgetProvider.refresh(context)' in boot
assert 'Intent.ACTION_MY_PACKAGE_REPLACED' in boot
assert 'RideMapOverlayPolicy.validPeak(' in mapui
assert '최고속도 위치' in mapui
assert 'zoomIn()' in mapui and 'zoomOut()' in mapui
assert 'restStops' in insights and 'gpsGaps' in insights
assert '확인한' not in insights or 'gpsObservedMillis' in insights
assert 'RideDiagnostics.summarize(' in settings
assert 'BackupReadiness.inspect(' in settings
assert 'partialStatus()' in updater
assert '다운로드 중단' in updater
assert '검증된 다운로드는 유지됩니다' in activity
manifest=ElementTree.parse("app/src/main/AndroidManifest.xml").getroot()
assert "android.intent.action.MY_PACKAGE_REPLACED" in str([(i.attrib) for i in manifest.iter()])
assert 'setUseDataConnection(false)' in mapui
print("PASS UX56 24 app-level regression checks")
PY
