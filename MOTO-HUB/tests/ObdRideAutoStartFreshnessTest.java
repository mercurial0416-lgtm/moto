package com.motohub.next;
public final class ObdRideAutoStartFreshnessTest {
 static int checks;
 static void expect(ObdRideAutoGate.Event wanted,ObdRideAutoGate.Event actual,String name){
  checks++;if(wanted!=actual)throw new AssertionError(name+": wanted "+wanted+", got "+actual);
 }
 public static void main(String[] args){
  ObdRideAutoGate gate=new ObdRideAutoGate();
  expect(ObdRideAutoGate.Event.NONE,gate.accept(5,1000),"first movement");
  expect(ObdRideAutoGate.Event.NONE,gate.accept(5,1300),"duplicate fast sample ignored");
  expect(ObdRideAutoGate.Event.START,gate.accept(5,2300),"second independent moving sample");
  gate.reset();
  expect(ObdRideAutoGate.Event.NONE,gate.accept(9,1000),"first start");
  expect(ObdRideAutoGate.Event.NONE,gate.accept(9,7000),"gap clears old first sample");
  expect(ObdRideAutoGate.Event.START,gate.accept(9,8000),"fresh second");
  gate.reset();
  expect(ObdRideAutoGate.Event.NONE,gate.accept(10,1000),"first");
  expect(ObdRideAutoGate.Event.NONE,gate.accept(-1,2000),"bad invalid resets");
  expect(ObdRideAutoGate.Event.NONE,gate.accept(10,3000),"restart 1");
  expect(ObdRideAutoGate.Event.START,gate.accept(10,4000),"restart 2");
  gate.reset();
  expect(ObdRideAutoGate.Event.NONE,gate.accept(4,1000),"walking/idle does not start");
  expect(ObdRideAutoGate.Event.NONE,gate.accept(5,2000),"first >5");
  expect(ObdRideAutoGate.Event.NONE,gate.accept(4,3000),"below threshold breaks run");
  expect(ObdRideAutoGate.Event.NONE,gate.accept(5,4000),"fresh first");
  expect(ObdRideAutoGate.Event.START,gate.accept(5,5000),"fresh second");
  System.out.println("PASS ObdRideAutoStartFreshnessTest "+checks);
 }
}
