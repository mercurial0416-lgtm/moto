#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
: "${JSON_JAR:?CI supplies org.json Java jar}"
out="$(mktemp -d)"
trap 'rm -rf "$out"' EXIT
javac -encoding UTF-8 -cp "$JSON_JAR" -d "$out" \
 app/src/main/java/com/motohub/next/{RecentRecordReuse,RecentFuelRecord}.java \
 tests/RecentFuelRecordTest.java
java -cp "$JSON_JAR:$out" com.motohub.next.RecentFuelRecordTest
python3 - <<'PY'
from pathlib import Path
src=Path("app/src/main/java/com/motohub/next/QuickEntryUi.java").read_text()
assert 'RecentFuelRecord.latest(a.items("expenses"),bike.optString("id"))' in src
assert 'costPerLiter.setText(FuelEntryMath.unitPrice(' in src
assert 'full.setOnCheckedChangeListener(' in src and 'if(checked)extra.setVisibility(View.VISIBLE)' in src
print("PASS quick fuel latest history, unit price and full-tank mileage UI wiring")
PY
