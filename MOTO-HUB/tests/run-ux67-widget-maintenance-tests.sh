#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
javac -encoding UTF-8 -d "$tmp" \
  app/src/main/java/com/motohub/next/MaintenancePlanner.java \
  app/src/main/java/com/motohub/next/HomeMaintenanceStatus.java \
  tests/MaintenanceImportedDataTest.java
java -cp "$tmp" com.motohub.next.MaintenanceImportedDataTest
python3 - <<'PY'
from pathlib import Path
root=Path("app/src/main/java/com/motohub/next")
widget=(root/"MotoQuickWidgetProvider.java").read_text()
maint=(root/"MaintenanceAlertWindow.java").read_text()
assert widget.count("Intent.FLAG_ACTIVITY_SINGLE_TOP") == 2, "Both widget actions must reuse MainActivity"
assert "onAppWidgetOptionsChanged" in widget, "Resized widgets must be rebound"
assert "onUpdate(context,manager,new int[]{id})" in widget
assert "validOdo=Double.isFinite(odometer)" in maint
assert "safeKm=validOdo&&Double.isFinite(rawKm)&&rawKm>=0" in maint
assert "catch(java.time.DateTimeException invalidDate)" in maint
merge=(root/"JsonDataMerger.java").read_text()
main=(root/"MainActivity.java").read_text()
settings=(root/"HubScreens.java").read_text()
assert 'String collection:Store.COLLECTIONS' in merge
assert 'ids.contains(id)' in merge and 'listingUrls.contains(normalized)' in merge
assert 'return new Result(Store.parse(target.toString()),added,skipped,blocked)' in merge
assert 'ambiguousBikeIds.contains(entry.optString("vehicleId"))' in merge
assert 'JsonDataMerger.merge(Store.parse(previewSnapshot),source)' in main and 'JsonDataMerger.merge(Store.parse(currentSnapshot),source)' in main and 'write(ready.data)' in main
assert 'RestoreRideGate.begin(RideService.running,RideService.autoArmed)' in main
assert 'if(request==61){prepareJsonMerge(uri);return;}' in main
ride=(root/"RideUi.java").read_text()
home=(root/"GarageHomeUi.java").read_text()
quick=(root/"QuickEntryUi.java").read_text()
assert 'void finishWithSummary()' in ride and 'RideEndSummary.text(' in ride
assert 'new File(track.getPath()+".done").isFile()' in ride
assert 'new RideUi(a).finishWithSummary()' in home
assert 'extra_cards' in home and 'if(extra)' in home
assert '이전 정비주기 불러오기' in quick and 'QuickRecordDraft.nextOdometer(' in quick
assert '차량 ID 충돌' in merge and 'byId.get(incoming.getString("id"))' in merge
assert 'moto-json-merge-commit' in main
assert 'before-json-merge.json' in main and 'before-json-merge.json' in settings
assert 'a::importJsonMerge' in settings and settings.index('a::importJsonMerge')>settings.index('void advancedBackup()')
print("PASS UX67 widget intent reuse, maintenance fallback and opt-in JSON duplicate-safe merge")
PY
