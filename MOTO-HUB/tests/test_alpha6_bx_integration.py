#!/usr/bin/env python3
from pathlib import Path
from version_consistency import version_declaration_ok, debug_alignment_ok, signed_alignment_ok
p=Path('app/src/main/java/com/motohub/next')
def src(name):return (p/name).read_text()
checks={
'UI persistent bike selection': 'selected_bike' in src('MainActivity.java') and 'selected_bike' in src('GarageHomeUi.java') and 'selected_bike' in src('HubScreens.java'),
'photo gallery direct access': 'MotorcyclePhotoGallery.open(a,brand,model,bike.optInt("year"));' in src('GarageHomeUi.java') and 'userSelected' in src('MotorcyclePhotoGallery.java'),
'fuel zero litre prevention': 'FuelRecordValidation.requireFuel(' in src('MainActivity.java'),
'GPS paused without automatic sharing': 'GPS 일시 중단 · 켜면 기록 재개' in src('RideService.java') and 'Sharing.stop();status=' in src('RideService.java'),
'backup before DB commit': src('MainActivity.java').index('LocalSafetyBackup.snapshot(this,db);')<src('MainActivity.java').index('atomic(disk,next.toString(2)'),
'maintenance clears stale notifications': 'manager.cancel(405);' in src('DailyMaintenanceJob.java') and 'if(enabled){android.app.NotificationManager' in src('HubScreens.java'),
'local ride report background': 'new Thread(()->{' in src('RideUi.java') and 'RideMonthlyReport report=' in src('RideUi.java'),
'per-bike cost split': 'void costDashboard()' in src('HubScreens.java') and 'CostDashboard.proportion' in src('HubScreens.java'),
'widget privacy choice': 'hide_details' in src('MotoWidgetProvider.java') and 'widget_privacy' in src('HubScreens.java'),
'group logout stops sharing': 'Sharing.stop();run(()->{Sharing.awaitEnd(8000);api.logout();});' in src('SocialUi.java'),
'update build version': version_declaration_ok(),
'nested-repository GitHub workflow': (Path('../.github/workflows/moto-hub.yml').exists() and 'working-directory: MOTO-HUB' in Path('../.github/workflows/moto-hub.yml').read_text()) or Path('.github/workflows/android-debug.yml').exists(),
'CI package alignment': debug_alignment_ok() and 'run-bx-hardening-tests.sh' in Path('.github/workflows/android-debug.yml').read_text(),
'prior 12 area baseline preserved': 'test_alpha6_bw_integration.py' in '\n'.join(x.name for x in Path('tests').iterdir()),
'sharing survives activity reopen only when GPS FGS running': 'if(!RideService.running)Sharing.stop();disk=' in src('MainActivity.java'),
'room leave waits server stop': 'Sharing.awaitEnd(8000)' in src('GroupUi.java'),
'privacy: no auto sharing': 'Sharing.start(' not in src('MotoWidgetProvider.java') and 'Sharing.start(' not in src('DailyMaintenanceJob.java'),
'core local data unchanged': 'motohub-v3.json' in src('MainActivity.java') and 'motohub-v3.json' in src('DailyMaintenanceJob.java'),
}
for label,ok in checks.items():print(('PASS' if ok else 'FAIL'),label)
assert all(checks.values()), 'BX integration source check failed'
print('BX wiring checks:',len(checks),'passed')
