package com.motohub.next;

import org.junit.Test;
import static org.junit.Assert.*;

public class RecentRecordReuseTest {
 @Test public void prefersLatestDateRatherThanOldestInsertion(){
  assertTrue(RecentRecordReuse.isNewer("2026-10-18",5000,"2026-08-01",8000));
  assertFalse(RecentRecordReuse.isNewer("2026-08-01",8000,"2026-10-18",5000));
 }
 @Test public void tieBreaksByOdometer(){
  assertTrue(RecentRecordReuse.isNewer("2026-10-18",5100,"2026-10-18",5000));
  assertFalse(RecentRecordReuse.isNewer("2026-10-18",4900,"2026-10-18",5000));
  assertFalse(RecentRecordReuse.isNewer("2026-10-18",5000,"2026-10-18",5000));
 }
 @Test public void rollsForwardIntervalNotStaleAbsoluteTarget(){
  assertEquals(7200,RecentRecordReuse.nextServiceKm(3000,5000,5200));
 }
 @Test public void doesNotInventIntervalForMissingOrOverdueTarget(){
  assertEquals(0,RecentRecordReuse.nextServiceKm(5000,0,6000));
  assertEquals(0,RecentRecordReuse.nextServiceKm(5000,4000,6000));
  assertEquals(0,RecentRecordReuse.nextServiceKm(5000,5000,6000));
 }
 @Test public void rejectsCorruptMileage(){
  assertEquals(0,RecentRecordReuse.nextServiceKm(Double.NaN,6000,7000));
  assertEquals(0,RecentRecordReuse.nextServiceKm(5000,6000,Double.POSITIVE_INFINITY));
  assertEquals(0,RecentRecordReuse.nextServiceKm(5000,6000,-1));
  assertEquals(0,RecentRecordReuse.nextServiceKm(5000,6000,1e13));
 }
 @Test public void fractionalMileageRoundsTargetUp(){
  assertEquals(7201,RecentRecordReuse.nextServiceKm(3000,5000,5200.1));
 }
}
