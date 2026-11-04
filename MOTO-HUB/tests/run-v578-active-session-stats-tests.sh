#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
dir="$(mktemp -d)"
trap 'rm -rf "$dir"' EXIT
javac -encoding UTF-8 -d "$dir" app/src/main/java/com/motohub/next/RideActiveSessionPolicy.java tests/Ux125ActiveSessionPolicyTest.java
java -cp "$dir" com.motohub.next.Ux125ActiveSessionPolicyTest
python3 - <<'PY'
from pathlib import Path
p=Path("app/src/main/java/com/motohub/next")
for name,variable in (("RideStatsUi.java","ride"),("RideUi.java","track"),("MotoTimelineUi.java","track")):
 text=(p/name).read_text()
 assert f'RideActiveSessionPolicy.exclude({variable}.getName(),RideService.running,RideService.active)' in text,name
 assert f'RideService.running&&{variable}.getName().equals(RideService.active)' not in text,name
stats=(p/"RideStatsUi.java").read_text()
assert "data.scanned++" in stats
assert "RideCostPerKm.caption(selected,sixMonthExpense,data.totalKm,data.excluded)" in stats
types=(p/"RideTypeComparison.java").read_text()
assert "commuteBuckets" in types and "windingBuckets" in types
print("PASS UX125 chart, period, timeline wiring and cost/km completeness")
PY
