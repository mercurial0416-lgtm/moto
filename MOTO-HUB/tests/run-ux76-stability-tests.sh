#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
python3 - <<'PY'
from pathlib import Path
base=Path("app/src/main/java/com/motohub/next")
read=lambda file:(base/file).read_text()
hub=read("HubScreens.java")
home=read("GarageHomeUi.java")
ride=read("RideStorageManager.java")
main=read("MainActivity.java")
status=read("ObdConnectionState.java")
assert 'visibleLedgerCollections.getOrDefault(x,"services")' in hub
assert 'final String collection=' in hub and 'a.edit(collection,x)' in hub
assert 'String[] kinds={"전체","주유","정비","비용","GPS"}' in hub
assert 'GarageSearchDate.iso(from.getText().toString())' in hub and 'GarageSearchDate.iso(to.getText().toString())' in hub
assert 'RideBikeBinding.readStrict(track)' in hub
assert 'String[] indexed=new String[tracks.size()]' in ride
assert 'for(int i=0;i<indexed.length;i++)' in ride
assert 'settingsNeedReview' in main and '설정 일부 복구 실패' in main
assert '활동·유지비 더 보기' in home
assert 'ObdTelemetryFreshness.speedFresh(' in status
assert 'motoReleaseCode=551' in Path("../.github/workflows/moto-ux-validation.yml").read_text()
print("PASS UX76 ledger filters, GPS index, backup warnings, home and OBD diagnostics")
PY
