package com.motohub.next;

/** Cheap, purely local decisions for an explicitly registered Cardo trigger. */
final class CardoAutoRidePolicy {
 static final long START_DEBOUNCE_MS=90_000L;
 private CardoAutoRidePolicy(){}
 static boolean trigger(boolean enabled,boolean addressMatched,boolean rideActive,long last,long now){
  if(!enabled||!addressMatched||rideActive||now<=0)return false;
  return last<=0||now<last||now-last>=START_DEBOUNCE_MS;
 }
 static boolean shouldStopWatcher(boolean cardoSession,boolean running,boolean cardoDisconnected){
  return cardoSession&&!running&&cardoDisconnected;
 }
}