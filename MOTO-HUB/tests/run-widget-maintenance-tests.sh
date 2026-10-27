#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
out="$(mktemp -d)"
trap 'rm -rf "$out"' EXIT
javac -encoding UTF-8 -d "$out" \
 app/src/main/java/com/motohub/next/MaintenancePlanner.java \
 app/src/main/java/com/motohub/next/HomeMaintenanceStatus.java \
 app/src/main/java/com/motohub/next/WidgetMaintenanceSummary.java \
 tests/WidgetMaintenanceSummaryTest.java
java -cp "$out" com.motohub.next.WidgetMaintenanceSummaryTest
python3 - <<'PYTEST'
from pathlib import Path
widget=Path('app/src/main/java/com/motohub/next/MotoWidgetProvider.java').read_text()
assert 'getFilesDir()' not in widget and 'widget_bike_status' not in widget
assert 'Store.due(service,bike,today)' not in widget
assert 'EXTRA_ONE_TAP_RIDE' in widget
assert '예정 정비 정상' not in widget
print('PASS: maintenance unit tests retained; launcher stays private and minimal')
PYTEST
