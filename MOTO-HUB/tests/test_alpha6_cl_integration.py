from pathlib import Path
from version_consistency import current_version, debug_alignment_ok, signed_alignment_ok
root=Path(__file__).resolve().parents[1]
service=(root/'app/src/main/java/com/motohub/next/RideService.java').read_text()
checks={
 'new version metadata': current_version() is not None and debug_alignment_ok() and signed_alignment_ok(),
 'developer mode disarm stops OBD monitor': 'onObdDisconnected("개발자 모드가 종료되어 자동 감지를 중지했습니다.")' in service and 'if(monitor!=null)monitor.stop();' in service,
 'developer mode disarm preserves active GPS': 'if(!running)return;' in service and 'GPS track must stay recoverable/active' in service,
 'restore rejected start preserves foreground ride': 'if(!RideSessionInterlock.keepExistingSession(running,autoArmed))' in service and 'if(!RestoreRideGate.beginRideStart())' in service,
 'OBD and GPS disarm have separate actions': 'AUTO_DISARM.equals(action)' in service and 'STOP.equals(action)' in service,
 'no boot-initiated GPS or sharing': 'START_NOT_STICKY' in service and 'Sharing.stop(); // Automatic local rides' in service,
}
for name,ok in checks.items():print(('PASS' if ok else 'FAIL')+': '+name)
assert all(checks.values())
