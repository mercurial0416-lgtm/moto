#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
out="$(mktemp -d)"
trap 'rm -rf "$out"' EXIT
javac -encoding UTF-8 -d "$out" \
 app/src/main/java/com/motohub/next/{PhotoTitleMatcher,PhotoCacheYearPolicy}.java \
 tests/PhotoCacheYearPolicyTest.java
java -cp "$out" com.motohub.next.PhotoCacheYearPolicyTest
javac -encoding UTF-8 -d "$out" \
 app/src/main/java/com/motohub/next/{TrackFile,TrackCompletion}.java \
 tests/Ux9TrackCompletionTest.java
java -cp "$out" com.motohub.next.Ux9TrackCompletionTest
python3 - <<'PY'
from pathlib import Path
base=Path('app/src/main/java/com/motohub/next')
service=(base/'RideService.java').read_text()
ride=(base/'RideUi.java').read_text()
gallery=(base/'MotorcyclePhotoGallery.java').read_text()
photo=(base/'MotorcyclePhoto.java').read_text()
assert 'if(STOP.equals(action)){\n   finishUserRideAsync();' in service
assert 'if(autoFinishPending){' in service and 'finishPending=true;Sharing.stop();return;' in service
assert 'if(finishPending){' in service and 'stopWith(verified?' in service
assert 'finishPending=true;\n  Sharing.stop();' in service
assert 'new Handler(worker.getLooper()).post(()->{' in service
assert service.count('boolean verified=closeTrack(true);')==2, 'Both user and OBD completion verify on the worker'
assert 'private synchronized boolean closeTrack(boolean completed)' in service
assert 'if(!finishPending)return;finishPending=false;' in service and 'stopWith(verified?' in service
assert 'autoFinishPending=false;running=false;' in service
assert 'if(!autoArmed||autoFinishPending||finishPending)return;' in service
assert 'gpsIoThread.quitSafely()' in service and 'TrackCompletion.mark(track,count)' in service
assert 'final boolean saving=RideService.finishPending||RideService.autoFinishPending;' in ride
assert 'start.setEnabled(!saving);' in ride
assert 'finishRide.setEnabled(!saving);' in ride
assert 'if(!RideService.running){a.render();return;}' in ride
assert 'Bitmap bitmap=null;' in gallery and 'finally{if(bitmap!=null&&!bitmap.isRecycled())bitmap.recycle();}' in gallery
assert 'if(ready!=null&&!ready.isRecycled())ready.recycle();' in gallery
assert 'PhotoCacheYearPolicy.canShow(' in photo
assert 'if(file.isFile()&&cacheAllowed)' in photo
assert 'meta.optBoolean("userSelected")' in photo
print('PASS UX9 GPS stop ordering, user override, UI state, bitmap cleanup and cached-year policy')
PY
