#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
out="$(mktemp -d)"
trap 'rm -rf "$out"' EXIT
javac -encoding UTF-8 -d "$out" \
  app/src/main/java/com/motohub/next/MaintenancePlanner.java \
  app/src/main/java/com/motohub/next/HomeMaintenanceStatus.java \
  tests/HomeMaintenanceStatusTest.java
java -cp "$out" com.motohub.next.HomeMaintenanceStatusTest
python3 - <<'PYTEST'
from pathlib import Path
ui=Path('app/src/main/java/com/motohub/next/GarageHomeUi.java').read_text()
assert 'MaintenanceAlertWindow.status(service,a.today(),b.optDouble("odo"))' in ui
assert 'String condition=condition(bike);' in ui and 'TextView state=a.text("●  "+condition' in ui, 'hero shows one compact maintenance state'
assert 'int statusColor=' in ui and '?orange:soft' in ui, 'maintenance status has coherent accent'
assert 'alert.setOnClickListener(v->showNotices())' in ui, 'attention-needed maintenance opens detail'
assert 'String nearest=ServiceDueSummary.nearest(' in ui and 'return nearest.equals("미설정")?summary:nearest;' in ui
assert 'MaintenanceAlertWindow.status(' in ui
print('PASS: 6 home UI wiring checks')
PYTEST
