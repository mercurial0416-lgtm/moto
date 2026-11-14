package com.motohub.next;

/** Optional battery-saving mode only while parked. Trades faster ignition detection
 * for fewer long-term Bluetooth connection attempts. An active GPS ride is unaffected. */
final class ObdPowerModePolicy {
 private ObdPowerModePolicy(){}
 static long delayMs(int failureRounds,boolean powerSaving,boolean recording){
  long normal=ObdHandsFreePolicy.reconnectCooldownMs(failureRounds);
  if(!powerSaving||recording)return normal;
  if(failureRounds>=8)return 900_000L;
  if(failureRounds>=4)return 600_000L;
  return normal;
 }
}
