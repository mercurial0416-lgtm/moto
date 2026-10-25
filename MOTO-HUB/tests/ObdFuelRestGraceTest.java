package com.motohub.next;
public final class ObdFuelRestGraceTest {
 static int n=0;
 static void eq(ObdRideAutoGate.Event expected,ObdRideAutoGate.Event actual,String label){
  n++;if(expected!=actual)throw new AssertionError(label+": "+actual);
 }
 static void ok(boolean good,String label){n++;if(!good)throw new AssertionError(label);}
 public static void main(String[] args){
  ObdRideAutoGate gate=new ObdRideAutoGate();
  ok(!gate.extendStopForBreak(600000),"cannot extend unstarted ride");
  eq(ObdRideAutoGate.Event.NONE,gate.accept(9,0),"first");
  eq(ObdRideAutoGate.Event.NONE,gate.accept(9,1000),"second");
  eq(ObdRideAutoGate.Event.START,gate.accept(9,2000),"start");
  eq(ObdRideAutoGate.Event.NONE,gate.accept(0,3000),"stationary");
  ok(!gate.extendStopForBreak(1200000),"fixed grace only");
  ok(gate.extendStopForBreak(600000),"allow user break extension");
  ok(gate.stopAfterMs()==600000,"extended to 10m");
  for(long t=7000;t<603000;t+=4000)
   eq(ObdRideAutoGate.Event.NONE,gate.accept(0,t),"no split inside 10m");
  eq(ObdRideAutoGate.Event.FINISH,gate.accept(0,603000),"end at 10m");
  ok(gate.stopAfterMs()==180000,"restore default after save");
  eq(ObdRideAutoGate.Event.NONE,gate.accept(9,604000),"next ride first");
  eq(ObdRideAutoGate.Event.NONE,gate.accept(9,605000),"next ride second");
  eq(ObdRideAutoGate.Event.START,gate.accept(9,606000),"next ride started");
  eq(ObdRideAutoGate.Event.NONE,gate.accept(0,607000),"next rest");
  for(long t=611000;t<787000;t+=4000)
   eq(ObdRideAutoGate.Event.NONE,gate.accept(0,t),"3m default retained");
  eq(ObdRideAutoGate.Event.FINISH,gate.accept(0,787000),"3m end");
  gate.setStopAfterMs(300000);
  gate.reset();
  ok(gate.stopAfterMs()==300000,"retain chosen 5m preference");
  System.out.println("PASS ObdFuelRestGraceTest "+n);
 }
}
