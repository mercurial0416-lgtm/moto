#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
dir="$(mktemp -d)"
trap 'rm -rf "$dir"' EXIT
javac -encoding UTF-8 -d "$dir" \
 app/src/main/java/com/motohub/next/RidePostCommit.java tests/RidePostCommitTest.java
java -cp "$dir" com.motohub.next.RidePostCommitTest
python3 - <<'PY'
from pathlib import Path
s=Path('app/src/main/java/com/motohub/next/RideService.java').read_text()
start=s.index('@Override public synchronized void onLocationChanged(Location location)')
end=s.index('@Override public void onProviderDisabled(',start)
call=s[start:end]
assert 'output.getFD().sync()' in call
assert 'RidePostCommit.afterWrite(' in call
assert call.index('output.getFD().sync()') < call.index('RidePostCommit.afterWrite(')
assert call.index('catch(Exception e){') < call.index('RidePostCommit.afterWrite(')
assert 'stopWith("저장 오류로 기록 중지: "+e.getMessage(),false);' in call
assert '()->{Sharing.stop();status="그룹 공유 실패 · 로컬 GPS 기록 유지";}' in call
assert 'notifySafely(String.format(' in call, 'notification failures stay outside durable GPS writes'
print("PASS group sharing and notification are outside GPS write failure handling")
PY
