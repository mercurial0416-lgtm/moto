"""CO source-level safety wiring assertions; not a replacement for Android compilation."""
from pathlib import Path
from version_consistency import version_declaration_ok,debug_alignment_ok,signed_alignment_ok
root=Path(__file__).resolve().parents[1]
source=lambda n:(root/'app/src/main/java/com/motohub/next'/n).read_text(encoding='utf-8')
read=source('ObdReadOnlyProtocol.java')
cap=source('ObdPidCapabilities.java')
boot=source('BootRidePolicy.java')
ui=source('DeveloperObdScreen.java')
ride=source('RideService.java')
checks={
 'Current version and workflows':version_declaration_ok() and debug_alignment_ok() and signed_alignment_ok(),
 'bounded consensus OBD bytes':'agreed!=null' in read and 'return null;' in read,
 'ambiguous speed disabled':'responseBytes("010D",raw)' in read,
 'ambiguous ODO bitmap unknown':'agreed.longValue()!=bitmap' in cap,
 'only committed GPS rows trigger boot notice':'hasCommittedPoint(file)' in boot and "if(c=='\\n')" in boot,
 'boot starts no GPS or location sharing':'startForegroundService' not in source('BootRideReminder.java') and 'Sharing.' not in source('BootRideReminder.java'),
 'ODO invalid entry remains editable':'field.setError(ex.getMessage())' in ui and 'entry.setOnShowListener' in ui,
 'ODO cannot silently overwrite':'직접 확인 · 반영' in ui and 'OdometerSyncPolicy.checkedManual' in ui,
 'incomplete ELM reply fails':'throw new java.io.IOException("OBD2 응답이 완료되지 않았습니다.")' in ui,
 'stale developer telemetry hidden':'ObdTelemetryFreshness.fresh(RideService.obdRpmAt' in ui,
 'no ECU writes / read-only whitelist':'ObdReadOnlyProtocol.query("010D")' in source('ObdAutoSpeedMonitor.java') and 'ATSH' not in source('ObdAutoSpeedMonitor.java'),
 'no implicit group location sharing':'Sharing.stop(); // Automatic local rides' in ride,
 'stationary notification throttled':'autoNotificationRate.shouldNotify' in ride,
 'GPS coordinates never throttled':'output.write(TrackFile.point' in ride,
 'GPS jitter excluded consistently':'jitterMetres' in source('RideMotionGate.java') and 'RideMotionGate.countDistance' in source('RideInsights.java'),
 'CO pure JVM test in both CI workflows':'run-co-riding-safety-tests.sh' in (root/'.github/workflows/android-debug.yml').read_text() and 'run-co-riding-safety-tests.sh' in (root.parent/'.github/workflows/moto-hub-debug.yml').read_text(),
}
for name,ok in checks.items():print(('PASS' if ok else 'FAIL')+': '+name)
assert all(checks.values()),'CO source integration regression'
