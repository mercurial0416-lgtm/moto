#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
javac -encoding UTF-8 -d "$tmp" \
 app/src/main/java/com/motohub/next/RestoreRideGate.java \
 app/src/main/java/com/motohub/next/OdoAutoSyncPolicy.java \
 tests/Ux105ReliabilityTest.java
java -cp "$tmp" com.motohub.next.Ux105ReliabilityTest
python3 - <<'PY'
from pathlib import Path
base=Path("app/src/main/java/com/motohub/next")
main=(base/"MainActivity.java").read_text()
ride=(base/"RideService.java").read_text()
odo=(base/"OdometerAutoSyncInbox.java").read_text()
dev=(base/"DeveloperObdScreen.java").read_text()
gps=(base/"RideMileageEstimate.java").read_text()
auto=ride.split("private void handleAutoSpeed(int speedKmh)",1)[1].split("private void finishUserRideAsync()",1)[0]
assert 'if(!RestoreRideGate.beginRideStart())' in auto
assert 'autoGate.reset();' in auto
assert 'finally{RestoreRideGate.endRideStart();}' in auto
assert auto.index('RestoreRideGate.beginRideStart()') < auto.index('openRideTrack();requestGpsUpdatesChecked()')
assert 'OdometerAutoSyncInbox.rebaseline(activity,configuredId,confirmed,oldKm)' in dev
assert 'OdometerSyncPolicy.checkedManual(entryKm.getText().toString(),oldKm)' in dev
assert '.remove("last").remove("lastAt").remove("pending")' in odo
assert 'first(previous,actual)' in odo and 'lastNote' in odo
assert 'suspendAfterRestore(this)' in main
zip_restore=main.split('void prepareBundleRestore(Uri uri,char[] password)',1)[1].split('void exportEncryptedBundle()',1)[0]
json_restore=main.split('void confirmImport(JSONObject next)',1)[1].split('@Override public void onBackPressed()',1)[0]
assert zip_restore.index('ready.install(')<zip_restore.index('OdometerAutoSyncInbox.suspendAfterRestore(this)')
assert json_restore.index('atomic(disk,validated.toString(2)')<json_restore.index('OdometerAutoSyncInbox.suspendAfterRestore(this)')
assert 'RestoreRideGate.isRestoring()||RestoreRideGate.isExporting()' in main
assert 'resetAfterFullRestore(this)' in zip_restore
assert 'rebuildGpsEstimateAsync()' in zip_restore
assert 'static synchronized int rebuild(Context c)' in gps
batch=gps.split('static synchronized int rebuild(Context c)',1)[1].split('static synchronized boolean resetAfterFullRestore(',1)[0]
assert batch.count('prefs.getString("ledger","{}")') == 1
assert batch.count('prefs.edit().putString("ledger",ledger.toString()).commit()') == 1
assert 'ledger.has(track.getName())' in batch
assert 'RideMileageIntegrity.validCompleted(track)' in batch
assert 'RideBikeBinding.readStrict(track)' in batch
assert 'RideInsights.analyze(track)' in batch
print("PASS UX105 source wiring: OBD interlock, rebaseline, restore isolation, batched GPS mileage")
PY
