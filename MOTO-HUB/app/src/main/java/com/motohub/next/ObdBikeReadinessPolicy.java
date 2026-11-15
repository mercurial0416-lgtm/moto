package com.motohub.next;

/** A bike's setup page must never claim another motorcycle's ECU connection. */
final class ObdBikeReadinessPolicy {
 private ObdBikeReadinessPolicy(){}
 static boolean ownsActiveMonitor(boolean activeMonitor,boolean approvedForSelectedBike){
  return activeMonitor&&approvedForSelectedBike;
 }
 static boolean anotherBikeActive(boolean activeMonitor,boolean approvedForSelectedBike){
  return activeMonitor&&!approvedForSelectedBike;
 }
 static String status(boolean approvedForBike,boolean activeMonitor,boolean recording,
                      boolean speedFresh){
  if(anotherBikeActive(activeMonitor,approvedForBike))
   return "다른 바이크 자동 감지 중 · 먼저 해당 라이딩에서 종료해 줘";
  if(ownsActiveMonitor(activeMonitor,approvedForBike)){
   if(recording)return "이 바이크 GPS 주행 기록 중";
   return speedFresh?"ECU 속도 수신 확인 · 자동 출발 대기":
    "OBD 자동 감지 켜짐 · ECU 속도값 확인 중";
  }
  if(recording)return "GPS 기록 중 · OBD 자동 연결은 중지됨";
  if(approvedForBike)return "설정 저장됨 · 자동 감지 서비스 재시작 필요";
  return "자동 기록 꺼짐 · 최초 한 번 연결 필요";
 }
}
