#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
python3 - <<'PY'
from pathlib import Path
base=Path("app/src/main/java/com/motohub/next")
main=(base/"MainActivity.java").read_text()
home=(base/"GarageHomeUi.java").read_text()
quick=(base/"QuickEntryUi.java").read_text()
parking=(base/"ParkingSpotUi.java").read_text()
widget=(base/"MotoQuickWidgetProvider.java").read_text()
choices=(base/"QuickWidgetChoices.java").read_text()
hub=(base/"HubScreens.java").read_text()
service=(base/"ServiceStatusUi.java").read_text()
ci=Path("../.github/workflows/moto-ux-validation.yml").read_text()
assert 'promptRideOdometer(JSONObject ride)' in main and 'new ServiceStatusUi(this).updateOdo(linked)' in main
assert '.setPositiveButton("계기판 갱신"' in main
assert 'chooseCompletion(bike)' in home and 'new ServiceStatusUi(a).complete(' in home
assert 'void complete(JSONObject old)' in service and 'ServiceCycle.completed(' in service
assert 'quickRecentEdit(bike)' in home and 'a.edit(kinds.get(row),recent.get(row))' in home
assert 'String[] choices=new String[count]' in home
assert '지난 기록·단가 계산 도구' in quick and 'tools.setVisibility(View.GONE)' in quick
assert '계기판 바로 촬영' in quick and 'a.action(extra,' in quick
assert 'new ParkingSpotUi(a).show(bike)' in home
assert 'getCurrentLocation(' in parking and 'RideService.running' in parking
assert 'put("parkingLat"' in parking and 'Store.find(next,"vehicles",id)' in parking
assert 'parkingLat' in parking and 'ACTION_VIEW' in parking
assert 'QuickWidgetChoices.choice(context,0)' in widget
assert 'EXTRA_FORM,"fuel_quick"' in widget
assert 'new QuickEntryUi(this).fuel()' in main
assert '작은 위젯 버튼 선택' in hub and 'QuickWidgetChoices.configure(a)' in hub
assert 'static String normalize(' in choices and 'static void refresh(' in choices
assert 'showConsumables(bike)' in home and 'LatestServiceByType.latest(' in home
assert '-PmotoReleaseCode="$CODE"' in ci
assert "versionCode='$CODE'" in ci
print("PASS UX79 seven convenience features and fail-safe source guards")
PY
