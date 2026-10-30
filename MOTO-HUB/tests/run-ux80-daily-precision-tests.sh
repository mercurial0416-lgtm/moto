#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
javac -encoding UTF-8 -d "$tmp" app/src/main/java/com/motohub/next/MoneyStep.java app/src/main/java/com/motohub/next/OdometerGuard.java tests/UX80MoneyAndOdoTest.java
java -cp "$tmp" com.motohub.next.UX80MoneyAndOdoTest
python3 - <<'PY'
from pathlib import Path
base=Path("app/src/main/java/com/motohub/next")
def read(f): return (base/f).read_text()
main=read("MainActivity.java")
quick=read("QuickEntryUi.java")
home=read("GarageHomeUi.java")
service=read("ServiceStatusUi.java")
inbox=read("RideCompletionInbox.java")
parking=read("ParkingSpotUi.java")
updater=read("UpdateManifest.java")
ci=Path("../.github/workflows/moto-ux-validation.yml").read_text()
assert 'moneyStepButtons(parent,e)' in main
assert 'new String[]{"100만","300만","500만","700만","1000만"}' not in main
assert 'if(title.equals("새 바이크 구매가")||title.equals("기존 바이크 예상 판매가"))moneyStepButtons(parent,input)' in main
assert 'String bound=RideBikeBinding.readStrict(track)' in main
assert 'RideMileageIntegrity.validCompleted(track)' in main
assert '.put("bikeId",boundBike)' in inbox
assert 'void promptRideOdometer(JSONObject ride)' in main
assert 'if(bound.isEmpty())' in main
assert 'UndoLastSave.offer(a,beforeSave,a.db.toString(),"주유")' in quick
assert 'UndoLastSave.offer(this,beforeSave,db.toString(),title,' in main
assert 'UndoLastSave.offer(a,beforeSave,a.db.toString(),"정비 완료")' in service
assert 'UndoLastSave.available(a)' in home and 'void showQuickTools(' in home
assert 'FuelNearDuplicate.near(' in quick and 'lastSaveClick' in quick
assert 'DailyMaintenanceJob.clearReminderState(a)' in service
assert 'parkedInfo(' in parking and 'parkingAccuracyM' in parking
assert 'moto-hub-v"+versionCode+"/"' in updater
assert '-PmotoReleaseCode=$CODE' in ci and "versionCode='$CODE'" in ci
print("PASS UX80 purchase adjustments, saved-ride identity, undo, warnings, simplified home and OTA consistency")
PY
