from pathlib import Path
root=Path(__file__).resolve().parents[1]
src=root/"app/src/main/java/com/motohub/next"
test=root/"app/src/test/java/com/motohub/next"
def read(n):return (src/n).read_text()
def testread(n):return (test/n).read_text()
starter=read("CardoRideStarter.java")
handshake=read("CardoStartHandshake.java")
ride=read("RideService.java")
profile=read("CardoConnectionProbe.java")
rec=read("CardoPresenceReconciler.java")
verify=read("RideSessionVerification.java")
proof=read("FirstRideSetupProof.java")
ui=read("BikeObdSettingsUi.java")
privacy=read("SafeConnectionDiagnosticReport.java")
hardware=read("RideHardwareVerification.java")
checks={
 "ACK before success, not just FGS request":'CardoStartHandshake.request(c,bike)' in starter and 'CardoStartHandshake.armed(this,pendingBikeId,pendingCardoToken' in ride and 'cancelTap(c)' not in starter.split('static boolean start(Context c)')[1].split('static void startFromTap')[0],
 "requested-but-not-confirmed is a distinct status":'Android에 자동 시작 요청 전달' in handshake and '시작 요청 전송' in starter,
 "missing ACK expires and retry is bounded":'ACK_TIMEOUT_MS=25_000L' in handshake and 'AUTO_RETRY_COOLDOWN_MS=90_000L' in handshake and 'automaticAllowed' in starter,
 "manual retry is visible after failure":'needsManualRecovery' in ui and '자동 시작 다시 요청' in ui,
 "Cardo profile counted once and disconnect verified":'completedProfiles.add' not in profile and 'acceptProfileReply(p.completedProfiles,profile)' in profile and 'CardoConnectionProbe.probe(app,address' in read("CardoBluetoothReceiver.java"),
 "reconciler queries only once":'CardoConnectionProbe.probe' in rec and 'getProfileProxy' not in rec,
 "all split files checked without mutation":'TrackFile.read(file' in verify and 'matches.length!=rotatedParts+1' in verify and 'TrackCompletion.mark' not in verify and 'file.delete()' not in verify,
 "session verification happens on GPS worker":'RideSessionVerification.verify(getFilesDir(),savedTrack,completedSegments,count,liveDistanceKm)' in ride and 'handler.post(()->{if(autoFinishPending)completeAutoTrack' in ride,
 "first ride proof depends on whole session":'whole!=null&&whole.ok' in proof and 'reportSessionCompletion' in ride,
 "top state and action rebuilt from true current state":'renderNextActions(nextActions,bikeId)' in ui and 'actionSignature(bikeId)' in ui and 'liveHeadline.setText(now)' in ui,
 "device names never included in report":'BikeObdPreferences.deviceName' not in privacy and 'CardoAutoRidePrefs.name' not in privacy and '장치 이름 비공개' in privacy,
 "real-device checks are not faked":'실제 기기에서 확인 필요' in hardware and 'FirstRideSetupProof.summary' in hardware and 'ObdLocalDiagnosticLog.recentManualSummary' in hardware,
 "GPS/ODO originals never overwritten":'target.put("odo"' not in verify and 'FileOutputStream' not in verify and 'FileOutputStream' not in proof,
 "JUnit regression coverage":all(x in testread('V605RideSessionVerificationTest.java') for x in ['multiPartValidatesAsWhole','missingEarlierMarkerIsNotCalledSuccessful','anUnlistedSplitFailsConservatively']) and 'oneProfileCannotCountTwice' in testread('V605CardoProfileReplyTest.java'),
}
for label,good in checks.items():print(('PASS' if good else 'FAIL')+': '+label)
assert all(checks.values()),"v605 automatic ride safety regressions"
