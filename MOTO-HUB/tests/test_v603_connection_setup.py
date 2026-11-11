from pathlib import Path
root=Path(__file__).resolve().parents[1]/"app/src/main/java/com/motohub/next"
def read(name):return (root/name).read_text()
ui=read("BikeObdSettingsUi.java")
dev=read("DeveloperObdScreen.java")
ride=read("RideService.java")
policy=read("ConnectionSetupMode.java")
proof=read("FirstRideSetupProof.java")
probe=read("CardoConnectionProbe.java")
issues=read("ConnectionIssueGuide.java")
starter=read("CardoRideStarter.java")
checks={
 "mode not auto-arms": 'class ConnectionSetupMode' in policy and 'startForegroundService' not in policy,
 "three explicit modes": all(x in policy for x in ['CARDO_OBD','OBD_ONLY','CARDO_GPS']),
 "GPS-only selected route bypasses OBD without changing legacy defaults": 'ConnectionSetupMode.explicit(c,bike)' in starter and 'obd=""' in starter,
 "ECU proof is actual response only": 'showVerifiedRegistration(device,selectedBike)' in dev and 'if(!loggedSensor)' in dev and 'ECU 센서 응답' in dev,
 "registration switch has prior consent warning": 'hadGps=changing' in ui and 'hadAuto=changing' in ui and 'hadCardo=changing' in ui,
 "permissions are deferred until selected devices": ui.index('wantsObd&&!obd')<ui.index('else if(!permissions)'),
 "live probe is foreground only": 'private void startLiveStatus' in ui and 'owner.isShowing()' in ui and 'getProfileProxy' in probe and 'closeProfileProxy' in probe,
 "troubleshooter routes to concrete action": 'ConnectionIssueGuide.diagnose' in ui and 'resolveConnectionIssue' in ui and 'case ECU_NO_REPLY' in ui,
 "GPS verification is based on validated disk track": 'FirstRideSetupProof.reportCompletion(this,pendingBikeId,savedTrack,verified,count,liveDistanceKm)' in ride and '.done' in proof and 'gpsKm>=0.10' in proof,
 "GPS ODO remains separate": 'OdometerAutoSyncInbox.enable' not in proof and 'setAutoRideEnabled' not in proof,
}
for k,v in checks.items():print(("PASS" if v else "FAIL")+": "+k)
assert all(checks.values()),"v603 connection setup regression"
