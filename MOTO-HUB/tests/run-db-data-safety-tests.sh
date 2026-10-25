#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
out="$(mktemp -d)"
trap 'rm -rf "$out"' EXIT
javac -encoding UTF-8 -d "$out" \
 app/src/main/java/com/motohub/next/GpsFixPolicy.java \
 tests/GpsFixPolicyTest.java
java -cp "$out" com.motohub.next.GpsFixPolicyTest
bash tests/run-restore-tests.sh
python3 - <<'PYTEST'
from pathlib import Path
service=Path('app/src/main/java/com/motohub/next/RideService.java').read_text()
ui=Path('app/src/main/java/com/motohub/next/RideUi.java').read_text()
restore=Path('app/src/main/java/com/motohub/next/RestoreArchive.java').read_text()
assert 'GpsFixPolicy.acceptTime(' in service
assert 'startedElapsed=SystemClock.elapsedRealtime()' in service
assert 'SystemClock.elapsedRealtime()-RideService.startedElapsed' in ui
assert restore.index('Files.deleteIfExists(new File(internal,name).toPath())') < restore.index('writer.write(json)')
assert 'Sharing.start(' not in ui
print('PASS: GPS clock, restore commit order and no automatic sharing UI wiring')
PYTEST
