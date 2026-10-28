#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
OUT="$(mktemp -d)"
trap 'rm -rf "$OUT"' EXIT
javac -encoding UTF-8 -d "$OUT" \
 app/src/main/java/com/motohub/next/{ObdPidCapabilities,ObdSpeedResponsePolicy}.java tests/Ux6ObdPollTest.java
java -cp "$OUT" com.motohub.next.Ux6ObdPollTest
bash tests/run-df-quality-tests.sh
bash tests/run-photo-retry-tests.sh
python3 - <<'PY'
from pathlib import Path
root=Path("app/src/main/java/com/motohub/next")
photo=(root/"MotorcyclePhoto.java").read_text()
assert "Set<String> seenImageUrls=new HashSet<>()" in photo
assert "seenImageUrls.add(imageUrl)" in photo
screen=(root/"HubScreens.java").read_text()
garage=(root/"GarageHomeUi.java").read_text()
assert "LatestServiceByType.latest(allServices,id)" in screen, "maintenance overview must be chronological"
assert "LatestServiceByType.latest(a.items(\"services\"),b.optString(\"id\"))" in garage, "home same policy"
assert "void listings(){" not in screen, "retired marketplace renderer must remain absent"
test=Path("app/src/test/java/com/motohub/next/UpdateTest.java").read_text()
assert "certificateSha256" in test and "wrongSignerBlocked" in test
print("PASS UX6 photo dedup, home/dashboard consistency, update validation")
PY
