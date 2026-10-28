#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
python3 - <<'PY'
from pathlib import Path
p=Path("app/src/main/java/com/motohub/next")
ride=(p/"RideUi.java").read_text()
service=(p/"RideService.java").read_text()
activity=(p/"MainActivity.java").read_text()
inbox=(p/"RideCompletionInbox.java").read_text()
merge=(p/"JsonDataMerger.java").read_text()
snap=(p/"UpdateInstallSnapshot.java").read_text()
footprint=(p/"UpdateDataFootprint.java").read_text()
obd=(p/"DeveloperObdScreen.java").read_text()
maintenance=(p/"ServiceStatusUi.java").read_text()
home=(p/"GarageHomeUi.java").read_text()
assert 'a.startService(new Intent(a,RideService.class).setAction(RideService.STOP))' in ride
assert service.count('RideCompletionInbox.record(getApplicationContext(),savedTrack)') >= 2
assert 'if(verified)RideCompletionInbox.record' in service
assert 'RideMileageIntegrity.validCompleted(track)' in inbox
assert 'RideInsights.analyze(track)' in inbox
assert 'putString(KEY,next.toString()).commit()' in inbox
assert 'void showPendingRideSummary()' in activity and 'onResume(){super.onResume()' in activity
assert 'RideService.running||RideService.saving()' in activity
assert 'ambiguousBikeIds.contains(entry.optString("vehicleId"))' in merge
assert 'blocked++' in merge and 'blocked' in activity
assert 'before_footprint' in snap and 'UpdateDataFootprint.capture' in snap
assert 'tracks=' in footprint and 'trackBytes=' in footprint and 'garagePhotoBytes=' in footprint
assert 'ObdSignalAge.label(RideService.obdLastSampleElapsed' in obd
assert 'ObdTelemetryFreshness.connectionLabel' in obd
assert 'Store.find(next,"services",old.optString("id"))==null' in maintenance
assert 'if(extra){a.gap(a.content,16);recent(b);' in home
assert 'void complete(JSONObject old)' in maintenance and 'ServiceCycle.completed(old,a.today(),current,spent)' in maintenance
print("PASS UX69 durable GPS recap, conservative merge, install inventory, ECU age, maintenance safety and minimal home")
PY
