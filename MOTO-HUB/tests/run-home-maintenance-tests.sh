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
assert 'String condition=condition(bike);' in ui and 'int statusColor=' in ui, 'hero maintenance status is computed from selected bike'
assert '("점검 필요".equals(condition)||"곧 점검".equals(condition))?orange:soft' in ui, 'status tone is derived from maintenance risk'
assert 'condition+"' in ui or 'condition,' in ui, 'visible status uses computed condition'
assert 'String nearest=ServiceDueSummary.nearest(' in ui and 'return nearest.equals("미설정")?summary:nearest;' in ui
assert 'MaintenanceAlertWindow.status(' in ui
print('PASS: 6 home UI wiring checks')
PYTEST
