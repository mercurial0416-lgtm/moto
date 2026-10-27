package com.motohub.next;

/** Read-only, in-session estimate, not an ECU health grade or safety guarantee. */
final class ObdLinkQuality {
 private ObdLinkQuality(){}
 static int score(long lastSample,long now,long avgLatency,int samples,int failures,int reconnects,int attempts,int successes){
  if(lastSample<=0||now<lastSample||now-lastSample>12000||samples<3)return 0;
  int result=100;
  long age=now-lastSample;
  if(age>3000)result-=20;
  if(age>6000)result-=25;
  if(avgLatency<0)result-=10;
  else if(avgLatency>1800)result-=35;
  else if(avgLatency>900)result-=20;
  else if(avgLatency>450)result-=10;
  result-=Math.min(25,Math.max(0,failures)*5);
  result-=Math.min(24,Math.max(0,reconnects)*8);
  if(attempts>0&&successes>=0){
   int pct=(int)(100L*Math.min(successes,attempts)/attempts);
   if(pct<50)result-=15;
   else if(pct<85)result-=7;
  }
  return Math.max(0,Math.min(100,result));
 }
 static String label(int score,int samples){
  if(samples<3||score<=0)return "신호 부족 · 평가 보류";
  return (score>=80?"안정":score>=55?"주의":"불안정")+" · 연결 품질 추정 "+score+"/100";
 }
}
