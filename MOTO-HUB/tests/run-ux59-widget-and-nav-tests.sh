#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
python3 - <<'PY'
from pathlib import Path
import xml.etree.ElementTree as ET
res=Path("app/src/main/res")
src=Path("app/src/main/java/com/motohub/next")
andns="{http://schemas.android.com/apk/res/android}"
normal=ET.parse(res/"xml/moto_widget_info.xml").getroot()
quick=ET.parse(res/"xml/moto_widget_quick_info.xml").getroot()
assert int(normal.attrib[andns+"minHeight"].removesuffix("dp"))<=80
assert int(normal.attrib[andns+"minWidth"].removesuffix("dp"))<=225
assert int(quick.attrib[andns+"minHeight"].removesuffix("dp"))<=50
assert int(quick.attrib[andns+"minWidth"].removesuffix("dp"))<=110
widget=ET.parse(res/"layout/moto_widget.xml").getroot()
quickLayout=ET.parse(res/"layout/moto_widget_quick.xml").getroot()
ids={e.attrib.get(andns+"id") for e in quickLayout.iter()}
assert "@+id/quick_ride" in ids and "@+id/quick_nav" in ids
manifest=ET.parse("app/src/main/AndroidManifest.xml").getroot()
receivers={x.attrib[andns+"name"] for x in manifest.iter("receiver")}
assert ".MotoWidgetProvider" in receivers
assert ".MotoQuickWidgetProvider" in receivers
foreground=ET.parse(res/"drawable/ic_launcher_foreground.xml").getroot()
colors=" ".join(x.attrib.get(andns+"fillColor","") for x in foreground)
assert "#FF762B" in colors and "#FF8E35" in colors
assert "class MotoQuickWidgetProvider" in (src/"MotoQuickWidgetProvider.java").read_text()
quickSrc=(src/"MotoQuickWidgetProvider.java").read_text()
assert "EXTRA_ONE_TAP_RIDE,true" in quickSrc
assert "EXTRA_NAVIGATION,true" in quickSrc
assert "PendingIntent.FLAG_IMMUTABLE" in quickSrc
widgetSrc=(src/"MotoWidgetProvider.java").read_text()
assert 'views.setViewVisibility(R.id.widget_nav,View.VISIBLE)' in widgetSrc
assert 'widget_header' in widgetSrc
assert "ride_display" in (src/"RideUi.java").read_text()
assert "live.setVisibility(focus?android.view.View.GONE" in (src/"RideUi.java").read_text()
assert "NavigationApps.choose(a)" in (src/"RideUi.java").read_text()
pin=(src/"MotoWidgetPin.java").read_text()
assert "isRequestPinAppWidgetSupported()" in pin
assert "requestPinAppWidget(" in pin
assert "line(\"초소형 위젯 추가\"" in (src/"HubScreens.java").read_text()
assert "if(focused)return 5000;" in (src/"RideScreenRefreshPolicy.java").read_text()
print("PASS UX59 icon, adaptive widget 2x1/4x1, immutable shortcuts, nav, focus, pinning, privacy")
PY
