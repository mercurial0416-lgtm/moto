package com.motohub.next;

/** Pure JVM regressions for v568 GPS start exclusion and conservative ECU ODO behavior. */
public final class Ux105ReliabilityTest {
 static int checks;
 static void ok(boolean value,String reason){checks++;if(!value)throw new AssertionError(reason);}
 public static void main(String[] args){
  RestoreRideGate.end();RestoreRideGate.endExport();RestoreRideGate.endFormWrite();
  ok(RestoreRideGate.beginFormWrite(),"begin save");
  ok(!RestoreRideGate.beginRideStart(),"OBD auto start cannot interrupt atomic save");
  ok(!RestoreRideGate.begin(false,false),"restore cannot interrupt atomic save");
  RestoreRideGate.endFormWrite();
  ok(RestoreRideGate.beginRideStart(),"auto start allowed after save");
  ok(!RestoreRideGate.beginExport(false,false),"ZIP export blocked during start");
  RestoreRideGate.endRideStart();
  ok(RestoreRideGate.begin(false,false),"ZIP/JSON restore may begin");
  ok(!RestoreRideGate.beginRideStart(),"OBD auto start blocked during restore");
  RestoreRideGate.end();
  ok(RestoreRideGate.beginExport(false,false),"ZIP can export while idle");
  ok(!RestoreRideGate.beginRideStart(),"OBD auto start blocked during ZIP export");
  RestoreRideGate.endExport();
  ok(RestoreRideGate.beginRideStart(),"start lock recovers");
  RestoreRideGate.endRideStart();
  ok(OdoAutoSyncPolicy.first(1000,1002.9),"first reading within confirmed margin");
  ok(!OdoAutoSyncPolicy.first(1000,1010),"stale baseline needs explicit recheck");
  ok(OdoAutoSyncPolicy.first(1010,1010.1),"rechecked baseline can auto sync normally");
  ok(!OdoAutoSyncPolicy.next(1020,1010,60_000),"never lower garage odometer");
  ok(!OdoAutoSyncPolicy.next(1020,2020,60_000),"never accept implausible jump");
  System.out.println("PASS UX105 "+checks+" GPS start/atomic save exclusion and ODO baseline safety");
 }
}
