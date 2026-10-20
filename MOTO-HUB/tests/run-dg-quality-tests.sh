#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
BIN=$(mktemp -d)
trap 'rm -rf "$BIN"' EXIT
javac -encoding UTF-8 -d "$BIN" \
 app/src/main/java/com/motohub/next/{BackupFormat,TrackFile,BackupBundle,BackupIntegrity,BackupReadiness,RestoreArchive,RestoreCapacity}.java \
 tests/{RestoreCapacityTest,BackupTenThousandTest}.java
java -cp "$BIN" com.motohub.next.RestoreCapacityTest
java -Xmx768m -cp "$BIN" com.motohub.next.BackupTenThousandTest
python3 - <<'PYS'
from pathlib import Path
root=Path('app/src/main/java/com/motohub/next')
main=(root/'MainActivity.java').read_text()
restore=(root/'RestoreArchive.java').read_text()
assert 'RestoreCapacity.ensure' in restore
assert 'RestoreCapacity.ensure(RestoreCapacity.available(getFilesDir()),required)' in main
assert 'ready.requiredSpace(getFilesDir())' in main
assert 'spaceNote+=' in main
assert 'versionCode 517' in Path('app/build.gradle').read_text()
assert 'Sharing.start(' not in (root/'RideService.java').read_text()
print('PASS DG disk-space preflight, UI and privacy wiring')
PYS
