package com.motohub.next;
public final class ObdBikeReadinessPolicyTest {
 static int tests;
 static void has(String actual,String expected){tests++;if(!actual.contains(expected))throw new AssertionError(actual+" missing "+expected);}
 static void yes(boolean result){tests++;if(!result)throw new AssertionError("expected true");}
 static void no(boolean result){tests++;if(result)throw new AssertionError("expected false");}
 public static void main(String[] args){
  has(ObdBikeReadinessPolicy.status(false,true,false,true),"다른 바이크");
  has(ObdBikeReadinessPolicy.status(true,true,false,false),"속도값 확인 중");
  has(ObdBikeReadinessPolicy.status(true,true,false,true),"속도 수신 확인");
  has(ObdBikeReadinessPolicy.status(true,true,true,false),"GPS 주행 기록 중");
  has(ObdBikeReadinessPolicy.status(true,false,false,false),"재시작 필요");
  has(ObdBikeReadinessPolicy.status(false,false,false,false),"최초 한 번");
  has(ObdBikeReadinessPolicy.status(false,false,true,false),"GPS 기록 중");
  yes(ObdBikeReadinessPolicy.ownsActiveMonitor(true,true));
  no(ObdBikeReadinessPolicy.ownsActiveMonitor(true,false));
  yes(ObdBikeReadinessPolicy.anotherBikeActive(true,false));
  no(ObdBikeReadinessPolicy.anotherBikeActive(false,false));
  System.out.println("ObdBikeReadinessPolicyTest PASS "+tests);
 }
}
