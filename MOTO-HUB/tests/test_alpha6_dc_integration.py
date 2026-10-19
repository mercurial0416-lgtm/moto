from pathlib import Path
R=Path(__file__).resolve().parents[1]
src=lambda name:(R/'app/src/main/java/com/motohub/next'/name).read_text()
service=src('RideService.java')
back=src('LocalSafetyBackup.java')
activity=src('MainActivity.java')
ui=src('HubScreens.java')
assert 'requestGpsUpdatesChecked();' in service
assert service.count('manager.requestLocationUpdates(')==1
assert 'checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED' in service
assert 'catch(Exception e){stopWith(' in service
assert 'v.setTypeface(null,android.graphics.Typeface.BOLD);' in src('ObdDashboardActivity.java')
assert 'static File[] recoveryPoints(Context context)' in back
assert 'static void checkpoint(Context context,JSONObject data)' in back
assert 'Store.parse(new String(bytes,StandardCharsets.UTF_8))' in back
assert 'new AtomicFile(file)' in back
assert 'if(points!=null&&points.length>12)' in back
assert activity.index('atomic(disk,next.toString(2)') < activity.index('LocalSafetyBackup.checkpoint(this,next)')
assert 'LocalSafetyBackup.snapshot(this,db);' in activity
assert 'LocalSafetyBackup.recoveryPoints(a)' in ui
assert 'LocalSafetyBackup.read(snapshots[i])' in ui
assert 'Sharing.start(' not in service
assert 'prepareForeground(true)' in service
from version_consistency import version_declaration_ok
assert version_declaration_ok()
print('PASS: 17 DC source/integration assertions')
