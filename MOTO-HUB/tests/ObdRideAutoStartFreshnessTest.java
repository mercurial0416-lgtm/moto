package com.motohub.next;
public final class ObdRideAutoStartFreshnessTest {
 static int checks;
 static void expect(ObdRideAutoGate.Event wanted,ObdRideAutoGate.Event actual,String name){checks++;if(wanted!=actual)throw new AssertionError(name);}
 public static void main(String[] args){
  ObdRideAutoGate gate=new ObdRideAutoGate();
  expect(ObdRideAutoGate.Event.NONE,gate.accept(9,1000),"first start");
  expect(ObdRideAutoGate.Event.NONE,gate.accept(9,6000),"second in range");
  expect(ObdRideAutoGate.Event.NONE,gate.accept(9,12000),"gap resets");
  expect(ObdRideAutoGate.Event.NONE,gate.accept(9,13000),"fresh second");
  expect(ObdRideAutoGate.Event.START,gate.accept(9,14000),"fresh third");
  gate.reset();
  expect(ObdRideAutoGate.Event.NONE,gate.accept(10,1000),"first");
  expect(ObdRideAutoGate.Event.NONE,gate.accept(-1,2000),"bad invalid");
  expect(ObdRideAutoGate.Event.NONE,gate.accept(10,3000),"restart 1");
  expect(ObdRideAutoGate.Event.NONE,gate.accept(10,4000),"restart 2");
  expect(ObdRideAutoGate.Event.START,gate.accept(10,5000),"valid start");
  System.out.println("PASS ObdRideAutoStartFreshnessTest "+checks);
 }
}
