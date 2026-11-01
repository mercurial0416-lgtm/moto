package com.motohub.next;
/** No Android dependencies: safeguards for automatic ODO before touching real motorcycle records. */
public final class Ux104OdoAutoSyncTest {
 static void ok(boolean b,String what){if(!b)throw new AssertionError(what);}
 static String raw(long tenths){return String.format("41 A6 %02X %02X %02X %02X",(tenths>>>24)&255,(tenths>>>16)&255,(tenths>>>8)&255,tenths&255);}
 public static void main(String[] args){
  ok(OdoAutoSyncPolicy.first(1000,1001.3),"first reading while moving within verified baseline");
  ok(!OdoAutoSyncPolicy.first(1000,1020),"unconfirmed old odometer blocks auto overwrite");
  ok(!OdoAutoSyncPolicy.first(0,5),"zero baseline blocks");
  ok(!OdoAutoSyncPolicy.first(1000,Double.NaN),"invalid first sample");
  ok(OdoAutoSyncPolicy.next(1000,1001.2,60_000),"1.2 km per minute plausible");
  ok(!OdoAutoSyncPolicy.next(1000,1200,60_000),"jump 200 km in a minute rejected");
  ok(!OdoAutoSyncPolicy.next(1001,1000,60_000),"rollback rejected");
  ok(!OdoAutoSyncPolicy.next(1000,1000,-1),"invalid clock");
  ok(OdoAutoSyncPolicy.consistent(1050.0,1050.1)==1050.1,"two close samples");
  ok(Double.isNaN(OdoAutoSyncPolicy.consistent(1050,1060)),"conflicting samples");
  ok(Double.isNaN(OdoAutoSyncPolicy.consistent(Double.NaN,1060)),"missing ECU sample");
  ok("01A6\r".equals(ObdReadOnlyProtocol.query("01A6")),"explicit allowlisted read only request");
  ok(Double.isFinite(ObdOdometer.parseKm(raw(12345))),"valid standard A6 reply");
  ok(Math.abs(ObdOdometer.parseKm(raw(12345))-1234.5)<0.01,"standard A6 0.1 km resolution");
  ok(Double.isNaN(ObdOdometer.parseKm(raw(12345)+"\n"+raw(12355))),"ECU disagreement must fail");
  String a0="41 A0 04 00 00 00"; // A6 => bit (192-166)=26.
  ObdPidCapabilities yes=ObdPidCapabilities.fromAllResponses(null,null,null,a0);
  ok(yes.forPid("01A6")==ObdPidCapabilities.Support.YES,"ECU supported A6 bit");
  ok(ObdPidCapabilities.fromAllResponses(null,null,null,"41 A0 00 00 00 00").forPid("01A6")==ObdPidCapabilities.Support.NO,"negative bit");
  ok(ObdPidCapabilities.unknown().forPid("01A6")==ObdPidCapabilities.Support.UNKNOWN,"missing bitmap never assume supported");
  System.out.println("PASS v567 ODO baseline, plausibility, double sampling, allowlist, capability");
 }
}
