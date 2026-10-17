#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
out="$(mktemp -d)"
trap 'rm -rf "$out"' EXIT
javac -encoding UTF-8 -d "$out" app/src/main/java/com/motohub/next/TrackFile.java app/src/main/java/com/motohub/next/RideHistoryLabel.java tests/RideHistoryLabelTest.java
java -cp "$out" com.motohub.next.RideHistoryLabelTest
python3 - <<'PY'
from pathlib import Path
ride=Path('app/src/main/java/com/motohub/next/RideUi.java').read_text()
assert 'RideHistoryLabel.title(file,java.time.ZoneId.systemDefault())' in ride
assert 'String date="저장된 라이딩"' not in ride
assert 'Sharing.start(' not in ride
print('PASS: ride history UI wiring and unchanged explicit sharing')
PY
