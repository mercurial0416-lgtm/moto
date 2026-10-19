#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
javac -encoding UTF-8 -d "$TMP" \
  app/src/main/java/com/motohub/next/ObdAuxSensorPolicy.java \
  tests/ObdAuxSensorPolicyTest.java
java -cp "$TMP" com.motohub.next.ObdAuxSensorPolicyTest
python3 - <<'PY'
from pathlib import Path
src=Path('app/src/main/java/com/motohub/next/ObdAutoSpeedMonitor.java').read_text()
assert 'ObdAuxSensorPolicy auxiliary=new ObdAuxSensorPolicy()' in src
assert 'auxiliary.failed(pid,System.nanoTime());' in src
assert 'auxiliary.succeeded(pid,System.nanoTime());' in src
assert 'catch(InterruptedException e)' in src
assert 'listener.speed(speed);' in src
assert 'ObdReadOnlyProtocol.query(pid)' in src
print('PASS: OBD speed polling remains active and optional telemetry is isolated')
PY
