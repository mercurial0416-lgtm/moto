#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
javac -encoding UTF-8 -d "$tmp" app/src/main/java/com/motohub/next/FuelAnomalyPolicy.java tests/UX83FuelSafetyTest.java
java -cp "$tmp" com.motohub.next.UX83FuelSafetyTest
python3 - <<'PY'
from pathlib import Path
p=Path('app/src/main/java/com/motohub/next')
def s(file):return (p/file).read_text()
main=s('MainActivity.java');ride=s('RideUi.java');notes=s('RideNotes.java')
fuel=s('QuickEntryUi.java');batch=s('ServiceBatch.java');audit=s('GarageIntegrity.java')
assert 'String savedMemo=RideNotes.memo(a.db,file)' in ride
assert 'date+" "+memo' in ride
assert 'RideNotes.clearDeleted(next,track.getName())' in main
assert 'new String[]{".done",".imported",".recovered",".category",".bike",".unmerged",".favorite"}' in main
assert '이전 주유 날짜 확인' in fuel and '오늘 날짜로 변경' in fuel
assert 'FuelAnomalyPolicy.unusualLiters' in fuel and 'FuelAnomalyPolicy.lowOdometer' in fuel
assert 'FuelAnomalyPolicy.unusualAmount' in fuel
assert 'MaintenanceDueOverrides.apply' in main and 'dueKms' in main and 'dueDates' in main
assert 'GarageIntegrity.findings(checked)' in main and 'edit(found.collection,record)' in main
assert 'approvedRideBikeId' in main and 'final String fixedBikeId=a.approvedRideBikeId;' in ride
assert '기록할 바이크:' in ride and '이 바이크로 기록 시작' in ride
assert 'apksigner' in Path('../.github/workflows/moto-fixed-signed.yml').read_text()
ci=Path('../.github/workflows/moto-ux-validation.yml').read_text()
assert '-PmotoReleaseCode=552' in ci and "versionCode='552'" in ci
assert 'bash tests/run-ux83-stabilization-tests.sh' in ci
print("PASS UX83: memo search, ride delete cleanup, fuel/date warnings, per-item service overrides, audit drilldown and fixed GPS bike")
PY
