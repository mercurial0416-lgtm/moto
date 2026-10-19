package com.motohub.next;

/** Throttles routine GPS notification refreshes, not location capture or safety alerts. */
final class RideNotificationRate {
 static final long MIN_INTERVAL_MS=10_000L;
 private long last=-1;
 boolean shouldNotify(long elapsedMs){
  if(elapsedMs<0)return false;
  if(last<0||elapsedMs<last||elapsedMs-last>=MIN_INTERVAL_MS){last=elapsedMs;return true;}
  return false;
 }
 void reset(){last=-1;}
}
