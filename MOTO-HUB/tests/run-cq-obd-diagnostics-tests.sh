#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
folder="$(mktemp -d)"
trap 'rm -rf "$folder"' EXIT
javac -encoding UTF-8 -d "$folder" \
  app/src/main/java/com/motohub/next/ObdAuxSensorPolicy.java \
  app/src/main/java/com/motohub/next/ObdRideAutoGate.java \
  app/src/main/java/com/motohub/next/ObdRestStatus.java \
  tests/ObdDiagnosticSummaryTest.java
java -cp "$folder" com.motohub.next.ObdDiagnosticSummaryTest
python3 - <<'PYTEST'
from pathlib import Path
src=Path('app/src/main/java/com/motohub/next')
monitor=(src/'ObdAutoSpeedMonitor.java').read_text()
ride=(src/'RideService.java').read_text()
dev=(src/'DeveloperObdScreen.java').read_text()
dash=(src/'ObdDashboardActivity.java').read_text()
checks={
 'diagnostic callback remains on worker side':'listener.auxiliaryDiagnostics(auxiliary.summary' in monitor,
 'speed samples still start rides':'listener.speed(speed)' in monitor,
 'read-only pid query retained':'ObdReadOnlyProtocol.query(pid)' in monitor,
 'obd sensor state only volatile in service':'static volatile String obdAuxDiagnostics=' in ride,
 'sensor diagnosis published in developer-only UI':'보조 센서 진단:' in dev,
 'rest countdown displayed in developer screen':'ObdRestStatus.label' in dev,
 'diagnosis displayed in developer dash':'ObdRestStatus.label' in dash and '보조 센서 진단:' in dash,
 'location sharing remains disabled for auto ride':'Sharing.stop(); // Automatic local rides' in ride,
 'notification shows rest countdown':'ObdRestStatus.label(true,true,obdStationaryRemainingMs)' in ride,
}
for name,value in checks.items():print(('PASS' if value else 'FAIL')+': '+name)
assert all(checks.values())
PYTEST
