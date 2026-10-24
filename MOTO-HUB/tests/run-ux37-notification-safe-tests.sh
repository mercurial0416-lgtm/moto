#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
javac -encoding UTF-8 -d "$tmp" \
 app/src/main/java/com/motohub/next/RideNotificationSafety.java \
 tests/RideNotificationSafetyTest.java
java -cp "$tmp" com.motohub.next.RideNotificationSafetyTest
python3 - <<'PY'
from pathlib import Path
s=Path("app/src/main/java/com/motohub/next/RideService.java").read_text()
assert s.count(".notify(404,") == 1, "unsheltered notification update in RideService"
start=s.index("private void notifyRideSafely(")
end=s.index("Notification notification(String text)", start)
assert "RideNotificationSafety.run(" in s[start:end]
assert "notifications.notify(404,notification(text))" in s[start:end]
assert "if(Sharing.enabled&&!shareAllowed())Sharing.stop();" in s
assert "catch(RuntimeException unavailable){return false;}" in s
assert "s.notifyRideSafely(" in s
assert "notifyRideSafely(status);" in s
assert "notifyRideSafely(text);" in s
print("PASS all ride status updates isolated and sharing fails closed")
PY
