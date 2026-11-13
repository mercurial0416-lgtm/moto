#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
out="$(mktemp -d)"
trap 'rm -rf "$out"' EXIT
javac -encoding UTF-8 -d "$out" \
 app/src/main/java/com/motohub/next/ObdReconnectHealthPolicy.java \
 tests/ObdReconnectHealthPolicyTest.java
java -cp "$out" com.motohub.next.ObdReconnectHealthPolicyTest
python3 - <<'PY'
from pathlib import Path
svc=Path('app/src/main/java/com/motohub/next/RideService.java').read_text()
checks={
 'live GPS speed untouched on ECU reconnect':'obdStationaryRemainingMs=-1;liveSpeedKmh=0' not in svc,
 'new connection resets health':'obdValidSamplesThisConnection=0;' in svc,
 'health based on per-connection samples':'ObdReconnectHealthPolicy.acceptValidSpeed(obdValidSamplesThisConnection)' in svc,
 'unverified ride cannot silently continue':'if(!verified){' in svc and 'ObdHandsFreeSession.disable(this);' in svc,
 'explicit STOP always revokes consent':'else ObdHandsFreeSession.disable(this);' in svc,
}
for name,passed in checks.items():
 assert passed,name
print('PASS hands-free source safety guards:',len(checks))
PY
