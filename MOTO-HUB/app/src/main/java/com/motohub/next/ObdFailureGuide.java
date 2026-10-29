package com.motohub.next;

/** User-facing OBD diagnosis; never include adapter identifiers or exception details. */
final class ObdFailureGuide {
 private ObdFailureGuide(){}
 static String classify(String text){
  String v=text==null?"":text.toLowerCase(java.util.Locale.ROOT);
  if(v.contains("bluetooth off")||v.contains("adapter disabled")||v.contains("블루투스 꺼"))
   return "휴대폰 Bluetooth 꺼짐 · 설정에서 켠 후 직접 재연결";
  if(v.contains("device busy")||v.contains("resource busy")||v.contains("already in use"))
   return "동글 연결 점유 중 · 다른 OBD 앱을 닫고 재연결";
  if(v.contains("no data")||v.contains("no response")||v.contains("데이터 없음"))
   return "ECU 센서 응답 없음 · GPS 단독 기록 가능, 시동·호환성 확인";
  if(v.contains("permission")||v.contains("security")||v.contains("권한"))
   return "주변 기기 권한 없음 · Android 설정에서 Bluetooth 권한 허용";
  if(v.contains("010d")||v.contains("speed pid")||v.contains("속도 pid")
    ||v.contains("unsupported")||v.contains("미지원"))
   return "속도 PID 미지원 · ECU/6핀 변환 케이블 호환성 확인, GPS 단독 기록 가능";
  if(v.contains("disconnect")||v.contains("broken pipe")||v.contains("연결 끊")
    ||v.contains("connection reset")||v.contains("socket closed"))
   return "Bluetooth 통신 끊김 · 동글 전원·페어링 확인 후 재연결";
  if(v.contains("timeout")||v.contains("timed out")||v.contains("응답 지연")
    ||v.contains("prompt")||v.contains("응답 없음")||v.contains("응답 완료"))
   return "ECU·ELM327 응답 지연 · 시동 ON, 동글 전원 및 케이블 확인";
  if(v.contains("bluetooth")||v.contains("socket")||v.contains("connect")||v.contains("연결"))
   return "Bluetooth 연결 오류 · 기기 페어링 및 동글 전원 확인";
  return "OBD 통신 장애 · 연결 진단에서 속도 PID 지원 여부 확인";
 }
}
