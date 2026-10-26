#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
javac -encoding UTF-8 -d "$tmp" \
 app/src/main/java/com/motohub/next/NavigationChoice.java \
 tests/NavigationChoiceTest.java
java -cp "$tmp" com.motohub.next.NavigationChoiceTest
python3 - <<'PY'
from pathlib import Path
p=Path("app/src/main/java/com/motohub/next")
manifest=Path("app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
play_manifest=Path("app/src/playRelease/AndroidManifest.xml").read_text(encoding="utf-8")
gradle=Path("app/build.gradle").read_text(encoding="utf-8")
navi=(p/"NavigationApps.java").read_text(encoding="utf-8")
ride=(p/"RideUi.java").read_text(encoding="utf-8")
service=(p/"RideService.java").read_text(encoding="utf-8")
updater=(p/"AppUpdater.java").read_text(encoding="utf-8")
checks={
 "only explicit queryable navigation packages":all(f'<package android:name="{pid}"/>' in manifest for pid in
  ("com.locnall.KimGiSa","com.nhn.android.nmap","com.skt.tmap.ku")),
 "no QUERY_ALL_PACKAGES":"QUERY_ALL_PACKAGES" not in manifest,
 "no GPS location handed to third party":all(z not in navi for z in
  ("getLastKnownLocation","putExtra(\"gps","EXTRA_LATITUDE","Sharing.offer")),
 "no implicit nav start from ride UI":"NavigationApps.choose(a)" in ride,
 "notification uses selected installed app":"NavigationApps.preferred(this)" in service and
   "NavigationApps.installedLaunchIntent(this,choice)" in service,
 "stores only selected app ID":'putString(KEY,app.id)' in navi,
 "store-specific build variant":'playRelease {' in gradle and '"STORE_DISTRIBUTION", "true"' in gradle,
 "store removes installer permission":'REQUEST_INSTALL_PACKAGES" tools:node="remove"' in play_manifest,
 "store updater is Play link only":'if(BuildConfig.STORE_DISTRIBUTION)' in updater and
  "market://details?id=" in updater,
 "sideload APK updater remains for existing fixed signed channel":'void validateArchive(File file,UpdateManifest m)' in updater,
}
for name,passed in checks.items():assert passed,name
print("PASS UX49 multnav/store source checks",len(checks))
PY
