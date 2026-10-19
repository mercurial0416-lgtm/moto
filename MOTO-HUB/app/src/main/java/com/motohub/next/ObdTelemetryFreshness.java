package com.motohub.next;
/** Monotonic-clock freshness validation. Never infer absent ECU readings. */
final class ObdTelemetryFreshness {
 static final long SENSOR_TTL_MS=50_000L;
 static final long SPEED_TTL_MS=12_000L;
 static boolean fresh(long sampleElapsedMs,long nowElapsedMs){
  return recent(sampleElapsedMs,nowElapsedMs,SENSOR_TTL_MS);
 }
 static boolean speedFresh(long sampleElapsedMs,long nowElapsedMs){return recent(sampleElapsedMs,nowElapsedMs,SPEED_TTL_MS);}
 private static boolean recent(long sample,long now,long limit){return sample>0&&now>=sample&&now-sample<=limit;}
 static String speedLabel(int value,long sampled,long now){
  return speedFresh(sampled,now)&&value>=0&&value<=200?value+" km/h":"—";
 }
 static String connectionLabel(boolean armed,boolean recording,boolean fresh){
  if(!armed)return recording?"GPS 단독 기록 · OBD2 연결 종료":"OBD2 자동 감지 꺼짐";
  return fresh?"차량 데이터 수신 중":"차량 신호 대기 · 마지막 값을 표시하지 않음";
 }
 private ObdTelemetryFreshness(){}
}
