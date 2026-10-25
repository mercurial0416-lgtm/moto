from pathlib import Path
from version_consistency import current_version, debug_alignment_ok, signed_alignment_ok
root=Path(__file__).resolve().parents[1]
src=root/'app/src/main/java/com/motohub/next'
service=(src/'RideService.java').read_text()
monitor=(src/'ObdAutoSpeedMonitor.java').read_text()
ui=(src/'RideUi.java').read_text()
complete=(src/'TrackCompletion.java').read_text()
checks={
 'CK version and signed/debug checkpoint alignment': current_version() is not None and debug_alignment_ok() and signed_alignment_ok(),
 'OBD retry budget consumed after healthy session': 'connectedAtNanos=0;' in monitor and 'ObdConnectionPolicy.nextFailureCount' in monitor,
 'GPS completion validates original bytes': 'TrackFile.read(track' in complete and "input.read()!='\\n'" in complete,
 'GPS service completion goes through validator': 'TrackCompletion.mark(track,count)' in service,
 'failed GPS completion is recoverable': 'GPS 기록 검증 실패' in service and 'boolean verified=closeTrack(true)' in service,
 'GPS outage does not silently complete': 'GPS 일시 중단' in service and 'lastGood=null' in service,
 'manual/automatic STOP separated': 'AUTO_DISARM.equals(action)' in service and 'STOP.equals(action)' in service,
 'auto ride offers explicit GPS finish button': '라이딩 기록 저장 후 종료' in ui and 'RideService.autoArmed&&RideService.running' in ui,
 'Kakao navigation is separate and does not upload': 'KakaoNaviLaunch.open(a)' in ui and '경로·위치 정보는 자동 전달되지 않아요.' in ui,
 'OBD remains developer-only and read only': 'developer_experiments' in service and 'ObdReadOnlyProtocol.query' in monitor,
 'no implicit live sharing on automatic start': 'Sharing.stop(); // Automatic local rides' in service,
 'GPS tracks retain crash recovery marker convention': '".done"' in complete,
}
for name,ok in checks.items():print(('PASS' if ok else 'FAIL')+': '+name)
assert all(checks.values()),'CK source integration failure'
