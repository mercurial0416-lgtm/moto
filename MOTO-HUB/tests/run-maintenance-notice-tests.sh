#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
out="$(mktemp -d)"
trap 'rm -rf "$out"' EXIT
javac -encoding UTF-8 -d "$out" \
 app/src/main/java/com/motohub/next/MaintenancePlanner.java \
 app/src/main/java/com/motohub/next/HomeMaintenanceStatus.java \
 app/src/main/java/com/motohub/next/MaintenanceNoticeOrder.java \
 tests/MaintenanceNoticeOrderTest.java
java -cp "$out" com.motohub.next.MaintenanceNoticeOrderTest
python3 - <<'PYTEST'
from pathlib import Path
s=Path('app/src/main/java/com/motohub/next/GarageHomeUi.java').read_text()
assert 'MaintenanceNoticeOrder.prioritized(history,a.today(),b.optDouble("odo"))' in s
assert 'LatestServiceByType.latest(a.items("services"),b.optString("id"))' in s
assert 'Math.min(5,notices.size())' in s
assert 'notices.size()-5' in s
assert 'MaintenancePlanner.caption(status)' in s
print('PASS: 5 maintenance dialog integration checks')
PYTEST
