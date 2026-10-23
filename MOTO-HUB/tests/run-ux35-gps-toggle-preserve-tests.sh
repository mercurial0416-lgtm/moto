#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
dir="$(mktemp -d)"
trap 'rm -rf "$dir"' EXIT
javac -encoding UTF-8 -d "$dir" \
 app/src/main/java/com/motohub/next/GpsProviderLossPolicy.java \
 tests/GpsProviderLossPolicyTest.java
java -cp "$dir" com.motohub.next.GpsProviderLossPolicyTest
python3 - <<'PY'
from pathlib import Path
s=Path('app/src/main/java/com/motohub/next/RideService.java').read_text()
watch=s.split('final Runnable permissionCheck=',1)[1].split('  @Override public IBinder',1)[0]
assert 'GpsProviderLossPolicy.onLoss(running,autoArmed)' in watch
assert 'if(monitor!=null)monitor.stop();' in watch
assert 'onObdDisconnected("GPS가 꺼져 자동 감지를 중지했습니다.")' in watch
assert 'GPS 꺼짐 · 기존 라이딩 보존' in watch
assert 'else{stopWith("GPS가 꺼져 자동 출발 감지를 중지했습니다.",false);return;}' in watch
disconnect=s.split('private void onObdDisconnected(String reason){',1)[1].split('Notification notification(',1)[0]
assert 'if(running){' in disconnect and 'gpsOnlyAfterObd=true;' in disconnect
assert 'getSystemService(NotificationManager.class).notify(404,notification(status))' in disconnect
assert 'GPS 단독 기록 유지' in disconnect
print("PASS OBD disarm preserves active GPS session without silently completing GPS track")
PY
