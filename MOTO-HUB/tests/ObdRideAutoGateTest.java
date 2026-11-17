package com.motohub.next;
final class ObdRideAutoGateTest {
 static int n;
 static void eq(ObdRideAutoGate.Event expected,ObdRideAutoGate.Event actual){
  if(expected!=actual)throw new AssertionError("expected "+expected+" got "+actual);n++;
 }
 static void yes(boolean b){if(!b)throw new AssertionError("unexpected state");n++;}
 public static void main(String[] args){
  ObdRideAutoGate g=new ObdRideAutoGate();
  // Connecting/idle alone does not produce bogus GPS trips.
  eq(ObdRideAutoGate.Event.NONE,g.accept(0,1000));
  eq(ObdRideAutoGate.Event.NONE,g.accept(4,2000));
  eq(ObdRideAutoGate.Event.NONE,g.accept(5,3000));
  eq(ObdRideAutoGate.Event.START,g.accept(6,4000));yes(g.recording());
  eq(ObdRideAutoGate.Event.NONE,g.accept(0,5000));
  for(long t=6000;t<185_000;t+=1000)eq(ObdRideAutoGate.Event.NONE,g.accept(0,t));
  eq(ObdRideAutoGate.Event.FINISH,g.accept(0,185_000));yes(!g.recording());
  // A second ride starts automatically without rearming or another confirmation.
  eq(ObdRideAutoGate.Event.NONE,g.accept(5,186_000));
  eq(ObdRideAutoGate.Event.START,g.accept(5,187_000));
  eq(ObdRideAutoGate.Event.NONE,g.accept(0,188_000));
  yes(!g.stoppedBeforeDisconnect(198_000));yes(g.stoppedBeforeDisconnect(204_000));
  eq(ObdRideAutoGate.Event.NONE,g.accept(-1,205_000));yes(!g.stoppedBeforeDisconnect(300_000));
  g.reset();yes(!g.recording());
  eq(ObdRideAutoGate.Event.NONE,g.accept(10,5));
  eq(ObdRideAutoGate.Event.NONE,g.accept(11,25_000)); // stale samples reset
  eq(ObdRideAutoGate.Event.START,g.accept(11,26_000));
  g.reset();
  eq(ObdRideAutoGate.Event.NONE,g.accept(1,1000));
  eq(ObdRideAutoGate.Event.NONE,g.accept(5,2000));
  eq(ObdRideAutoGate.Event.NONE,g.accept(1,3000)); // creeping stop interrupts
  eq(ObdRideAutoGate.Event.NONE,g.accept(5,4000));
  eq(ObdRideAutoGate.Event.START,g.accept(5,5000));
  eq(ObdRideAutoGate.Event.NONE,g.accept(0,6000));
  eq(ObdRideAutoGate.Event.NONE,g.accept(5,7000)); // movement cancels parked timeout
  eq(ObdRideAutoGate.Event.NONE,g.accept(0,8000));
  for(long t=9000;t<188_000;t+=1000)eq(ObdRideAutoGate.Event.NONE,g.accept(0,t));
  eq(ObdRideAutoGate.Event.FINISH,g.accept(0,188_000));
  eq(ObdRideAutoGate.Event.NONE,g.accept(255,189_000));
  eq(ObdRideAutoGate.Event.NONE,g.accept(-1,190_000));
  System.out.println("OBD auto ride gating checks passed: "+n);
 }
}
