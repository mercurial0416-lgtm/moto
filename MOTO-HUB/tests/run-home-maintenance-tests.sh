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
assert 'HomeMaintenanceStatus.summary(schedules,a.today(),b.optDouble("odo"))' in ui
assert 'condition(bike).equals("곧 점검")?"MY BIKE · SERVICE SOON"' in ui
assert 'condition.equals("곧 점검")?0xFFFFD58A' in ui
assert 'condition.equals("곧 점검")?0xFF503E20' in ui
assert 'if(summary.equals("점검 필요")||summary.equals("곧 점검"))return summary;' in ui
assert 'HomeMaintenanceStatus.safeStatus(' in ui
print('PASS: 6 home UI wiring checks')
PYTEST
