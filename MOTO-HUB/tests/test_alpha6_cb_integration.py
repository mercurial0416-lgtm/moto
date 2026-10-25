from pathlib import Path
from version_consistency import version_declaration_ok, debug_alignment_ok, signed_alignment_ok
src=Path('app/src/main/java/com/motohub/next')
def read(name):return (src/(name+'.java')).read_text(encoding='utf-8')
checks={
 'home maintenance warning has tap target':'눌러서 정비 일정 확인' in read('GarageHomeUi') and 'alert.setOnClickListener(v->showNotices())' in read('GarageHomeUi'),
 'always-available validated refill template':'if(key.equals("expenses")){' in read('MainActivity') and 'FuelRecordValidation.requireFuel(x.optDouble(' in read('MainActivity'),
 'long GPS gap is excluded from live ride':'RideMotionGate.countDistance' in read('RideService') and 'MAX_CONTINUOUS_FIX_MS' in read('RideService'),
 'long GPS gap is excluded from history':'RideMotionGate.countDistance' in read('RideInsights') and 'have[0]=false' in read('RideInsights'),
 'speed display shows quality state':'RideService.status' in read('RideUi') and '최고속도' in read('RideUi'),
 'gallery photo final size bounded':'photoOut.getChannel().size()>8_000_000L' in read('MotorcyclePhotoGallery'),
 'maintenance dedup normalizes whitespace':'canonical=n.type.trim()' in read('MaintenanceNoticeOrder'),
 'restore verifies staged and installed assets':'if(!sameContent(staged,installed))' in read('RestoreArchive') and 'writer.write(json)' in read('RestoreArchive'),
 'ride report trend shows same-scale bars':'RideReportBars.render(report.current.km,Math.max(report.current.km,report.previous.km))' in read('RideUi'),
 'group sharing requires local active membership and consent':'if(!api.joined(id))throw' in read('GroupUi') and '동의하고 공유' in read('GroupUi'),
 'no stale location offered when accuracy too poor':'if(!autoArmed&&location.getAccuracy()<=40)Sharing.offer(location)' in read('RideService'),
 'release version matches checkpoint and signing key guard':version_declaration_ok() and 'MOTO_SIGNING_STORE_B64' in Path('.github/workflows/android-signed-release.yml').read_text(),
 'CI invokes new quality tests':'run-cb-quality-tests.sh' in Path('.github/workflows/android-debug.yml').read_text(),
 'explicit user start retained':'START_NOT_STICKY' in read('RideService') and 'rideStartConsent.consume' in read('RideUi'),
}
for title,ok in checks.items():print(('PASS ' if ok else 'FAIL ')+title)
assert all(checks.values()),'CB integration wiring failed'
print(f'PASS: {len(checks)} source integration checks (not Android runtime)')
