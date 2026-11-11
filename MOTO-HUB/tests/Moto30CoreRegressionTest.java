package com.motohub.next;
import java.util.Arrays;

/** Simulated ECU/GPS inputs, no motorcycle or Android permissions required. */
public final class Moto30CoreRegressionTest {
 private static int checks;
 private static void ok(boolean yes,String message){checks++;if(!yes)throw new AssertionError(message);}
 public static void main(String[] args){
  ObdRideAutoGate gate=new ObdRideAutoGate();
  ok(gate.accept(8,1000)==ObdRideAutoGate.Event.NONE,"first speed");
  ok(gate.accept(8,1000)==ObdRideAutoGate.Event.NONE,"duplicate speed");
  ok(gate.accept(8,1100)==ObdRideAutoGate.Event.NONE,"too soon");
  ok(gate.accept(8,1400)==ObdRideAutoGate.Event.NONE,"second independent sample");
  ok(gate.accept(8,1800)==ObdRideAutoGate.Event.START,"third valid sample");
  ok(!ObdOdoPollPolicy.shouldProbe(true,ObdPidCapabilities.Support.YES,80,60,false),"ODO cannot stall moving speed");
  ok(ObdOdoPollPolicy.shouldProbe(true,ObdPidCapabilities.Support.YES,0,60,false),"confirmed stationary ODO read");
  ok(!ObdOdoPollPolicy.shouldProbe(true,ObdPidCapabilities.Support.NO,0,80,false),"unsupported ODO never probed");
  ok(ObdOdoPollPolicy.shouldProbe(true,ObdPidCapabilities.Support.UNKNOWN,0,8,false),"one opt-in stationary probe");
  ok(!ObdOdoPollPolicy.shouldProbe(true,ObdPidCapabilities.Support.UNKNOWN,0,9,true),"unadvertised one-shot");
  ok(ObdReadOnlyProtocol.speedReplyDiagnosis("7E8 03 41 0D 28\r7E9 03 41 0D 40").contains("불일치"),"multi ECU disagreement diagnostic");
  ok(ObdReadOnlyProtocol.responseBytes("010D","7E8 03 41 0D 28\r7E9 03 41 0D 40")==null,"conflicting speeds never trigger start");
  ok(ObdReadOnlyProtocol.speedReplyDiagnosis("7E8 03 41 0D 28\r7E9 03 41 0D 28").contains("일치"),"multiple agreeing ECUs recognized");
  ok(ObdOdometer.udsDiagnosis("7F 22 31").contains("0x31"),"UDS negative response classified");
  java.util.List<Long> times=Arrays.asList(1000L,5000L,9000L);
  ok(RideReplayTimeline.targetTime(times,500)==5000L,"halfway recorded timestamp");
  ok(Math.abs(RideReplayTimeline.fraction(1000,5000,3000,true)-.5)<1e-6,"smooth interpolation");
  ok(RideReplayTimeline.fraction(1000,5000,3000,false)==0,"never interpolate across gaps");
  ok(MapPathDetailPolicy.corner(37,127,37.00015,127,37.00015,127.0002,5),"turn survives decimation");
  System.out.println("PASS Moto30CoreRegressionTest "+checks+" assertions");
 }
}
