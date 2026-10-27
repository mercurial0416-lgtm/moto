package com.motohub.next;

/** Human-readable advisory only. No ECU commands or vehicle health guarantees. */
final class ObdQualityAdvice {
 private ObdQualityAdvice(){}
 static String explain(boolean connected,long latencyMs,int reconnects,int failures){
  if(!connected)return "속도 신호 없음 · Bluetooth 페어링, 점화 상태와 어댑터 전원을 확인하세요";
  if(reconnects>=3)return "재연결 반복 · 어댑터 접점/전원과 주변 Bluetooth 간섭을 확인하세요";
  if(latencyMs>1300)return "응답 지연 · 동글 통신 품질을 확인하세요. ECU 고장 판정은 불가";
  if(failures>=3)return "OBD 속도 응답 누락 · 연결 상태와 동글 호환성을 확인하세요";
  return "OBD 통신 참고값 · 차량 조작 없이 데이터 수신 중";
 }
}
