package com.motohub.next;

/** Non-Android deterministic diagnostics and rest-timer regression coverage. */
public final class ObdDiagnosticSummaryTest {
 private static int n=0;
 private static void check(boolean condition,String note){n++;if(!condition)throw new AssertionError(note);}
 public static void main(String[] unused){
  ObdAuxSensorPolicy sensors=new ObdAuxSensorPolicy();
  long now=2_000_000_000_000L;
  check(sensors.summary(now).contains("RPM 정상 0/오류 0"),"empty diagnostics");
  sensors.failed("010C",now);
  sensors.failed("010C",now+1);
  String suppressed=sensors.summary(now+1);
  check(suppressed.contains("RPM 정상 0/오류 2"),"per-PID cumulative failures");
  check(suppressed.contains("일시중지 60초"),"cooldown remaining visible");
  check(!sensors.mayPoll("010C",now+2),"RPM stopped during cooldown");
  check(sensors.mayPoll("0105",now+2),"coolant unaffected");
  check(!sensors.summary(now+ObdAuxSensorPolicy.COOLDOWN_NS+2).contains("일시중지"),"expired cooldown not shown");
  sensors.succeeded("010C",now+ObdAuxSensorPolicy.COOLDOWN_NS+2);
  check(sensors.summary(now+ObdAuxSensorPolicy.COOLDOWN_NS+2).contains("RPM 정상 1/오류 2"),"counters retained");
  check(sensors.summary(now+ObdAuxSensorPolicy.COOLDOWN_NS+2).contains("최근 0초 전"),"last valid read");
  sensors.failed("0142",now);
  check(sensors.summary(now).contains("전압 정상 0/오류 1"),"voltage independent");
  sensors.failed("FFFF",now);
  check(!sensors.summary(now).contains("FFFF"),"no unexpected PID");
  ObdRideAutoGate ride=new ObdRideAutoGate();
  check(ride.stationaryRemainingMs(0)==-1,"not recording");
  ride.accept(8,1000);ride.accept(8,2000);
  check(ride.accept(8,3000)==ObdRideAutoGate.Event.START,"start");
  check(ride.stationaryRemainingMs(3000)==-1,"moving");
  ride.accept(0,4000);
  check(ride.stationaryRemainingMs(4000)==180000L,"full rest interval");
  check(ride.stationaryRemainingMs(65_000)==-1,"stale OBD sample not a countdown");
  ride.accept(0,5000);
  check(ride.stationaryRemainingMs(10000)==174000L,"remaining rest interval");
  check(ObdRestStatus.label(true,true,61000).contains("1분 1초"),"minute/second presentation");
  check(ObdRestStatus.label(false,true,-1).contains("GPS 단독"),"OBD disarmed while GPS running");
  ride.accept(4,11000);
  check(ride.stationaryRemainingMs(11000)==-1,"movement cancels rest countdown");
  ride.connectionLost();
  check(ride.stationaryRemainingMs(12000)==-1,"disconnect hides idle countdown");
  check(ObdRestStatus.label(true,false,-1).contains("출발 대기"),"armed standby");
  System.out.println("PASS: ObdDiagnosticSummaryTest "+n+" assertions");
 }
}
