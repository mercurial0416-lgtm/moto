#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
temp="$(mktemp -d)";trap 'rm -rf "$temp"' EXIT
javac -encoding UTF-8 -d "$temp" \
 app/src/main/java/com/motohub/next/{ObdOdometer,OdoAutoSyncPolicy,ObdPidCapabilities,ObdReadOnlyProtocol}.java \
 tests/Ux104OdoAutoSyncTest.java
java -cp "$temp" com.motohub.next.Ux104OdoAutoSyncTest
python3 - <<'PY'
from pathlib import Path
p=Path("app/src/main/java/com/motohub/next")
monitor=(p/"ObdAutoSpeedMonitor.java").read_text()
service=(p/"RideService.java").read_text()
main=(p/"MainActivity.java").read_text()
inbox=(p/"RideCompletionInbox.java").read_text()
garage=(p/"GarageHomeUi.java").read_text()
developer=(p/"DeveloperObdScreen.java").read_text()
assert 'capabilities.forPid("01A6")==ObdPidCapabilities.Support.YES' in monitor
assert 'listener.allowOdometer()' in monitor
assert 'OdoAutoSyncPolicy.consistent(a,b)' in monitor
assert 'listener.odometer(verified)' in monitor
assert 'OdometerAutoSyncInbox.offer(RideService.this,pendingBikeId,km,km' in service
assert 'OdometerAutoSyncInbox.enable(activity,id,baseline)' in developer
assert 'OdometerAutoSyncInbox.disable(activity)' in developer
assert 'RideService.running||jsonImportRunning' in main
assert 'RestoreRideGate.beginFormWrite()' in main
assert 'OdometerAutoSyncInbox.acknowledged(this,bikeId,offered)' in main
assert 'atomic(disk,original.toString(2)' in main
assert 'LocalSafetyBackup.checkpoint(this,original)' in main
assert 'RideMileageEstimate.record(context,track,boundBike,result.distanceKm)' in inbox
assert 'GPS 기록 누적 추정' in garage
assert '계기판 km와 별도' in garage
assert 'obd' not in (p/"RideMileageEstimate.java").read_text().lower()
print("PASS v567 source wiring: explicit ODO consent, safe background write and independent GPS ledger")
PY
