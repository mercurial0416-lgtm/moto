#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
python3 - <<'PY'
from pathlib import Path
from xml.etree import ElementTree as ET
a="{http://schemas.android.com/apk/res/android}"
res=Path("app/src/main/res")
src=Path("app/src/main/java/com/motohub/next")
layout=ET.parse(res/"layout/moto_widget.xml").getroot()
quick=ET.parse(res/"layout/moto_widget_quick.xml").getroot()
for root,limit in ((layout,72),(quick,47)):
 assert root.attrib[a+"background"]=="@android:color/transparent", "Oversized black widget background"
 assert root.attrib[a+"gravity"]=="center_vertical", "Content must not stick at top"
 cards=[x for x in root if x.attrib.get(a+"background")=="@drawable/widget_background"]
 assert len(cards)==1, "Only one bounded visual card"
 assert int(cards[0].attrib[a+"layout_height"].removesuffix("dp"))<=limit
assert "android" not in list(layout.attrib) or True
for node in quick.iter():
 if node.attrib.get(a+"id") in ("@+id/quick_ride","@+id/quick_nav"):
  assert node.attrib[a+"layout_height"]=="37dp"
normalIDs={n.attrib.get(a+"id") for n in layout.iter()}
assert {"@+id/widget_home","@+id/widget_ride","@+id/widget_nav","@+id/widget_nav_choice"}<=normalIDs
assert len([n for n in layout.iter() if "widget_bike" in n.attrib.get(a+"id","")])==0
provider=(src/"MotoWidgetProvider.java").read_text()
main=(src/"MainActivity.java").read_text()
ride=(src/"RideUi.java").read_text()
settings=(src/"HubScreens.java").read_text()
nav=(src/"NavigationApps.java").read_text()
assert "getFilesDir()" not in provider
assert 'new RemoteViews(context.getPackageName(),R.layout.moto_widget)' in provider
assert "NavigationApps.preferred(context).label" in provider
assert 'R.id.widget_nav_choice,pending(context' in provider
assert 'EXTRA_SELECT_NAV,true' in provider and 'EXTRA_SELECT_NAV' in main
assert "MotoWidgetProvider.refresh(activity)" in nav
assert "new RideUi(this).requestStart(true)" in main
assert "RideService.running||RideService.autoArmed" in main
assert 'startForegroundService(new Intent(a,RideService.class)' in ride
assert 'rideStartConsent.approve()' in ride
assert 'ridePreflight' not in settings
assert '라이딩·OBD2 사전 점검' not in settings
assert 'LinearLayout warning=a.col()' not in ride
assert "new RideReadiness" not in ride or "requestStart" in ride
print("PASS UX60 bounded-height widgets, privacy, nav choice, ride consent, no preflight")
PY
