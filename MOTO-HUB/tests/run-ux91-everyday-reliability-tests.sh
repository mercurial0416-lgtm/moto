#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
out="$(mktemp -d)"
trap 'rm -rf "$out"' EXIT
javac -encoding UTF-8 -d "$out" \
  app/src/main/java/com/motohub/next/UpdatePromptPolicy.java \
  tests/UpdateCheckCadenceTest.java
java -cp "$out" com.motohub.next.UpdateCheckCadenceTest
python3 - <<'PY'
from pathlib import Path
p=Path("app/src/main/java/com/motohub/next")
notice=(p/"AppUpdateNotice.java").read_text()
navigation=(p/"NavigationApps.java").read_text()
fuel=(p/"FuelStats.java").read_text()
assert 'if(!UpdatePromptPolicy.checkDue(now,last,prefs.getBoolean("last_check_succeeded",true)))return;' in notice
assert '.putBoolean("last_check_succeeded",false)' in notice
assert '.putBoolean("last_check_succeeded",true)' in notice
assert notice.index('.putBoolean("last_check_succeeded",true)')>notice.index('new UpdateManifest(')
fallback=navigation[navigation.index('static void chooseInstalledAndLaunch'):navigation.index('static void openPreferred')]
assert 'activity.startActivity(launch);' in fallback
assert fallback.index('activity.startActivity(launch);')<fallback.index('.putString(KEY,target.id).commit()')
assert 'catch(RuntimeException failed){open(activity,target);return;}' in fallback
assert 'if(bikeId==null||bikeId.isEmpty())' in fuel
assert 'if(x==null||!bikeId.equals(x.optString("vehicleId"))' in fuel
print("PASS UX91: manifest fail-retry, successful nav-before-persist, null-safe fuel data")
PY
