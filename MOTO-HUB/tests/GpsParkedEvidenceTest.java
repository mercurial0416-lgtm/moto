package com.motohub.next;

public final class GpsParkedEvidenceTest {
 static int passed=0;
 static void yes(boolean b,String desc){if(!b)throw new AssertionError(desc);passed++;}
 static void no(boolean b,String desc){yes(!b,desc);}
 public static void main(String[] args){
  GpsParkedEvidence e=new GpsParkedEvidence();
  long since=100_000L,now=since;
  // Stationary positions every 5 seconds over three minutes.
  for(int i=0;i<37;i++){now+=5_000L;e.observe(now,5_000L,0.2,5,5);}
  yes(e.sustained(now,180_000L),"continuous accurate stationary fixes enable parking");
  no(e.sustained(now,600_000L),"three minutes insufficient for ten-minute setting");
  no(e.sustained(now+16_000L,180_000L),"stale GPS cannot finish");
  e.reset();no(e.sustained(now,180_000L),"reset removes evidence");
  // A ten-minute gap followed by one stationary fix is NEVER ten minutes of parking.
  long afterGap=900_000L;
  e.observe(afterGap,600_000L,0,5,5);
  no(e.sustained(afterGap,180_000L),"single GPS fix after large gap");
  e.observe(afterGap+5_000L,5_000L,0.1,5,5);
  no(e.sustained(afterGap+5_000L,180_000L),"first stationary pair after gap");
  // GPS movement must revoke any accumulation.
  e.observe(afterGap+10_000L,5_000L,60,5,5);
  no(e.sustained(afterGap+10_000L+180_000L,180_000L),"moving revokes evidence");
  // Poor accuracy never becomes stationary proof.
  e.observe(afterGap+15_000L,5_000L,0,80,5);
  no(e.sustained(afterGap+15_000L,180_000L),"inaccurate fixes rejected");
  // Real blackout breaks a previously valid sequence.
  e.reset();now=100_000L;
  for(int i=0;i<40;i++){now+=5_000L;e.observe(now,5_000L,0.1,5,5);}
  yes(e.sustained(now,180_000L),"stationary before outage");
  e.observe(now+40_000L,5_000L,0.1,5,5);
  no(e.sustained(now+40_000L,180_000L),"wall gap invalidates evidence");
  e.reset();now=100_000L;
  for(int i=0;i<121;i++){now+=5_000L;e.observe(now,5_000L,0.1,5,5);}
  yes(e.sustained(now,600_000L),"ten-minute parking validated");
  e.observe(now+5_000L,5_000L,Double.NaN,5,5);
  no(e.sustained(now+5_000L,600_000L),"invalid distance resets");
  System.out.println("GpsParkedEvidenceTest PASS "+passed);
 }
}
