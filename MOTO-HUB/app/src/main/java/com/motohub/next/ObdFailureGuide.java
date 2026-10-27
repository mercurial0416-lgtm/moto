package com.motohub.next;

/** Actionable, redacted troubleshooting. Never expose MAC or stack traces. */
final class ObdFailureGuide {
 private ObdFailureGuide(){}
 static String classify(String text){
  String value=text==null?"":text.toLowerCase(java.util.Locale.ROOT);
  if(value.contains("permission")||value.contains("security")||value.contains("권한"))
   return "블루투스 권한 오류 · 설정에서 주변 기기 권한 확인";
  if(value.contains("unsupported")||value.contains("미지원")||value.contains("010d"))
   return "차량 속도 PID 미지원 · 어댑터/차종 호환성 확인";
  if(value.contains("timeout")||value.contains("응답")||value.contains("prompt"))
   return "ECU·ELM327 응답 지연 · 점화 상태와 어댑터 전원 확인";
  if(value.contains("bluetooth")||value.contains("socket")||value.contains("connect")||value.contains("연결"))
   return "Bluetooth 연결 오류 · 페어링과 장치 전원 확인";
  return "OBD 통신 장애 · 어댑터 연결과 ECU 지원 여부 확인";
 }
}
