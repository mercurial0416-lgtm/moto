package com.motohub.next;

/** Bound, foreground-session-only retries. No device scan or boot-time reconnect. */
final class ObdConnectionPolicy {
 static final int MAX_RETRIES=8;
 static long delayMs(int retry){
  if(retry<1||retry>MAX_RETRIES)throw new IllegalArgumentException("invalid retry");
  return Math.min(20_000L, 1500L*(1L<<(retry-1)));
 }
 // A long *completed* healthy connection restores the retry budget once.  A
 // failed reconnect is not another healthy connection and must not reset it.
 static int nextFailureCount(int failures,long connectedAtNanos,long nowNanos){
  boolean healthy=connectedAtNanos>0 && nowNanos>=connectedAtNanos
   && nowNanos-connectedAtNanos>=60_000_000_000L;
  return (healthy?0:failures)+1;
 }
 static boolean shouldRetry(int failures,boolean armed,boolean supported){
  return armed&&supported&&failures<=MAX_RETRIES;
 }
 private ObdConnectionPolicy(){}
}
