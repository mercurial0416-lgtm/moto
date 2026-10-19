package com.motohub.next;
final class ObdStopGpsSafetyTest {
 static int checks;
 static void ok(boolean v){if(!v)throw new AssertionError("GPS stop safety");checks++;}
 public static void main(String[] args){
  ObdRideAutoGate g=new ObdRideAutoGate();
  g.accept(8,1_000);g.accept(8,2_000);ok(g.accept(8,3_000)==ObdRideAutoGate.Event.START);
  g.accept(0,4_000);
  ok(g.stationaryRemainingMs(4_000)==180_000);
  g.gpsMovement(15,5_000); // real GPS movement cancels zero ECU speed
  ok(g.stationaryRemainingMs(5_000)==-1);
  g.accept(0,6_000);
  ok(g.stationaryRemainingMs(6_000)==180_000);
  g.gpsMovement(Double.NaN,7_000);ok(g.stationaryRemainingMs(7_000)==179_000);
  g.gpsMovement(4.9,8_000);ok(g.stationaryRemainingMs(8_000)==178_000);
  g.gpsMovement(30,5_000);ok(g.stationaryRemainingMs(8_000)==178_000); // old timestamp ignored
  for(long t=11_000;t<186_000;t+=5_000)ok(g.accept(0,t)==ObdRideAutoGate.Event.NONE);
  ok(g.accept(0,186_000)==ObdRideAutoGate.Event.FINISH);
  g.gpsMovement(40,187_000);ok(!g.recording());
  System.out.println("OBD/GPS stop safety tests passed: "+checks);
 }
}
