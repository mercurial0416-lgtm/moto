package com.motohub.next;
/** Read-only speed polling policy. Invalid replies trigger bounded reconnect, not permanent PID rejection. */
final class ObdSpeedResponsePolicy {
 static final int MAX_INVALID_SAMPLES=6;
 static boolean unsupported(ObdPidCapabilities.Support support){
  return support==ObdPidCapabilities.Support.NO;
 }
 static boolean retryConnection(int invalidSamples,ObdPidCapabilities.Support support){
  return invalidSamples>=MAX_INVALID_SAMPLES && !unsupported(support);
 }
 static long pollDelayMs(int speed){
  // A slower stationary poll saves radio/CPU work; moving samples stay responsive.
  return speed==0?4_000L:1_100L;
 }
 private ObdSpeedResponsePolicy(){}
}
