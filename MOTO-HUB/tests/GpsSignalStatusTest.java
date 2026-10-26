package com.motohub.next;
public final class GpsSignalStatusTest {
 static int checks;
 static void ok(boolean p,String name){checks++;if(!p)throw new AssertionError(name);}
 public static void main(String[] args){
  ok(!GpsSignalStatus.fresh(0,1000),"missing GPS sample");
  ok(!GpsSignalStatus.fresh(10000,1000),"future monotonic time");
  ok(GpsSignalStatus.fresh(1000,16000),"fresh boundary");
  ok(!GpsSignalStatus.fresh(1000,16001),"stale threshold");
  ok(!GpsSignalStatus.lost(1000,36000),"outage boundary");
  ok(GpsSignalStatus.lost(1000,36001),"outage beyond threshold");
  ok(GpsSignalStatus.liveSpeedLabel(51,1000,17000).equals("GPS 대기"),"stale speed never zero");
  ok(GpsSignalStatus.liveSpeedLabel(0,1000,2000).equals("0 km/h"),"fresh real zero allowed");
  ok(GpsSignalStatus.liveSpeedLabel(38.5,1000,2000).equals("39 km/h"),"fresh actual value");
  ok(GpsSignalStatus.liveSpeedLabel(Double.NaN,1000,2000).equals("GPS 대기"),"nan absent");
  ok(GpsSignalStatus.liveSpeedLabel(-1,1000,2000).equals("GPS 대기"),"negative value rejected");
  ok(GpsSignalStatus.liveSpeedLabel(500,1000,2000).equals("GPS 대기"),"implausible speed rejected");
  System.out.println("PASS GpsSignalStatusTest "+checks);
 }
}