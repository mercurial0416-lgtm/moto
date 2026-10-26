#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
javac -encoding UTF-8 -d "$tmp" \
 app/src/main/java/com/motohub/next/TrackFile.java \
 app/src/main/java/com/motohub/next/RideBikeBinding.java \
 app/src/main/java/com/motohub/next/RideTrackMerge.java \
 app/src/main/java/com/motohub/next/TrackCompletion.java \
 app/src/main/java/com/motohub/next/RideMileageIntegrity.java \
 app/src/main/java/com/motohub/next/RideHomeStatus.java \
 app/src/main/java/com/motohub/next/ObdLatencyStats.java \
 app/src/main/java/com/motohub/next/RideMotionGate.java \
 app/src/main/java/com/motohub/next/RideInsights.java \
 app/src/main/java/com/motohub/next/TrackRecovery.java \
 tests/RideMileageIntegrityTest.java \
 tests/RideHomeStatusTest.java \
 tests/ObdLatencyStatsTest.java \
 tests/GpsLongRideResilienceTest.java
for klass in RideMileageIntegrityTest RideHomeStatusTest ObdLatencyStatsTest GpsLongRideResilienceTest;do
 java -cp "$tmp" "com.motohub.next.$klass"
done
python3 - <<'PY'
from pathlib import Path
root=Path("app/src/main/java/com/motohub/next")
read=lambda name:(root/name).read_text(encoding="utf-8")
main=read("MainActivity.java");home=read("GarageHomeUi.java");obd=read("ObdAutoSpeedMonitor.java")
gps=read("RideService.java")
integrity=read("RideMileageIntegrity.java")
checks={
 "content hash pinned at analysis":"RideMileageIntegrity.fingerprint(track)" in main,
 "hash verification off UI thread":'"moto-ride-mileage-sha256"' in main and 'sourceBytes,sourceModified,sourceHash' in main,
 "bounded GPS hashing":"size>TrackFile.MAX_BYTES" in integrity and "MessageDigest.isEqual" in integrity,
 "home scans without 10k sorting":"RideHomeStatus.interrupted(a.getFilesDir())" in home,
 "async home scan remains nonblocking":"HOME_IO.execute(()->{" in home,
 "streaming safe sidecars":"DirectoryStream<Path>" in read("RideHomeStatus.java") and "NOFOLLOW_LINKS" in read("RideHomeStatus.java"),
 "read-only ECU calls":'ObdReadOnlyProtocol.query("010D")' in obd and 'ObdReadOnlyProtocol.query(pid)' in obd,
 "session-only per PID latency":'ObdLatencyStats latency=new ObdLatencyStats()' in obd,
 "auxiliary errors do not interrupt speed":"auxiliary.failed(pid,System.nanoTime());" in obd,
 "no remote sharing on disconnected ECU":'gpsOnlyAfterObd=true;' in gps and "Sharing.stop();" in gps,
 "no unattended GPS restart":"return START_NOT_STICKY;" in gps,
 "GPS worker saves off UI":"new HandlerThread(\"moto-gps-disk\"" in gps,
 "GPS provider loss stops remote sharing":"void onProviderDisabled(" in gps and "Sharing.stop();" in gps,
 "crash never invents completion":"cleanup(false);" in gps
}
for label,ok in checks.items():assert ok,label
print("PASS UX48 source safety checks:",len(checks))
PY
