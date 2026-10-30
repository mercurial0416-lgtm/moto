#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
python3 - <<'PY'
from pathlib import Path
import re
home=Path('app/src/main/java/com/motohub/next/GarageHomeUi.java').read_text()
ride=Path('app/src/main/java/com/motohub/next/RideUi.java').read_text()
service=Path('app/src/main/java/com/motohub/next/RideService.java').read_text()
activity=Path('app/src/main/java/com/motohub/next/MainActivity.java').read_text()
checks={
 'home navigates to ride tab before consent': 'a.navigate("라이딩");' in home and 'new RideUi(a).requestStart(true);' in home,
 'ride start requires ride tab': 'if(!a.rideStartConsent.pending()||!"라이딩".equals(a.tab))return;' in ride,
 'start consent still explicit and bike-fixed': 'setPositiveButton("이 바이크로 기록 시작"' in ride and 'if(!approvedBike.equals(liveBike))' in ride and 'putExtra(RideService.EXTRA_BIKE_ID,fixedBikeId)' in ride,
 'home stop sends completed STOP command': 'new RideUi(a).finishWithSummary()' in home and 'a.startService(new Intent(a,RideService.class).setAction(RideService.STOP))' in ride,
 'ride tab stop sends completed STOP command': 'a.startService(new Intent(a,RideService.class).setAction(RideService.STOP))' in ride,
 'no direct stopService for ride anywhere': all(not re.search(r'\.stopService\(\s*new Intent\([^;]*RideService\.class',p.read_text()) for p in Path('app/src/main/java').rglob('*.java')),
 'STOP is completed not interrupted': 'if(STOP.equals(action))' in service and 'finishUserRideAsync();return START_NOT_STICKY;' in service and 'boolean verified=closeTrack(true)' in service and 'if(AUTO_DISARM.equals(action))' in service,
 'verified completed ride creates done marker': 'TrackCompletion.mark(track,count)' in service and 'TrackCompletion.java' in {p.name for p in Path('app/src/main/java/com/motohub/next').iterdir()},
 'unexpected destruction remains interrupted': 'cleanup(false);' in service and 'gpsIoThread.quitSafely()' in service and 'instance=null;super.onDestroy();' in service,
 'permission callback requires ride tab': 'rideStartConsent.mayContinue("라이딩".equals(tab),granted)' in activity,
 'GPS shared location not enabled by home': 'Sharing.start(' not in home and 'Sharing.enabled=true' not in home,
}
for title, passed in checks.items():
 print(('PASS' if passed else 'FAIL')+': '+title)
assert all(checks.values()),'home GPS UI wiring regression'
print('PASS: %d Android-source wiring checks (not a device test)'%len(checks))
PY
