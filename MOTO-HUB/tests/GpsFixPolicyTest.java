package com.motohub.next;

public final class GpsFixPolicyTest {
 static int checks;
 static void check(boolean result,String label){checks++;if(!result)throw new AssertionError(label);}
 public static void main(String[] args){
  long now=1_800_000_000_000L;
  check(GpsFixPolicy.acceptTime(now,now,0),"current fix accepted");
  check(GpsFixPolicy.acceptTime(now-30000,now,0),"normal delayed fix accepted");
  check(GpsFixPolicy.acceptTime(now+120000,now,0),"boundary clock skew accepted");
  check(!GpsFixPolicy.acceptTime(now+120001,now,0),"future GPS time rejected");
  check(!GpsFixPolicy.acceptTime(now+86400000,now,0),"day-ahead GPS time rejected");
  check(!GpsFixPolicy.acceptTime(now,now,now),"duplicate timestamp rejected");
  check(!GpsFixPolicy.acceptTime(now-1,now,now),"backward timestamp rejected");
  check(!GpsFixPolicy.acceptTime(0,now,0),"missing timestamp rejected");
  check(!GpsFixPolicy.acceptTime(now,0,0),"invalid device time rejected");
  check(GpsFixPolicy.acceptTime(now+1000,now,now-1000),"small forward skew remains usable");
  check(GpsFixPolicy.acceptTime(now+1000,now+2000,now),"valid next fix after skew accepted");
  System.out.println("PASS: "+checks+" GPS timestamp guards");
 }
}
