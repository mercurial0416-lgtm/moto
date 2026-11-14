#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
temp="$(mktemp -d)"
trap 'rm -rf "$temp"' EXIT
javac -encoding UTF-8 -d "$temp" \
 app/src/main/java/com/motohub/next/GpsParkedEvidence.java \
 tests/GpsParkedEvidenceTest.java
java -cp "$temp" com.motohub.next.GpsParkedEvidenceTest
python3 - <<'PY'
from pathlib import Path
s=Path("app/src/main/java/com/motohub/next/RideService.java").read_text()
assert "gpsParkedEvidence.sustained(now,autoGate.stopAfterMs())" in s
assert "gpsParkedEvidence.observe(lastFixElapsed,dt,metres" in s
assert "gpsParkedEvidence.reset()" in s
print("PASS GPS parked proof integrated with OBD-loss finish")
PY
