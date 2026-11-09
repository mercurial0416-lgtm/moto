package com.motohub.next;

/**
 * Prefer standard advertised ODO support. Some gateways omit the A0 bitmap,
 * so an opt-in, single read-only direct probe may run when safely stationary.
 * A failed unadvertised probe is never repeated until the next OBD session.
 */
final class ObdOdoPollPolicy {
 private ObdOdoPollPolicy(){}
 static boolean shouldProbe(boolean consented,ObdPidCapabilities.Support support,
                            int speedKmh,int speedPolls,boolean directProbeAttempted){
  if(!consented||speedKmh<0)return false;
  if(support==ObdPidCapabilities.Support.YES)
   return speedPolls>0&&speedPolls%60==0;
  return !directProbeAttempted&&speedKmh==0&&speedPolls>=8;
 }
}