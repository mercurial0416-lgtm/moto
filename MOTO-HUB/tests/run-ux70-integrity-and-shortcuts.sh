#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
python3 - <<'PY'
from pathlib import Path
p=Path("app/src/main/java/com/motohub/next")
inbox=(p/"RideCompletionInbox.java").read_text()
activity=(p/"MainActivity.java").read_text()
merger=(p/"JsonDataMerger.java").read_text()
footprint=(p/"UpdateDataFootprint.java").read_text()
home=(p/"GarageHomeUi.java").read_text()
obd=(p/"DeveloperObdScreen.java").read_text()
ci=Path("../.github/workflows/moto-ux-validation.yml").read_text()
assert 'LIMIT=10' in inbox and 'putString(KEY,next.toString()).commit()' in inbox
assert 'static synchronized JSONObject unread(' in inbox and 'seen(Context c,String id)' in inbox
assert 'void showRideCompletionHistory()' in activity and 'void showPendingRideSummary()' in activity
assert 'void onWindowFocusChanged(boolean focused)' in activity
assert 'new AlertDialog.Builder(this).setTitle("최근 완료 라이딩 · 최대 10건")' in activity
assert 'MessageDigest.getInstance("SHA-256")' in footprint
assert 'verifyRegular(file)' in footprint and 'digest.update(buffer,0,n)' in footprint
assert 'String baseline=UpdateDataFootprint.capture' not in footprint
assert 'static Result merge(JSONObject current,JSONObject imported,Map<String,String> confirmed)' in merger
assert 'JsonDataMerger.conflicts(' in activity
assert 'selectJsonMergeBikes(' in activity and 'commitJsonMerge(source,new java.util.HashMap<>(selections))' in activity
assert 'HomeShortcutOrder.parse(' in home and 'quickAction("fuel")' in home and 'quickAction("service")' in home
assert 'setMultiChoiceItems(labels,visible' in home
assert 'ObdLinkQuality.score(' in obd and 'qualityScore<55' in obd
assert 'motoReleaseCode=552' in ci and "versionCode='552'" in ci
print("PASS UX70 capped persistent recaps, full-file hash audit, explicit JSON bike mapping, OBD quality and home shortcuts")
PY
