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
assert '"내 바이크"' in ui and 'String condition=condition(bike);TextView badge=' in ui, 'hero has one dedicated maintenance status'
assert 'condition.equals("곧 점검")?0xFFFFD58A' in ui
assert 'condition.equals("곧 점검")?0xFF503E20' in ui
assert 'String nearest=ServiceDueSummary.nearest(' in ui and 'return nearest.equals("미설정")?summary:nearest;' in ui
assert 'MaintenanceAlertWindow.status(' in ui
print('PASS: 6 home UI wiring checks')
PYTEST
