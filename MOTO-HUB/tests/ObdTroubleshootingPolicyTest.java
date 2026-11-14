package com.motohub.next;
public final class ObdTroubleshootingPolicyTest {
 static int n=0;
 static void has(String txt,String needle){n++;if(!txt.contains(needle))throw new AssertionError(txt+" / "+needle);}
 static String advice(boolean en,boolean arm,boolean gps,boolean bt,boolean loc,boolean notify,boolean speed,boolean ride){
  return ObdTroubleshootingPolicy.advice(en,arm,gps,bt,loc,notify,speed,ride);
 }
 public static void main(String[] args){
  has(advice(false,false,true,true,true,true,true,true),"꺼져");
  has(advice(true,true,true,true,false,true,true,true),"위치 권한");
  has(advice(true,true,true,true,true,false,true,true),"알림");
  has(advice(true,true,false,true,true,true,true,true),"GPS");
  has(advice(true,true,true,false,true,true,true,true),"Bluetooth");
  has(advice(true,false,true,true,true,true,true,true),"설정은 저장");
  has(advice(true,true,true,true,true,true,false,true),"속도 수신 기록");
  has(advice(true,true,true,true,true,true,true,false),"7km/h");
  has(advice(true,true,true,true,true,true,true,true),"자동 기록 시작 이력");
  System.out.println("ObdTroubleshootingPolicyTest PASS "+n);
 }
}
