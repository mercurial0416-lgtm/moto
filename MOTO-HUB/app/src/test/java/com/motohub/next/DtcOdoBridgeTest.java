package com.motohub.next;

import android.content.Context;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=35)
public class DtcOdoBridgeTest {
 static final String BIKE="01234567-89ab-4cde-8fab-0123456789ab";
 static final String OTHER="01234567-89ab-4cde-8fab-1123456789ab";
 Context c;
 @Before public void setup(){
  c=RuntimeEnvironment.getApplication();
  c.getSharedPreferences("obd_since_clear_odo_v590",0).edit().clear().commit();
  RideService.running=false;RideService.autoArmed=false;
 }
 @Test public void optInBaselineAndChangeAccumulateAcrossDisconnects(){
  assertFalse(DtcOdoBridge.enabled(c,BIKE));
  assertFalse(DtcOdoBridge.offer(c,BIKE,45,System.currentTimeMillis()));
  assertTrue(DtcOdoBridge.calibrate(c,BIKE,1200.0,44));
  assertEquals(1200.0,DtcOdoBridge.snapshot(c,BIKE).estimatedKm,0.001);
  long now=System.currentTimeMillis();
  assertTrue(DtcOdoBridge.offer(c,BIKE,50,now+120000L));
  assertEquals(1206.0,DtcOdoBridge.snapshot(c,BIKE).estimatedKm,0.001);
  // A remote garage selection cannot mix data into a different bike.
  assertFalse(DtcOdoBridge.offer(c,OTHER,222,now+125000L));
  assertFalse(DtcOdoBridge.enabled(c,OTHER));
 }
 @Test public void aResetQuarantinesCounterAndNeverSubtractsDistance(){
  assertTrue(DtcOdoBridge.calibrate(c,BIKE,1200,44));
  long now=System.currentTimeMillis();
  assertFalse(DtcOdoBridge.offer(c,BIKE,1,now+20000L));
  DtcOdoBridge.Reading r=DtcOdoBridge.snapshot(c,BIKE);
  assertTrue(r.paused);assertFalse(r.valid);
  assertFalse(DtcOdoBridge.offer(c,BIKE,2,now+40000L));
  assertTrue(DtcOdoBridge.calibrate(c,BIKE,1210,2));
  assertTrue(DtcOdoBridge.snapshot(c,BIKE).valid);
 }
 @Test public void saturatingCounterStopsInsteadOfWrapping(){
  assertFalse(DtcOdoBridge.calibrate(c,BIKE,1000,65535));
  assertTrue(DtcOdoBridge.calibrate(c,BIKE,1000,65534));
  assertFalse(DtcOdoBridge.offer(c,BIKE,65535,System.currentTimeMillis()+100000));
  assertTrue(DtcOdoBridge.snapshot(c,BIKE).paused);
 }
 @Test public void corruptJumpRejectedWithoutAlteringLastEstimate(){
  assertTrue(DtcOdoBridge.calibrate(c,BIKE,1111,55));
  assertFalse(DtcOdoBridge.offer(c,BIKE,5000,System.currentTimeMillis()+1000));
  assertEquals(1111,DtcOdoBridge.snapshot(c,BIKE).estimatedKm,0.001);
 }
 @Test public void deletingBikePurgesOnlyItsOwnDtcCalibration(){
  assertTrue(DtcOdoBridge.calibrate(c,BIKE,1200,44));
  assertTrue(DtcOdoBridge.calibrate(c,OTHER,2000,80));
  assertTrue(DtcOdoBridge.clearBike(c,BIKE));
  assertFalse(DtcOdoBridge.enabled(c,BIKE));
  assertFalse(DtcOdoBridge.snapshot(c,BIKE).valid);
  assertTrue(DtcOdoBridge.enabled(c,OTHER));
  assertTrue(DtcOdoBridge.snapshot(c,OTHER).valid);
 }
 @Test public void backupRestoreClearsAnchors(){
  assertTrue(DtcOdoBridge.calibrate(c,BIKE,1200,44));
  assertTrue(DtcOdoBridge.suspendAfterRestore(c));
  assertFalse(DtcOdoBridge.enabled(c,BIKE));
 }
}