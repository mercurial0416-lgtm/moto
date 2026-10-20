#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
javac -encoding UTF-8 -d "$TMP" \
 app/src/main/java/com/motohub/next/{ReceiptSlots,BackupFormat,TrackFile,RestoreCapacity,RestoreArchive}.java \
 tests/Ux8RestoreSafetyTest.java
java -cp "$TMP" com.motohub.next.Ux8RestoreSafetyTest
bash tests/run-photo-title-tests.sh
python3 - <<'PY'
from pathlib import Path
root=Path("app/src/main/java/com/motohub/next")
ride=(root/"RideService.java").read_text()
photo=(root/"MotorcyclePhoto.java").read_text()
assert 'new HandlerThread("moto-gps-disk"' in ride, "GPS needs an off-UI callback looper"
assert 'manager.requestLocationUpdates(LocationManager.GPS_PROVIDER,5000,0,this,gpsIoThread.getLooper())' in ride
assert '@Override public synchronized void onLocationChanged(Location location)' in ride
assert 'private synchronized boolean closeTrack(boolean completed)' in ride
assert 'private synchronized void openRideTrack()throws Exception' in ride
assert 'synchronized void cleanup(boolean completed)' in ride
assert 'output.getFD().sync()' in ride, "committed samples remain durable"
assert 'gpsIoThread.quitSafely()' in ride
assert 'handler.post(()->{if(autoArmed&&running){autoGate.gpsMovement(' in ride, "OBD gate stays on main"
assert 'PhotoTitleMatcher.matchesFallback(title,brand,model,year)' in photo
provider=ride.split('@Override public void onProviderDisabled')[1].split('@Override public void onProviderEnabled')[0]
assert provider.index('Sharing.stop();')<provider.index('handler.post('), 'privacy: stop location sharing immediately'
signed=Path("../.github/workflows/moto-fixed-signed.yml").read_text()
assert 'test "$CODE" -gt "$CURRENT"' in signed
assert "if: github.ref == 'refs/heads/main'" in signed
assert 'WRONG SIGNING CERTIFICATE. Refusing APK.' in signed
print("PASS UX8 GPS off-main ordering, final fsync and year-aware photo selection")
PY
