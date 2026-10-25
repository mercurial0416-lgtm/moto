package com.motohub.next;

/** Allowlisted diagnostic events only; no arbitrary device IDs or location strings. */
final class ObdDiagnosticEventPolicy {
 private ObdDiagnosticEventPolicy(){}
 static boolean valid(String code){
  return "BT_OK".equals(code)||"PID".equals(code)||"SENSOR_OK".equals(code)||
   "NO_ECU".equals(code)||"NO_SPEED".equals(code)||"RECONNECT".equals(code)||
   "AUTO_ON".equals(code)||"AUTO_OFF".equals(code)||"GPS_STARTED".equals(code)||
   "GPS_SAVED".equals(code)||"ERROR".equals(code);
 }
 static String text(String code){
  switch(code){
   case "BT_OK":return "Bluetooth 연결 확인";
   case "PID":return "ECU 지원 목록 확인";
   case "SENSOR_OK":return "ECU 센서 정상 수신";
   case "NO_ECU":return "ECU 센서 미응답";
   case "NO_SPEED":return "차량 속도 미지원 또는 무효";
   case "RECONNECT":return "Bluetooth 연결 재시도";
   case "AUTO_ON":return "자동 라이딩 감지 시작 요청";
   case "AUTO_OFF":return "자동 라이딩 감지 종료";
   case "GPS_STARTED":return "GPS 기록 시작";
   case "GPS_SAVED":return "GPS 기록 저장";
   case "ERROR":return "OBD 통신 오류";
   default:return "";
  }
 }
}
