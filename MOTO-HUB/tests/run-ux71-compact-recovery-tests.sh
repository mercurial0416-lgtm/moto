#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
python3 - <<'PY'
from pathlib import Path
p=Path("app/src/main/java/com/motohub/next")
main=(p/"MainActivity.java").read_text()
home=(p/"GarageHomeUi.java").read_text()
hub=(p/"HubScreens.java").read_text()
inbox=(p/"RideCompletionInbox.java").read_text()
notify=(p/"RideSavedNotification.java").read_text()
snapshot=(p/"UpdateInstallSnapshot.java").read_text()
order=(p/"HomeShortcutOrder.java").read_text()
storage=(p/"RideStorageManager.java").read_text()
assert 'case"매물":listings();' not in main
assert 'tab="매물";render();edit("listings",null);' not in main
assert 'if(tab.equals("매물"))pair' not in main
assert 'a.navigate("매물")' not in home and 'a.navigate("매물")' not in hub
assert '"매물"' not in (p/"MotoDesign.java").read_text()
assert 'items("listings")' in hub, "legacy data must be kept accessible in backup"
assert 'HomeShortcutOrder.parse' in home and 'changeHomeShortcutOrder()' in home
assert 'Collections.swap' in order
assert 'RideSavedNotification.post(context)' in inbox and 'manager.notify(406' in notify
assert 'BackupGpsProof.record(this,checkedBackup.trackSha256,snapshot,"FULL".equals(part))' in main
assert 'BackupIntegrity.verify(check);' in main
assert 'RideStorageManager.show(a)' in hub
assert 'RideMileageIntegrity.validCompleted(track)' in storage
assert 'RideService.running||RideService.autoArmed' in storage
assert 'lastReport(Context context)' in snapshot and 'last_problem' in snapshot
assert 'updateRecoveryReport()' in hub and '전체 ZIP 백업' in hub
assert 'RestorePreflightDetail.text(' in main and 'ready.receipts' in main
assert 'JsonConflictPreview.text(source,id)' in main
print("PASS UX71 hidden listing UI preserving DB, ordered home, GPS backup-gated delete, restore review, manual mapping details, saved ride notification")
PY
