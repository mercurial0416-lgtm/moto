package com.motohub.next;

/** Monotonic time since latest supported speed sample, no stale speed guessed. */
final class ObdSignalAge {
 private ObdSignalAge(){}
 static String label(long sampleAt,long now){
  if(sampleAt<=0||now<sampleAt)return "응답 대기";
  long seconds=(now-sampleAt)/1000;
  return seconds<3?"방금 수신":seconds>=600?"10분 이상 신호 없음":
   "마지막 응답 "+seconds+"초 전";
 }
}
