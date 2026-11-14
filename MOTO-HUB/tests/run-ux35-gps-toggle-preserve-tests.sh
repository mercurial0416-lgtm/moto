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
pause=s.split('private void pauseObdForGpsOff(){',1)[1].split('private void resumeObdAfterGpsOn(){',1)[0]
resume=s.split('private void resumeObdAfterGpsOn(){',1)[1].split('private void armAutomaticRide(',1)[0]
assert 'if(autoArmed&&!gpsEnabled)pauseObdForGpsOff()' in watch
assert 'if(autoArmed&&gpsEnabled&&autoGpsPaused)resumeObdAfterGpsOn()' in watch
assert 'if(!autoArmed||autoGpsPaused)return;' in pause, 'GPS OFF can pause only once'
assert 'handler.removeCallbacks(reconnectObd)' in pause, 'GPS OFF cancels stale timers'
assert 'ObdAutoSpeedMonitor monitor=obdMonitor;obdMonitor=null;' in pause, 'GPS OFF invalidates stale OBD callbacks'
assert 'if(monitor!=null)monitor.stop();' in pause, 'GPS OFF stops active polling'
assert 'stopWith(' not in pause, 'GPS OFF must never close an active GPS track'
assert 'handler.post(reconnectObd)' in resume, 'GPS ON immediately resumes OBD connection attempts'
assert 'stopWith(' not in resume, 'GPS ON must not close the track'
assert 'GpsProviderLossPolicy.onLoss(' not in watch, 'watchdog must not repeatedly call old disconnect/disarm path'
disconnect=s.split('private void onObdDisconnected(String reason){',1)[1].split('Notification notification(',1)[0]
assert 'if(running){' in disconnect and 'gpsOnlyAfterObd=true;' in disconnect
assert 'notifySafely(status)' in disconnect
assert 'OBD 연결 끊김 · GPS 계속 기록' in disconnect
print("PASS OBD disarm preserves active GPS session without silently completing GPS track")
PY
