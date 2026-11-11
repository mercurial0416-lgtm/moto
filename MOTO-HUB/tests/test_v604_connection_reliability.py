from pathlib import Path
root=Path(__file__).resolve().parents[1]
src=root/"app/src/main/java/com/motohub/next"
def read(n):return (src/n).read_text()
ui=read("BikeObdSettingsUi.java")
issues=read("ConnectionIssueGuide.java")
cardo=read("CardoConnectionProbe.java")
starter=read("CardoRideStarter.java")
main=read("MainActivity.java")
device=read("ObdLocalDiagnosticLog.java")
ride=read("RideService.java")
proof=read("FirstRideSetupProof.java")
events=read("ConnectionEventLog.java")
report=read("SafeConnectionDiagnosticReport.java")
checks={
 "BT permission decision precedes power-off":issues.index('if(!btPermission)')<issues.index('if(!bluetooth)'),
 "background location required in Cardo setup":'ACCESS_BACKGROUND_LOCATION' in ui and 'mode.needsCardo()' in ui and 'ACCESS_BACKGROUND_LOCATION' in starter,
 "wizard resumes after external settings":'BikeObdSettingsUi.notePause(this)' in main and 'BikeObdSettingsUi.refreshAfterReturn(this)' in main,
 "Cardo probe single flight + timeout":'PENDING.get(mac)' in cardo and 'existing.waiters.add(callback)' in cardo and 'TIMEOUT_MS=4000L' in cardo and 'closeProfileProxy' in cardo and 'if(p.finished)return' in cardo,
 "explicit speed PID time independent of other sensors":'speed_observed_at' in device and '"010D".equals(pid)' in device and 'recentManualSummary' in ui,
 "non-recording setup test":'private void showSafeConnectionTest' in ui and '자동기록 없이 연결 테스트' in ui and 'RideService.class' not in ui.split('private void showSafeConnectionTest')[1].split('private void startLiveStatus')[0],
 "last actual sensor success retained":'probe_last_received' in device and 'probe_valid' in device and '최근 실제 ECU 센서 응답' in device,
 "timed error categories rather than raw positions":'ConnectionEventLog.fromAutoStage' in read('ObdAutoRideProgress.java') and 'ConnectionEventLog.record' in starter and '20' in events and 'getLatitude(' not in events,
 "first-ride failure reasons":'FirstRideSetupProof.reportCompletion' in ride and '0.1km 미만' in proof and 'GPS 파일 저장 검증 실패' in proof,
 "one-tap sanitized diagnostic copy":'SafeConnectionDiagnosticReport.create' in ui and 'setPrimaryClip' in ui and 'redact(String text)' in report,
 "no ODO data mutation by diagnosis":'target.put("odo"' not in report and 'TrackFile' not in report,
}
for label,ok in checks.items():print(('PASS' if ok else 'FAIL')+': '+label)
assert all(checks.values()),'v604 diagnostics and connection safety'
