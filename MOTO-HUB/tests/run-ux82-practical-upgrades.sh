#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
python3 - <<'PY'
from pathlib import Path
base=Path("app/src/main/java/com/motohub/next")
def src(file):return (base/file).read_text()
fuel=src("QuickEntryUi.java")
main=src("MainActivity.java")
service=src("ServiceStatusUi.java")
batch=src("ServiceBatch.java")
widget=src("MotoWidgetProvider.java")
quickWidget=src("MotoQuickWidgetProvider.java")
ride=src("RideUi.java")
checks=[
 ('fuel draft scoped per bike', all(x in src("FuelDraft.java") for x in ['fuel_"+id','MAX_AGE','bikeId'])),
 ('fuel draft restore and discard', 'FuelDraft.read(a,bikeId)' in fuel and 'FuelDraft.clear(a,bikeId)' in fuel),
 ('fuel draft persisted on changes', 'amount.addTextChangedListener(draftWatcher)' in fuel and 'date.addTextChangedListener(draftWatcher)' in fuel),
 ('full tank odometer confirmed', 'full.isChecked()&&!odoConfirmed.isChecked()' in fuel),
 ('last bike fuel price and liters', 'recentFuelLitersSafe(recentFuel)' in fuel and 'typicalPrice' in fuel),
 ('last bike service type', '이 바이크 최근 정비 항목' in fuel),
 ('batch service action', 'a::batchServiceEntry' in service and '여러 정비 항목' in service),
 ('batch preserves service next date', 'intervalMonths' in batch and 'nextDate' in batch),
 ('ride metadata in regular backup JSON', 'rideNotes' in src("RideNotes.java") and 'RideNotes.update' in main),
 ('ride metadata shown on list from stable snapshot', 'RideNotes.title(stableDb,file)' in ride and 'rideNotesSnapshot=liveNotes==null?' in ride and 'RideNotes.title(a.db,file)' not in ride),
 ('widget selected bike', 'WidgetBikeSnapshot.label(context)' in widget and 'WidgetBikeSnapshot.label(context)' in quickWidget),
 ('widget cache updates after bike switch', 'WidgetBikeSnapshot.update(this)' in main),
 ('garage audit non destructive', 'GarageIntegrity.report' in main and '데이터 연결 검사' in src("HubScreens.java")),
 ('Android current release', '-PmotoReleaseCode="$CODE"' in Path("../.github/workflows/moto-ux-validation.yml").read_text())
]
for title,ok in checks:
 if not ok: raise AssertionError(title)
 print('PASS',title)
PY
