package com.motohub.next;

import static org.junit.Assert.*;
import org.junit.Test;

/** Anchored estimate is derived only from verified bike-bound GPS records. */
public class HybridOdometerEstimateTest {
 @Test public void usesDashboardBaselinePlusCompletedRides(){
  HybridOdometerEstimate.Reading r=HybridOdometerEstimate.calculate(1210.0,81.0,123.25,0,100);
  assertTrue(r.configured);assertTrue(r.valid);assertFalse(r.ledgerChanged);
  assertEquals(1252.25,r.estimatedKm,0.0001);
  assertEquals(42.25,r.deltaKm,0.0001);
 }
 @Test public void addsLiveRideOnlyUntilRecorded(){
  HybridOdometerEstimate.Reading running=HybridOdometerEstimate.calculate(1210,81,100,8.5,100);
  HybridOdometerEstimate.Reading finished=HybridOdometerEstimate.calculate(1210,81,108.5,0,100);
  assertEquals(running.estimatedKm,finished.estimatedKm,0.0001);
 }
 @Test public void resetsAnchorAfterCalibrationWithoutDuplicate(){
  HybridOdometerEstimate.Reading later=HybridOdometerEstimate.calculate(1395.5,130.3,131.3,0,222);
  assertEquals(1396.5,later.estimatedKm,0.00001);
  assertEquals(1.0,later.deltaKm,0.00001);
 }
 @Test public void gpsRemovalOrRestoreFailsClosed(){
  HybridOdometerEstimate.Reading r=HybridOdometerEstimate.calculate(1250,120,100,1,100);
  assertTrue(r.configured);assertFalse(r.valid);assertTrue(r.ledgerChanged);
  assertTrue(Double.isNaN(r.estimatedKm));
 }
 @Test public void invalidInputsNeverGenerateFakeMileage(){
  assertFalse(HybridOdometerEstimate.calculate(50,10,Double.NaN,0,100).valid);
  assertFalse(HybridOdometerEstimate.calculate(50,10,20,-1,100).valid);
  assertFalse(HybridOdometerEstimate.calculate(2_000_000,10,50,0,100).valid);
  assertFalse(HybridOdometerEstimate.calculate(50,10,20,Double.POSITIVE_INFINITY,100).valid);
 }
}
