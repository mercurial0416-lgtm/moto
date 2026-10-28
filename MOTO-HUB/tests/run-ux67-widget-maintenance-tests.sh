#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
javac -encoding UTF-8 -d "$tmp" \
  app/src/main/java/com/motohub/next/MaintenancePlanner.java \
  app/src/main/java/com/motohub/next/HomeMaintenanceStatus.java \
  tests/MaintenanceImportedDataTest.java
java -cp "$tmp" com.motohub.next.MaintenanceImportedDataTest
python3 - <<'PY'
from pathlib import Path
root=Path("app/src/main/java/com/motohub/next")
widget=(root/"MotoQuickWidgetProvider.java").read_text()
maint=(root/"MaintenanceAlertWindow.java").read_text()
assert widget.count("Intent.FLAG_ACTIVITY_SINGLE_TOP") == 2, "Both widget actions must reuse MainActivity"
assert "onAppWidgetOptionsChanged" in widget, "Resized widgets must be rebound"
assert "onUpdate(context,manager,new int[]{id})" in widget
assert "validOdo=Double.isFinite(odometer)" in maint
assert "safeKm=validOdo&&Double.isFinite(rawKm)&&rawKm>=0" in maint
assert "catch(java.time.DateTimeException invalidDate)" in maint
print("PASS UX67 widget intent reuse, resize refresh and imported maintenance fallback")
PY
