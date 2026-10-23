package com.motohub.next;

/** Avoid conflating an OBD2 arming session with a consented active GPS track. */
final class GpsProviderLossPolicy {
 enum Action { IGNORE, DISARM_WAITING, KEEP_ACTIVE_TRACK }
 private GpsProviderLossPolicy(){}
 static Action onLoss(boolean recording,boolean autoArmed){
  if(!autoArmed)return Action.IGNORE;
  return recording?Action.KEEP_ACTIVE_TRACK:Action.DISARM_WAITING;
 }
}
