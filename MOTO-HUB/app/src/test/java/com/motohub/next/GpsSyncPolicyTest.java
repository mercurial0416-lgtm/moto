package com.motohub.next;
import org.junit.Test;
import static org.junit.Assert.*;
public final class GpsSyncPolicyTest {
 @Test public void neverWaitMoreThanTwoSecondsOrFiveAcceptedPoints(){
  GpsSyncPolicy policy=new GpsSyncPolicy();
  assertTrue(policy.shouldSync(100));
  assertFalse(policy.shouldSync(200));
  assertFalse(policy.shouldSync(500));
  assertFalse(policy.shouldSync(1500));
  assertFalse(policy.shouldSync(1800));
  assertTrue(policy.shouldSync(1900)); // 5 fixes since last sync
  assertFalse(policy.shouldSync(2000));
  assertTrue(policy.shouldSync(3900));
 }
 @Test public void recoveryAndClockReset(){
  GpsSyncPolicy p=new GpsSyncPolicy();
  assertTrue(p.shouldSync(2000));
  assertTrue(p.shouldSync(1990));
  p.reset();
  assertTrue(p.shouldSync(5000));
 }
}
