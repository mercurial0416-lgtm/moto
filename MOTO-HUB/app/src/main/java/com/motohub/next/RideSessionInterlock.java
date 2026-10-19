package com.motohub.next;

/** Pure lifecycle policy: a control event must never tear down an active ride. */
final class RideSessionInterlock {
 private RideSessionInterlock() {}
 /** A running GPS track or armed OBD monitor owns the foreground service. */
 static boolean keepExistingSession(boolean gpsRunning,boolean obdArmed){
  return gpsRunning||obdArmed;
 }
}
