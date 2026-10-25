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
release=Path("tools/choose_release_version.py").read_text()
assert 'next_code = max(source_code, published + 1)' in release and 'Published package/signing identity mismatch' in release, 'strictly monotonic signed release code and manifest identity'
assert "if: github.ref == 'refs/heads/main'" in signed
assert 'test "$CERT" = "212c5b39d0082ab4bed78f8da2b719819cd80a20bc116c84f8bdfabca507021b"' in signed, 'verified old-installed signing identity required'
assert 'ref: ${{ github.sha }}' in signed, 'release must build triggering source commit'
print("PASS UX8 GPS off-main ordering, final fsync and year-aware photo selection")
PY
