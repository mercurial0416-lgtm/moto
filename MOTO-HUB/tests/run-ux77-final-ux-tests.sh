#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
python3 - <<'PY'
from pathlib import Path
src=Path("app/src/main/java/com/motohub/next")
hub=(src/"HubScreens.java").read_text()
home=(src/"GarageHomeUi.java").read_text()
obd=(src/"DeveloperObdScreen.java").read_text()
dates=(src/"GarageSearchDate.java").read_text()
ci=Path("../.github/workflows/moto-ux-validation.yml").read_text()
assert 'mode.equals("주유")&&(!fuel||service)' in hub
assert 'RideHistorySearch.terms(input.getText().toString())' in hub
assert 'RideHistorySearch.matchesIndex(ride.index,terms)' in hub
assert 'GarageSearchDate.iso(from.getText().toString())' in hub
assert 'datesDialog.setOnShowListener' in hub
assert 'datesDialog.dismiss();' in hub
assert 'dialog.setOnShowListener' in home and 'if(shown==0)' in home
assert 'if(edit.commit()){dialog.dismiss();' in home
assert 'ObdConnectionState.label(' in obd
assert 'LocalDate.of(' in dates
assert '-PmotoReleaseCode=546' in ci
assert "versionCode='546'" in ci
print("PASS UX77 date filters, fuel classification, retained dialog and OBD dashboard")
PY
