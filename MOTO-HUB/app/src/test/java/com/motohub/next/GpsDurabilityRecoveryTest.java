package com.motohub.next;
import org.junit.Test;import static org.junit.Assert.*;
public class GpsDurabilityRecoveryTest {
 @Test public void failedSyncKeepsDirtyState(){
  GpsSyncPolicy p=new GpsSyncPolicy();
  assertTrue(p.shouldSync(100));assertTrue(p.hasDirty());
  assertTrue(p.shouldSync(200));assertTrue(p.hasDirty());
  p.synced(200);assertFalse(p.hasDirty());
  assertFalse(p.shouldSync(400));assertTrue(p.hasDirty());
  assertTrue(p.shouldSync(2401));p.synced(2401);assertFalse(p.hasDirty());
 }
 @Test public void rejectVeryOldFixAtRideStart(){
  long now=1_800_000_000_000L;
  assertTrue(GpsFixPolicy.acceptTime(now-300000,now,0));
  assertFalse(GpsFixPolicy.acceptTime(now-300001,now,0));
  assertFalse(GpsFixPolicy.acceptTime(now-86400000,now,0));
 }
}