#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
javac -encoding UTF-8 -d "$tmp" app/src/main/java/com/motohub/next/RideDistanceReviewPolicy.java tests/RideDistanceReviewPolicyTest.java
java -cp "$tmp" com.motohub.next.RideDistanceReviewPolicyTest
python3 - <<'PY'
from pathlib import Path
p=Path('app/src/main/java/com/motohub/next')
dash=(p/'ObdDashboardActivity.java').read_text()
service=(p/'RideService.java').read_text()
checks={
 'landscape opt in':'SCREEN_ORIENTATION_LANDSCAPE' in dash,
 'keep screen awake opt in':'FLAG_KEEP_SCREEN_ON' in dash,
 'keep awake released on pause':'onPause(){visible=false;handler.removeCallbacks(update);getWindow().clearFlags' in dash,
 'persisted preferences':'obd_dashboard_preferences' in dash,
 'distance suggestion only after verified auto save':'RideDistanceReviewPolicy.suggestion(summary.distanceKm,true)' in service,
 'no GPS-to-ODO silent mutation':'setOdometer(' not in service,
}
for name,good in checks.items():assert good,name
print(f"PASS UX46 source checks {len(checks)}")
PY
