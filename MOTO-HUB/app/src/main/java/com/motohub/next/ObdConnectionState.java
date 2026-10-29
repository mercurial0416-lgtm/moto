package com.motohub.next;

/** Conservative local-only OBD link diagnosis. Never treats Bluetooth pairing as live ECU data. */
final class ObdConnectionState {
 private ObdConnectionState(){}
 static String label(boolean armed,long lastSpeedAt,long now,int attempts,int successes,
                     int reconnects,String lastError,String supportedPids){
  if(!armed)return "OBD 자동 감지 꺼짐 · GPS 단독 기록 가능";
  if(ObdTelemetryFreshness.speedFresh(lastSpeedAt,now))return
   "차량 속도 데이터 수신 중 · 센서 정상";
  String error=lastError==null?"":lastError.toLowerCase(java.util.Locale.ROOT);
  if(error.contains("010d")||error.contains("speed pid")||error.contains("속도 pid")||
      error.contains("unsupported")||error.contains("미지원")||
      (supportedPids!=null&&supportedPids.contains("속도 PID 미지원")))
   return "ECU 속도 PID 미지원 가능 · GPS 단독 기록 확인";
  if(attempts>successes&&attempts>0&&reconnects>0)
   return "Bluetooth 재접속 시도 중 · GPS 기록은 계속";
  if(attempts>0&&successes==0)
   return "동글 연결 대기 중 · 페어링과 전원 확인";
  if(lastSpeedAt>0&&now>=lastSpeedAt)
   return "속도 데이터 응답 지연 · 마지막 값을 현재 속도로 사용하지 않음";
  return "속도 데이터 최초 수신 대기 · ECU 지원 여부 미확인";
 }
}