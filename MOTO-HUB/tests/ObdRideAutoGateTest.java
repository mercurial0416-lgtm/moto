package com.motohub.next;
final class ObdRideAutoGateTest {
 static int n;
 static void eq(ObdRideAutoGate.Event expected,ObdRideAutoGate.Event actual){if(expected!=actual)throw new AssertionError("expected "+expected+" got "+actual);n++;}
 static void yes(boolean b){if(!b)throw new AssertionError("unexpected state");n++;}
 public static void main(String[] args){
  ObdRideAutoGate g=new ObdRideAutoGate();
  eq(ObdRideAutoGate.Event.NONE,g.accept(0,1000));
  eq(ObdRideAutoGate.Event.NONE,g.accept(7,2000));
  eq(ObdRideAutoGate.Event.NONE,g.accept(8,3000));
  eq(ObdRideAutoGate.Event.START,g.accept(9,4000));yes(g.recording());
  eq(ObdRideAutoGate.Event.NONE,g.accept(0,5000));
  for(long t=6000;t<185_000;t+=1000)eq(ObdRideAutoGate.Event.NONE,g.accept(0,t));
  eq(ObdRideAutoGate.Event.FINISH,g.accept(0,185_000));yes(!g.recording());
  eq(ObdRideAutoGate.Event.NONE,g.accept(7,186_000));
  eq(ObdRideAutoGate.Event.NONE,g.accept(7,187_000));
  eq(ObdRideAutoGate.Event.START,g.accept(7,188_000));
  eq(ObdRideAutoGate.Event.NONE,g.accept(0,189_000));
  yes(!g.stoppedBeforeDisconnect(199_000));yes(g.stoppedBeforeDisconnect(204_000));
  eq(ObdRideAutoGate.Event.NONE,g.accept(-1,205_000));yes(!g.stoppedBeforeDisconnect(300_000));
  g.reset();yes(!g.recording());
  eq(ObdRideAutoGate.Event.NONE,g.accept(10,5));
  eq(ObdRideAutoGate.Event.NONE,g.accept(11,25_000)); // stale samples may not trigger false starts
  eq(ObdRideAutoGate.Event.NONE,g.accept(11,26_000));
  eq(ObdRideAutoGate.Event.START,g.accept(11,27_000));
  g.reset();
  eq(ObdRideAutoGate.Event.NONE,g.accept(1,1000));
  eq(ObdRideAutoGate.Event.NONE,g.accept(7,2000));
  eq(ObdRideAutoGate.Event.NONE,g.accept(1,3000));
  eq(ObdRideAutoGate.Event.NONE,g.accept(7,4000));
  eq(ObdRideAutoGate.Event.NONE,g.accept(7,5000));
  eq(ObdRideAutoGate.Event.START,g.accept(7,6000));
  eq(ObdRideAutoGate.Event.NONE,g.accept(0,7000));
  eq(ObdRideAutoGate.Event.NONE,g.accept(5,8000)); // creeping in traffic cancels stationary timeout
  eq(ObdRideAutoGate.Event.NONE,g.accept(0,9000));
  for(long t=10_000;t<189_000;t+=1000)eq(ObdRideAutoGate.Event.NONE,g.accept(0,t));
  eq(ObdRideAutoGate.Event.FINISH,g.accept(0,189_000));
  eq(ObdRideAutoGate.Event.NONE,g.accept(255,190_000));
  eq(ObdRideAutoGate.Event.NONE,g.accept(-1,191_000));
  System.out.println("OBD auto ride gating checks passed: "+n);
 }
}
