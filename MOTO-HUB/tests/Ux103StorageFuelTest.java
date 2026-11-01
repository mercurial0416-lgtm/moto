package com.motohub.next;

import java.io.IOException;

/** JVM tests for read-only fuel PID and atomic form-write transaction interlocks. */
public final class Ux103StorageFuelTest {
 static void ok(boolean condition,String label){if(!condition)throw new AssertionError(label);}
 public static void main(String[] args){
  String yes="41 20 00 02 00 00";
  ObdPidCapabilities good=ObdPidCapabilities.fromAllResponses(null,yes,null,null);
  ok(good.forPid("012F")==ObdPidCapabilities.Support.YES,"fuel PID bitmap positive");
  ok(good.forPid("010D")==ObdPidCapabilities.Support.UNKNOWN,"unrelated PID unknown");
  ok(good.forPid("012E")==ObdPidCapabilities.Support.NO,"unlisted 2E false");
  ok(ObdPidCapabilities.fromAllResponses(null,"41 20 00 00 00 00",null,null)
     .forPid("012F")==ObdPidCapabilities.Support.NO,"fuel unsupported no");
  ok(ObdPidCapabilities.fromAllResponses(null,null,null,null)
     .forPid("012F")==ObdPidCapabilities.Support.UNKNOWN,"missing bitmap unknown");
  ok(ObdPidCapabilities.fromAllResponses(null,yes+"\n41 20 00 00 00 00",null,null)
     .forPid("012F")==ObdPidCapabilities.Support.UNKNOWN,"conflicting bitmap fails closed");
  ok(ObdReadOnlyProtocol.query("012F").equals("012F\r"),"read-only fuel request allowed");
  boolean denied=false;
  try{ObdReadOnlyProtocol.query("042F");}catch(IllegalArgumentException expected){denied=true;}
  ok(denied,"no write-capable query");
  double percent=ObdSensorReading.number("012F","41 2F 80");
  ok(Math.abs(percent-128.0*100/255)<0.00001,"fuel percent formula");
  ok(ObdSensorReading.valid("012F",percent),"fuel percentage valid");
  ok(Double.isNaN(ObdSensorReading.number("012F","NO DATA")),"missing fuel response unknown");
  ok(Double.isNaN(ObdSensorReading.number("012F","41 2F 80\n41 2F FF")),"conflict must never show plausible fuel");
  ok(!ObdSensorReading.valid("012F",Double.NaN),"NaN never valid");

  ok(RestoreRideGate.beginFormWrite(),"form begins");
  ok(RestoreRideGate.isFormWriting(),"writing visible");
  ok(!RestoreRideGate.begin(false,false),"restore blocked while form commit");
  ok(!RestoreRideGate.beginExport(false,false),"ZIP blocked while form commit");
  ok(!RestoreRideGate.beginRideStart(),"new ride blocked while form commit");
  ok(!RestoreRideGate.beginFormWrite(),"duplicate form blocked");
  RestoreRideGate.endFormWrite();
  ok(!RestoreRideGate.isFormWriting(),"form unlock");
  ok(RestoreRideGate.begin(false,false),"restore may start after form");
  ok(!RestoreRideGate.beginFormWrite(),"form blocked while restore");
  RestoreRideGate.end();
  ok(RestoreRideGate.beginFormWrite(),"form may start after restore");
  RestoreRideGate.endFormWrite();
  System.out.println("PASS UX103 fuel PID capability/readback and serialized data mutations");
 }
}
