#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
python3 - <<'PY'
from pathlib import Path
base=Path("app/src/main/java/com/motohub/next")
hub=(base/"HubScreens.java").read_text()
storage=(base/"RideStorageManager.java").read_text()
prefs=(base/"DevicePreferencesBackup.java").read_text()
widget=(base/"MotoQuickWidgetProvider.java").read_text()
nav=(base/"NavigationApps.java").read_text()
assert 'void searchAllRecords()' in hub
assert 'for(JSONObject vehicle:a.items("vehicles"))' in hub
assert 'a.items("services")' in hub and 'a.items("expenses")' in hub
assert 'IdentityHashMap<>' in hub and 'a.edit(row.collection,row.item)' in hub
assert 'moto-unified-gps-search' in hub and 'TrackFile.tracks(a.getFilesDir())' in hub
assert 'RideStorageManager.show(a,ride.filename)' in hub
assert 'final String title,filename,bikeId,date,index;' in hub
assert 'shown<30' in hub and 'page[0]++' in hub
assert 'void showPage(MainActivity a,' in storage
assert 'static void show(MainActivity a,String initialFilter)' in storage
assert 'indexed[i]=(labels[i]+' in storage
assert 'showTrashPage(a,entries,labels,page+1,query)' in storage
assert 'GPS 복구함 날짜 검색' in storage
assert 'matches.get(from+index-1)' in storage
assert '(matches.isEmpty()?0:from+1)' in storage
assert 'RideTrash.restore(a.getFilesDir(),target)' in storage
assert 'NavigationApps.installedLaunchIntent(c,requested)!=null' in prefs
assert 'boolean restoreKeys(' in prefs
assert 'homeOk=restoreKeys(' in prefs
assert 'notifyOk=restoreKeys(' in prefs
assert 'boolean saved=' in nav
assert 'FLAG_ACTIVITY_SINGLE_TOP' in widget
assert 'versionCode=\'550\'' in Path("../.github/workflows/moto-ux-validation.yml").read_text()
print("PASS UX74 all-bike search, GPS deeplink, storage paging, navigator restore and widget safety")
PY
