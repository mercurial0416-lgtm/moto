#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
out="$(mktemp -d)"
trap 'rm -rf "$out"' EXIT
javac -encoding UTF-8 -d "$out" app/src/main/java/com/motohub/next/RideStartConsent.java tests/RideStartConsentTest.java
java -cp "$out" com.motohub.next.RideStartConsentTest
python3 - <<'PY'
from pathlib import Path
ui=Path('app/src/main/java/com/motohub/next/RideUi.java').read_text()
main=Path('app/src/main/java/com/motohub/next/MainActivity.java').read_text()
assert 'setPositiveButton("동의하고 시작",(d,w)->{a.rideStartConsent.approve();start();})' in ui
assert 'a.rideStartConsent.consume("라이딩".equals(a.tab),true)' in ui
assert 'if(readiness()!=RideReadiness.State.READY){a.rideStartConsent.cancel()' in ui
assert 'if(isFinishing()||isDestroyed()){rideStartConsent.cancel();return;}' in main
assert 'a.rideStartConsent.cancel();handleReadiness(state)' in ui
assert 'rideStartConsent.mayContinue("라이딩".equals(tab),granted)' in main
assert 'new RideUi(this).start()' in main
assert 'rideStartConsent.pending()' in main
assert 'rideStartConsent.approve()' in main
assert 'getBoolean("rideStartConsent",false)' in main
assert 'onRequestPermissionsResult' in main
assert '권한 설정을 확인한 뒤 GPS 기록 시작을 다시 눌러 주세요' not in main
print('PASS: explicit consent, chained permission callbacks, saved-state wiring')
PY
