package com.motohub.next;

/** Admission policy for an ECU-speed-triggered GPS file.
 * Never announce an auto ride as recording while location is disabled. */
final class ObdAutoStartPreflight {
 enum Decision { ALLOW, WAIT_GPS, SKIP }
 private ObdAutoStartPreflight(){}
 static Decision decide(boolean armed,boolean recording,boolean finishing,boolean gpsEnabled){
  if(!armed||recording||finishing)return Decision.SKIP;
  return gpsEnabled?Decision.ALLOW:Decision.WAIT_GPS;
 }
}
