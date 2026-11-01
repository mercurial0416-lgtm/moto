#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
javac -encoding UTF-8 -d "$tmp" \
 app/src/main/java/com/motohub/next/RestoreRideGate.java \
 app/src/main/java/com/motohub/next/ObdReadOnlyProtocol.java \
 app/src/main/java/com/motohub/next/ObdSensorReading.java \
 app/src/main/java/com/motohub/next/ObdPidCapabilities.java \
 tests/Ux103StorageFuelTest.java
java -cp "$tmp" com.motohub.next.Ux103StorageFuelTest
python3 - <<'PY'
from pathlib import Path
root=Path("app/src/main/java/com/motohub/next")
main=(root/"MainActivity.java").read_text()
dash=(root/"ObdDashboardActivity.java").read_text()
monitor=(root/"ObdAutoSpeedMonitor.java").read_text()
service=(root/"RideService.java").read_text()
assert 'commitFormRecordAsync(next,beforeSave,key,title,item,attachAfterSave.isChecked())' in main
assert 'LocalSafetyBackup.snapshot(this,Store.parse(beforeSave))' in main
assert 'atomic(disk,bytes);' in main
assert 'if(committed)formWriteRevision++' in main
assert 'RestoreRideGate.endFormWrite();' in main
assert 'if(!safe)error("기록은 저장됐지만' in main
assert 'if(!checkpointOk)error("기록은 저장됐지만' in main
assert 'moto-async-state-reload' in main
assert 'restoreReadRunning.compareAndSet(false,true)' in main
assert 'Store.parse(new String(disk.readFully(),StandardCharsets.UTF_8))' in main
assert 'obdFuelPercent' in service and 'obdFuelAt' in service
assert 'fromAllResponses(supported00,supported20,supported40,supportedA0)' in monitor
assert 'listener.allowOdometer()' in monitor
assert 'capabilities.forPid(pid)==ObdPidCapabilities.Support.YES' in monitor
assert 'fuel.setText(fuelFresh?' in dash
print("PASS UX103 activity/OBD background, data checkpoint warning and unsupported fuel safety wiring")
PY
