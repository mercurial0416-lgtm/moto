from pathlib import Path
from version_consistency import version_declaration_ok, debug_alignment_ok
root=Path(__file__).resolve().parents[1]
src=root/'app/src/main/java/com/motohub/next'
main=(src/'MainActivity.java').read_text()
ride=(src/'RideService.java').read_text()
checks={
 'shared_restore_guard_helper': 'boolean restoreIsBlocked()' in main and 'RestoreRideGate.blocked(RideService.running,RideService.autoArmed)' in main,
 'zip_picker_blocked': 'void importBundle(){\n  if(restoreIsBlocked())' in main,
 'zip_staging_blocked': 'void prepareBundleRestore(Uri uri){\n  if(restoreIsBlocked())' in main,
 'zip_confirmation_rechecks': 'if(restoreIsBlocked()){ready.close();explainRestoreBlocked();return;}' in main,
 'zip_apply_transaction': 'RestoreRideGate.begin(RideService.running,RideService.autoArmed)' in main,
 'zip_transaction_released': 'if(entered)RestoreRideGate.end();ready.close();' in main,
 'zip_json_final_guard': 'if(RideService.running||RideService.autoArmed)throw new IOException(RestoreRideGate.MESSAGE);' in main,
 'json_import_document_guard': 'if(restoreIsBlocked())throw new IOException(RestoreRideGate.MESSAGE)' in main,
 'json_confirmation_guard': 'void confirmImport(JSONObject next){\n  if(restoreIsBlocked())' in main,
 'json_commit_transaction': main.count('RestoreRideGate.begin(RideService.running,RideService.autoArmed)')==2,
 'json_finally_unlock': 'finally{RestoreRideGate.end();}' in main,
 'ride_start_blocked_while_restoring': 'if(!RestoreRideGate.beginRideStart())' in ride and 'finally{RestoreRideGate.endRideStart();}' in ride,
 'existing_gps_auto_recording_preserved': 'openRideTrack();' in ride and 'handleAutoSpeed' in ride,
 'existing_group_share_not_auto_enabled': 'Sharing.stop(); // Automatic local rides' in ride,
 'app_version_updated': version_declaration_ok() and debug_alignment_ok(),
}
for name,ok in checks.items():print(('PASS' if ok else 'FAIL')+' '+name)
if not all(checks.values()):raise SystemExit(1)
print(f'{len(checks)} restore integration source assertions passed (not Android runtime)')
