package com.motohub.next;

import org.junit.Test;
import static org.junit.Assert.*;

public class DtcPollingRetryTest {
 @Test public void optionalPidStopsSpammingAndRecoversAfterCooldown(){
  assertEquals(0L,DtcPollingRetry.delayMillis(0));
  assertEquals(0L,DtcPollingRetry.delayMillis(2));
  assertEquals(60_000L,DtcPollingRetry.delayMillis(3));
  assertEquals(60_000L,DtcPollingRetry.delayMillis(100));
  long start=1_000_000_000L;
  long after=start+DtcPollingRetry.COOLDOWN_MS*1_000_000L;
  assertTrue(DtcPollingRetry.due(start,0L));
  assertFalse(DtcPollingRetry.due(start,after));
  assertFalse(DtcPollingRetry.due(after-1L,after));
  assertTrue(DtcPollingRetry.due(after,after));
  assertTrue(DtcPollingRetry.due(after+1L,after));
 }
}