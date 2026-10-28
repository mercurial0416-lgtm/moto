#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
python3 - <<'PY'
from pathlib import Path
root=Path("app/src/main/java/com/motohub/next")
nav=(root/"NavigationApps.java").read_text(encoding="utf-8")
ride=(root/"RideUi.java").read_text(encoding="utf-8")
main=(root/"MainActivity.java").read_text(encoding="utf-8")
assert 'if(installedLaunchIntent(activity,selectedApp)==null)' in nav
guard=nav.index('if(installedLaunchIntent(activity,selectedApp)==null)')
persist=nav.index('.edit().putString(KEY,selectedApp.id).commit()')
assert guard<persist, "Unavailable app must never override working default"
assert 'openStore(activity,selectedApp)' in nav
assert 'chooseInstalledAndLaunch(activity)' in nav
assert 'activity.suppressUpdatePromptUntilElapsed=android.os.SystemClock.elapsedRealtime()+120_000L;' in nav
assert 'activity.startActivity(launch);return;' in nav
assert 'getLastKnownLocation' not in nav and 'EXTRA_LATITUDE' not in nav
assert 'if(!focus){' in ride and 'values[0].setText' in ride
assert 'a.hasWindowFocus())a.render();},800)' in ride
assert 'if(RideService.running)' in ride and 'RideService.STOP' in ride
assert 'rideStartConsent.consume(' in ride
assert 'renderPreservingPosition()' in main
print("PASS UX65: uninstalled navigation refusal, installed fallback, nav resume and no hidden gauge redraw")
PY
