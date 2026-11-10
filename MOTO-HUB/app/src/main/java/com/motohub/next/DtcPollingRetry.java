package com.motohub.next;

/**
 * An optional PID must recover after temporary adapter/ECU timeouts.
 * This backoff never stops the primary speed-based GPS ride session.
 */
final class DtcPollingRetry {
 static final int FAILURE_THRESHOLD=3;
 static final long COOLDOWN_MS=60_000L;
 private DtcPollingRetry(){}
 static long delayMillis(int consecutiveFailures){
  return consecutiveFailures>=FAILURE_THRESHOLD?COOLDOWN_MS:0L;
 }
 static boolean due(long nowElapsedNanos,long allowedAfterNanos){
  return allowedAfterNanos==0L||nowElapsedNanos-allowedAfterNanos>=0L;
 }
}