#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
javac -encoding UTF-8 -d "$tmp" app/src/main/java/com/motohub/next/ObdBikeReadinessPolicy.java tests/ObdBikeReadinessPolicyTest.java
java -cp "$tmp" com.motohub.next.ObdBikeReadinessPolicyTest
python3 - <<'PY'
from pathlib import Path
base=Path('app/src/main/java/com/motohub/next')
settings=(base/'DeveloperObdScreen.java').read_text()
ride=(base/'RideUi.java').read_text()
assert 'ObdBikeReadinessPolicy.status(' in settings
assert 'ObdBikeReadinessPolicy.anotherBikeActive(' in settings
assert 'ObdBikeReadinessPolicy.ownsActiveMonitor(' in settings
assert 'RideService.obdLastSampleElapsed' in settings
assert 'anotherBikeActive' in ride
assert '다른 바이크' in ride
assert 'ObdLocalDiagnosticLog.readableTroubleshooting(activity,bikeId)' in settings
print('PASS selected-bike telemetry privacy and safety source checks')
PY
