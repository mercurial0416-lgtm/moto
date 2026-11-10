package com.motohub.next;

import static org.junit.Assert.*;
import org.junit.Test;

public final class ObdPersistentStandbyTest {
 @Test public void ordinarySessionStopsAfterOriginalEightAttempts(){
  assertTrue(ObdConnectionPolicy.shouldRetry(8,true,true));
  assertFalse(ObdConnectionPolicy.shouldRetry(9,true,true));
 }
 @Test public void userOptedInStandbyUsesOneMinuteCooldown(){
  assertTrue(ObdConnectionPolicy.standbyAllowed(true,false));
  assertFalse(ObdConnectionPolicy.standbyAllowed(false,false));
  assertFalse(ObdConnectionPolicy.standbyAllowed(true,true));
  assertEquals(1500L,ObdConnectionPolicy.standbyDelayMs(1));
  assertEquals(20_000L,ObdConnectionPolicy.standbyDelayMs(8));
  assertEquals(60_000L,ObdConnectionPolicy.standbyDelayMs(9));
  assertEquals(60_000L,ObdConnectionPolicy.standbyDelayMs(150));
 }
 @Test(expected=IllegalArgumentException.class)
 public void noZeroOrNegativeFailureDelay(){
  ObdConnectionPolicy.standbyDelayMs(0);
 }
}