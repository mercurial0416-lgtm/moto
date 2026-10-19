#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
out="$(mktemp -d)";trap 'rm -rf "$out"' EXIT
javac -encoding UTF-8 -d "$out" \
 app/src/main/java/com/motohub/next/{ServiceDueSummary,RideTrend}.java tests/BzImprovementsTest.java
java -cp "$out" com.motohub.next.BzImprovementsTest
python3 - <<'PY'
from pathlib import Path
import sys
sys.path.insert(0, 'tests')
from version_consistency import version_declaration_ok, debug_alignment_ok
src=Path('app/src/main/java/com/motohub/next')
def java(n):return (src/(n+'.java')).read_text()
checks={
'home due dates and mileage': 'ServiceDueSummary.nearest(' in java('GarageHomeUi'),
'home interrupted ride manual recovery': 'interruptedRideShortcut();' in java('GarageHomeUi') and 'RideHistoryFilter.INTERRUPTED' in java('GarageHomeUi'),
'ride permission safe stop': 'START_NOT_STICKY' in java('RideService') and 'Sharing.stop();status=' in java('RideService'),
'GPS gap resets previous fix': 'lastGood=null;lastFixElapsed=0;Sharing.stop()' in java('RideService'),
'GPS peak metric updated': 'livePeakKmh=Math.max(' in java('RideService') and 'RideService.livePeakKmh' in java('RideUi'),
'maintenance private on lockscreen': 'setVisibility(Notification.VISIBILITY_PRIVATE)' in java('DailyMaintenanceJob'),
'gallery IO bounded': 'newFixedThreadPool(2' in java('MotorcyclePhotoGallery') and java('MotorcyclePhotoGallery').count('PHOTO_IO.execute(')==2,
'restore comparison before consent': 'RestorePreview.text(verified,db,ready.rides,ready.photos)' in java('MainActivity') and '.setPositiveButton("전체 복원"' in java('MainActivity'),
'ride comparison visible': 'RideTrend.compare(report.current.km,report.previous.km)' in java('RideUi'),
'group consent untouched': '.setPositiveButton("동의하고 공유"' in java('GroupUi') and 'Sharing.start(id)' in java('GroupUi'),
'update signing warning': '동일한 서명키로 빌드해야 합니다' in java('HubScreens'),
'version and workflow': version_declaration_ok() and debug_alignment_ok(),
'fuel 2-tap for nonfuel latest': '최근 주유값으로 채우기' in java('MainActivity') and '최근 주유 기록의 금액과 리터만 불러오기' not in java('MainActivity') and 'FuelRecordValidation.requireFuel(x.optDouble(' in java('MainActivity'),
'cost current vs previous month': 'CostTrend.caption(currentMonth,previousMonth)' in java('HubScreens'),
'ride end schedules deferred reminder only after verified save': 'if(completed&&verified)DailyMaintenanceJob.checkSoon(this)' in java('RideService'),
'signed release never builds unsigned': 'MOTO_SIGNING_STORE_B64' in Path('.github/workflows/android-signed-release.yml').read_text() and 'apksigner' in Path('.github/workflows/android-signed-release.yml').read_text(),
}
for name,ok in checks.items():print(('PASS' if ok else 'FAIL')+': '+name)
assert all(checks.values()),'BZ source integration check failed'
print('PASS: ',len(checks),' BZ source wiring assertions')
PY
