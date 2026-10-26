package com.motohub.next;
public final class ObdIdleStopPreferenceTest {
 static int n;
 static void eq(ObdRideAutoGate.Event expected,ObdRideAutoGate.Event actual){
  if(expected!=actual)throw new AssertionError("expected "+expected+" got "+actual);n++;
 }
 static void ok(boolean condition,String name){n++;if(!condition)throw new AssertionError(name);}
 public static void main(String[] args){
  ok(ObdRideAutoGate.USER_DEFAULT_STOP_AFTER_MS==600_000L,"user default ten minutes");
  ObdRideAutoGate gate=new ObdRideAutoGate();
  gate.setStopAfterMs(ObdRideAutoGate.USER_DEFAULT_STOP_AFTER_MS);
  eq(ObdRideAutoGate.Event.NONE,gate.accept(7,1000));
  eq(ObdRideAutoGate.Event.NONE,gate.accept(7,2000));
  eq(ObdRideAutoGate.Event.START,gate.accept(7,3000));
  eq(ObdRideAutoGate.Event.NONE,gate.accept(0,4000));
  for(long t=8000;t<604000;t+=4000)eq(ObdRideAutoGate.Event.NONE,gate.accept(0,t));
  eq(ObdRideAutoGate.Event.FINISH,gate.accept(0,604000));
  ok(!gate.recording(),"trip ends after ten minutes of verified stop");
  gate.reset();gate.setStopAfterMs(600000);
  gate.accept(8,1000);gate.accept(8,2000);eq(ObdRideAutoGate.Event.START,gate.accept(8,3000));
  gate.accept(0,4000);gate.connectionLost();
  ok(gate.recording(),"ECU lost never splits GPS track");
  eq(ObdRideAutoGate.Event.NONE,gate.accept(-1,700000));
  ok(gate.recording(),"no speed reading is not a trip end");
  gate.reset();gate.setStopAfterMs(180000);
  ok(gate.stopAfterMs()==180000,"user retains fast end choice");
  System.out.println("PASS ObdIdleStopPreferenceTest "+n);
 }
}