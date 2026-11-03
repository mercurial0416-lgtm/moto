package com.motohub.next;

import org.junit.Test;
import static org.junit.Assert.*;
import java.time.YearMonth;
import java.time.ZoneId;

/** Pure regression checks: read-only GPS statistics, no ECU or user data edits. */
public final class Ux111RideReplayCostTest {
 @Test public void costPerKmNeverUsesAllVehicleNumerator(){
  assertTrue(RideCostPerKm.caption("",400000,120,0).contains("바이크를 선택"));
  assertTrue(RideCostPerKm.caption("bike-1",30000,100,0).contains("300원/GPS km"));
 }
 @Test public void incompleteGpsMustNotPretendToBeAccurateCostPerKm(){
  assertTrue(RideCostPerKm.caption("bike-1",25000,50,1).contains("생략"));
  assertTrue(RideCostPerKm.caption("bike-1",25000,0,0).contains("1km 이상"));
  assertTrue(RideCostPerKm.caption("bike-1",25000,4,0).contains("실제 km당 유지비"));
  assertTrue(RideCostPerKm.caption("bike-1",0,4,0).contains("표시하지 않습니다"));
 }
 @Test public void nonFiniteOrHugeCostRateIsRejected(){
  assertTrue(RideCostPerKm.caption("bike",Double.NaN,30,0).contains("확인할 수"));
  assertTrue(RideCostPerKm.caption("bike",1_000_000_000.0,0.1,0).contains("1km 이상"));
  assertTrue(RideCostPerKm.caption("bike",1_000_000_000.0,1.0,0).contains("원/GPS km"));
  assertTrue(RideCostPerKm.caption("bike",1_000_000_000_000.0,1.0,0).contains("범위를 초과"));
 }
 @Test public void sixMonthMinutesStayBoundToRecordedRideStart(){
  YearMonth anchor=YearMonth.of(2026,11);
  RideStatsSeries series=new RideStatsSeries(anchor);
  long start=anchor.atDay(2).atStartOfDay(ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli();
  assertTrue(series.add(start,12.5,90L*60_000));
  assertEquals(90,series.totalMinutes());
  assertEquals(1,series.rides());
  assertEquals(12.5,series.totalKm,0.0001);
  assertFalse(series.add(start,Double.POSITIVE_INFINITY,30000));
  assertEquals(90,series.totalMinutes());
 }
}
