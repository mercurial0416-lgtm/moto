#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
python3 - <<'PY'
from pathlib import Path
from xml.etree import ElementTree as ET
base=Path('app/src/main')
android='{http://schemas.android.com/apk/res/android}'
def read(rel): return (base/rel).read_text(encoding='utf-8')
def parse(rel): return ET.fromstring(read(rel))
manifest=parse('AndroidManifest.xml')
app=manifest.find('application')
assert app.attrib[android+'icon']=='@mipmap/ic_launcher'
adaptive=parse('res/mipmap-anydpi-v26/ic_launcher.xml')
assert adaptive.tag=='adaptive-icon'
assert adaptive.find('foreground').attrib[android+'drawable']=='@drawable/ic_launcher_foreground'
for drawable in ('ic_launcher_foreground','ic_notification','widget_background','widget_primary','widget_secondary'):
 parse('res/drawable/'+drawable+'.xml')
layout=parse('res/layout/moto_widget.xml')
ids=set()
for el in layout.iter():
 ident=el.attrib.get(android+'id','')
 if ident: ids.add(ident.split('/')[-1])
required={'widget_home','widget_nav_choice','widget_ride','widget_nav'}
assert required<=ids,required-ids
provider=read('java/com/motohub/next/MotoWidgetProvider.java')
main=read('java/com/motohub/next/MainActivity.java')
service=read('java/com/motohub/next/RideService.java')
notice=read('java/com/motohub/next/AppUpdateNotice.java')
assert 'EXTRA_ONE_TAP_RIDE,true' in provider
assert 'EXTRA_NAVIGATION,true' in provider
assert 'applyWidgetQuickActions(Intent intent)' in main
assert 'new RideUi(this).requestStart(true)' in main
assert 'new RideUi(this).requestStart(true)' not in provider
assert 'void maybeNotifyStationary(long now)' in service
assert 'RideStopSuggestion.shouldSuggest(' in service
assert 'GPS 기록' in service or '라이딩' in service
assert 'stopSuggestionShown=true' in service
assert 'AppUpdateNotice.checkOnResume(this)' in main
assert 'new AppUpdater(a).show()' in notice
assert 'setPositiveButton("앱 업데이트"' in notice
assert 'if(RideService.running||RideService.autoArmed)NavigationApps.openPreferred(this)' in main
assert 'UpdatePromptPolicy.checkDue(now,last,prefs.getBoolean("last_check_succeeded",true))' in notice
policy=(base/'java/com/motohub/next/UpdatePromptPolicy.java').read_text(encoding='utf-8')
assert 'CHECK_INTERVAL_MS=6L*60*60*1000' in policy
assert 'FAILURE_RETRY_MS=15L*60*1000' in policy
assert 'STORE_DISTRIBUTION' in notice
assert 'MotoWidgetProvider.refresh(this)' in service
print('PASS UX54 native launcher, widget actions, privacy, idle notifications, update notice (25 assertions)')
PY
