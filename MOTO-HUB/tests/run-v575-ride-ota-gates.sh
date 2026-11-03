#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
python3 - <<'PY'
from pathlib import Path
p=Path("app/src/main/java/com/motohub/next")
read=lambda n:(p/n).read_text(encoding="utf-8")
service=read("RideService.java")
group=read("RideSegmentGroup.java")
timeline=read("MotoTimelineUi.java")
timeline_model=read("MotoTimeline.java")
report=read("RideUi.java")
stats=read("RideStatsUi.java")
updater=read("AppUpdater.java")
hub=read("HubScreens.java")
main=read("MainActivity.java")
diag=read("ObdLocalDiagnosticLog.java")
checks={
 "GPS safe rollover preserves original UUID": 'rotateRideSection()' in service and 'activeRideSession' in service and 'TrackCompletion.mark(previous,segmentPoints)' in service,
 "GPS rollover retains last boundary fix": "lastGood.getLatitude()" in service and "nextOutput.getFD().sync()" in service,
 "GPS counts match per-section markers": "TrackCompletion.mark(track,segmentPoints)" in service and "count++;segmentPoints++;" in service,
 "GPS uses existing backuppable track naming": 'activeRideSession+".track"' in service and "\.track" in group,
 "GPS session aggregation is view-only": 'RideSegmentGroup.collapse' in timeline and "RideSegmentGroup.id(ride)" in stats,
 "timeline avoids repeated total sort during progress": 'MotoTimeline.newest(merged,120)' in timeline and 'inspected%1024==0' in timeline and 'RideSegmentGroup.collapse(merged)' in timeline,
 "timeline group detail selects a safe original track": 'RideSegmentGroup.members' in timeline and 'RideMileageIntegrity.validCompleted(track)' in timeline,
 "60-day report shows progress and cancels work": 'progress.setMessage(' in report and 'REPORT_GENERATION.incrementAndGet()' in report and 'progress.dismiss()' in report,
 "OBD session stats are sanitized and local": 'automaticSession(Context c' in diag and 'ObdFailureGuide.classify(reason)' in diag and '"auto_samples"' in diag and 'automaticSession(this,obdConnectionAttempts' in service,
 "post-update detailed audit reads current GPS and backup": 'BackupGpsProof.fullCurrentStatus' in hub and 'GarageIntegrity.report(data)' in hub and 'TrackFile.tracks(a.getFilesDir())' in hub,
 "update report reachable in updater and after startup": 'new HubScreens(a).updateRecoveryReport()' in updater and 'new HubScreens(this).updateRecoveryReport()' in main,
 "unchanged package and source version updated": Path("release-code.txt").read_text().strip()=="575"
}
for name,ok in checks.items():print(("PASS" if ok else "FAIL")+" : "+name)
assert all(checks.values()),"v575 GPS/OBD/OTA regression"
PY
