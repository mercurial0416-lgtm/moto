package com.motohub.next;
/** No emulator required: exact boundaries and no implicit ECU-write coverage. */
final class ObdCfHardeningTest {
 private static int n;
 static void ok(boolean value,String why){if(!value)throw new AssertionError(why);n++;}
 static void throwsIllegal(Runnable test){boolean refused=false;try{test.run();}catch(IllegalArgumentException e){refused=true;}ok(refused,"invalid value was accepted");}
 public static void main(String[] args){
  ObdRideAutoGate gate=new ObdRideAutoGate();
  ok(gate.stopAfterMs()==180_000L,"default 3 min");
  throwsIllegal(()->gate.setStopAfterMs(0));
  throwsIllegal(()->gate.setStopAfterMs(240_000L));
  gate.setStopAfterMs(300_000L);ok(gate.stopAfterMs()==300_000L,"5-min pause selected");
  ok(gate.accept(9,1000)==ObdRideAutoGate.Event.NONE,"first movement");
  ok(gate.accept(9,2000)==ObdRideAutoGate.Event.NONE,"second movement");
  ok(gate.accept(9,3000)==ObdRideAutoGate.Event.START,"third movement starts");
  boolean protectedChange=false;try{gate.setStopAfterMs(600_000L);}catch(IllegalStateException e){protectedChange=true;}
  ok(protectedChange,"cannot change pause while recording");
  gate.accept(0,4000);
  for(long t=14000;t<304000;t+=10000){
   ok(gate.accept(0,t)==ObdRideAutoGate.Event.NONE,"5-minute idle countdown continues");
  }
  ok(gate.accept(0,304000)==ObdRideAutoGate.Event.FINISH,"5-min rest completes");
  gate.setStopAfterMs(600_000L);ok(gate.stopAfterMs()==600_000L,"10-min rest");
  gate.accept(10,400000);gate.accept(10,401000);
  ok(gate.accept(10,402000)==ObdRideAutoGate.Event.START,"restart at speed");
  gate.accept(0,403000);gate.connectionLost();
  ok(gate.recording(),"Bluetooth outage must preserve ride state");
  ok(!gate.stoppedBeforeDisconnect(700000),"stale stop not proof of finished trip");
  ok(gate.accept(0,701000)==ObdRideAutoGate.Event.NONE,"fresh idle timer after reconnection");
  for(long t=711000;t<1301000;t+=10000){
   ok(gate.accept(0,t)==ObdRideAutoGate.Event.NONE,"10-minute idle countdown continues");
  }
  ok(gate.accept(0,1301000)==ObdRideAutoGate.Event.FINISH,"10-min rest after reconnection");
  ok(!ObdTelemetryFreshness.fresh(0,1000),"unavailable sample hidden");
  ok(!ObdTelemetryFreshness.fresh(2000,1000),"future sample rejected");
  ok(ObdTelemetryFreshness.speedFresh(1,12001),"fresh speed boundary");
  ok(!ObdTelemetryFreshness.speedFresh(1,12002),"stale speed hidden");
  ok(ObdTelemetryFreshness.fresh(1,50001),"fresh slow sensor boundary");
  ok(!ObdTelemetryFreshness.fresh(1,50002),"stale sensor hidden");
  ok("—".equals(ObdTelemetryFreshness.speedLabel(120,1,20000)),"no stale speed display");
  ok("0 km/h".equals(ObdTelemetryFreshness.speedLabel(0,1,5000)),"stationary speed valid");
  ok("—".equals(ObdTelemetryFreshness.speedLabel(250,1,5000)),"invalid speed hidden");
  ok(ObdTelemetryFreshness.connectionLabel(false,true,false).contains("GPS 단독"),"GPS fallback label");
  ok(ObdTelemetryFreshness.connectionLabel(false,false,false).contains("꺼짐"),"no auto-restart");
  throwsIllegal(()->ObdReadOnlyProtocol.query("04FF"));
  throwsIllegal(()->ObdReadOnlyProtocol.query("0101"));
  ok("010D\r".equals(ObdReadOnlyProtocol.query("010D")),"only authorized speed read");
  System.out.println("CF JVM hardening assertions passed: "+n);
 }
}
