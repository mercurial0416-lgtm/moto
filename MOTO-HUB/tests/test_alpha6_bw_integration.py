#!/usr/bin/env python3
"""Static wiring checks for UI/accessibility/privacy and 12 requested BW areas.
Not a replacement for Android compilation or device tests.
"""
from pathlib import Path
from version_consistency import version_declaration_ok, debug_alignment_ok, signed_alignment_ok
p=Path('app/src/main/java/com/motohub/next')
def src(name):return (p/name).read_text()
checks={
 '01 home bike swipe/arrow and cost trend': all(x in src('GarageHomeUi.java') for x in ['void switchBike(int direction)','이전 바이크','다음 바이크','CostTrend.caption']),
 '02 fuel form instant unit-price preview': all(x in src('MainActivity.java') for x in ['FuelEntryMath.unitPrice(', 'f.get("amount").addTextChangedListener(watcher)']),
 '03 photo explicit local/gallery/remove choice': all(x in src('GarageHomeUi.java') for x in ['setTitle("바이크 사진 관리")','내 사진 제거 (기본 사진으로)','MotorcyclePhotoGallery.open','setPositiveButton("사진 제거"']),
 '04 interrupted GPS track discoverability': '중단된 주행 ' in src('RideUi.java') and 'TrackRecovery.recover(' in src('RideUi.java'),
 '05 real-time ride average speed': 'RideMetrics.averageText(' in src('RideUi.java'),
 '06 maintenance notification consent/permission': all(x in src('DailyMaintenanceJob.java') for x in ['maintenance_opt_in','POST_NOTIFICATIONS','areNotificationsEnabled()']) and '동의하고 켜기' in src('HubScreens.java'),
 '07 widget quick forms only, no GPS auto-start': all(x in src('MotoWidgetProvider.java') for x in ['EXTRA_FORM','widget_fuel','widget_service']) and 'edit(form,null);' in src('MainActivity.java'),
 '08 offline local features disclosed': 'OfflineStatus.hasInternet(this)' in src('MainActivity.java') and 'ACCESS_NETWORK_STATE' in Path('app/src/main/AndroidManifest.xml').read_text(),
 '09 daily local snapshot stored atomically': all(x in src('DailyMaintenanceJob.java') for x in ['LocalSafetyBackup.snapshot(this,db)','new AtomicFile(file).readFully()','NETWORK_TYPE_NONE']),
 '10 one-tap stop explicit group sharing': '공유 꺼짐 · 명시적 동의 필요' in src('HubScreens.java') and 'Sharing.stop();a.render();' in src('HubScreens.java'),
 '11 CI version and release artifact in sync': debug_alignment_ok() and 'run-bw-improvements-tests.sh' in Path('.github/workflows/android-debug.yml').read_text(),
 '12 version display is not stale/hardcoded': 'BuildConfig.VERSION_NAME' in src('HubScreens.java') and version_declaration_ok(),
 'local database schema preservation': 'motohub-v3.json' in src('MainActivity.java') and 'motohub-v3.json' in src('DailyMaintenanceJob.java'),
 'location sharing no auto enable from widgets': 'Sharing.start(' not in src('MotoWidgetProvider.java') and 'RideService' not in src('MotoWidgetProvider.java') and 'Sharing.start(' not in src('DailyMaintenanceJob.java'),
}
for name,passed in checks.items():print(('PASS' if passed else 'FAIL')+': '+name)
if not all(checks.values()):raise SystemExit(1)
print(f'Alpha6-BW source integration checks: {sum(checks.values())}/{len(checks)} passed')
