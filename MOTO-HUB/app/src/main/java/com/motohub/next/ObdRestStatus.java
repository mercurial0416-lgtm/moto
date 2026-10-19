package com.motohub.next;

/** Never imply a ride has ended during a GPS/OBD signal outage. */
final class ObdRestStatus {
 private ObdRestStatus(){}
 static String label(boolean armed,boolean recording,long remainingMs){
  if(!armed)return recording?"OBD2 종료 · GPS 단독 기록 중":"자동 라이딩 감지 꺼짐";
  if(!recording)return "자동 라이딩 출발 대기";
  if(remainingMs<0)return "라이딩 기록 중 · 정차 종료 카운트 없음";
  long seconds=(remainingMs+999L)/1000L;
  return "정차 중 · 자동 저장까지 "+(seconds/60)+"분 "+(seconds%60)+"초";
 }
}
