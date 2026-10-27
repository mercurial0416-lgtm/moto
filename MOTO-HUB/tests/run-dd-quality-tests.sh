#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
out="$(mktemp -d)"
trap 'rm -rf "$out"' EXIT
javac -encoding UTF-8 -d "$out" \
 app/src/main/java/com/motohub/next/{TrackFile,TrackRecovery,RideMotionGate,RideInsights,GpsFixPolicy,ObdTelemetryFreshness,RideHistorySearch,RideHistoryPeriod,RideHistoryBikeFilter,RideHistoryLabel,RideHistorySort,RideBikeBinding,RideHistoryFilter}.java \
 tests/{TrackRecoveryTest,TrackRecoveryRollbackTest,RideInsightsMovingAverageTest,RideDayLongSimulationTest,RideHistoryTenThousandTest}.java
java -cp "$out" com.motohub.next.TrackRecoveryTest
java -cp "$out" com.motohub.next.TrackRecoveryRollbackTest
java -cp "$out" com.motohub.next.RideInsightsMovingAverageTest
java -Xmx512m -cp "$out" com.motohub.next.RideDayLongSimulationTest
java -Xmx512m -cp "$out" com.motohub.next.RideHistoryTenThousandTest
python3 - <<'PY'
from pathlib import Path
src=Path('app/src/main/java/com/motohub/next/RideService.java').read_text()
assert 'Sharing.start(' not in src
assert 'ObdReadOnlyProtocol.query(' not in src or 'Obd' in src
ui=Path('app/src/main/java/com/motohub/next/MainActivity.java').read_text()
assert 'info.movingAverageKmh' in ui
assert 'GPS 관측 정차 추정' in ui and '미관측' in ui
assert "versionCode 518" in Path('app/build.gradle').read_text()
print('PASS DD safety/UI/version source wiring')
PY
