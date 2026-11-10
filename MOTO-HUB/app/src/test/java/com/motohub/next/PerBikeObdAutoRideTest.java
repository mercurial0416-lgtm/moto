package com.motohub.next;

import android.content.Context;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import java.util.Arrays;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=35) // Robolectric 4.14.1 does not support the app's compile/target SDK 36.
public class PerBikeObdAutoRideTest {
 private static final String DUKE="9f87e196-787f-4dc2-b292-37f70f6f5f3d";
 private static final String OTHER="5dff9845-efae-4200-9078-843042d4bb7e";
 @Test public void adapterRequiresConsentAndNewAdapterRevokesOldConsent(){
  Context c=RuntimeEnvironment.getApplication();
  BikeObdPreferences.clearBike(c,DUKE);
  assertFalse(BikeObdPreferences.autoRideEnabled(c,DUKE));
  BikeObdPreferences.setDevice(c,DUKE,"AA:BB:CC:DD:EE:01","Vgate");
  BikeObdPreferences.setAutoRideEnabled(c,DUKE,true);
  assertTrue(BikeObdPreferences.autoRideEnabled(c,DUKE));
  BikeObdPreferences.setDevice(c,DUKE,"AA:BB:CC:DD:EE:01","Renamed");
  assertTrue(BikeObdPreferences.autoRideEnabled(c,DUKE));
  BikeObdPreferences.setDevice(c,DUKE,"AA:BB:CC:DD:EE:02","Other");
  assertFalse(BikeObdPreferences.autoRideEnabled(c,DUKE));
  BikeObdPreferences.clearDevice(c,DUKE);
  assertFalse(BikeObdPreferences.autoRideEnabled(c,DUKE));
  BikeObdPreferences.clearBike(c,DUKE);
 }
 @Test public void verifiedOdometerOffersRemainIndependentAndNeverShareAcrossBikes(){
  Context c=RuntimeEnvironment.getApplication();
  OdometerAutoSyncInbox.suspendAfterRestore(c);
  try{
   assertTrue(OdometerAutoSyncInbox.enable(c,DUKE,1000.0));
   assertTrue(OdometerAutoSyncInbox.enable(c,OTHER,2000.0));
   assertTrue(OdometerAutoSyncInbox.enabled(c,DUKE));
   assertTrue(OdometerAutoSyncInbox.enabled(c,OTHER));
   assertTrue(OdometerAutoSyncInbox.offer(c,DUKE,1000.2,1000.2,100000));
   assertTrue(OdometerAutoSyncInbox.offer(c,OTHER,2000.1,2000.1,100000));
   assertEquals(1000.2,OdometerAutoSyncInbox.pending(c,DUKE),0.001);
   assertEquals(2000.1,OdometerAutoSyncInbox.pending(c,OTHER),0.001);
   assertEquals(DUKE,OdometerAutoSyncInbox.configuredBikeId(c));
   assertFalse(OdometerAutoSyncInbox.acknowledged(c,DUKE,1000.0));
   assertEquals(1000.2,OdometerAutoSyncInbox.pending(c,DUKE),0.001);
   assertTrue(OdometerAutoSyncInbox.acknowledged(c,DUKE,1000.2));
   assertEquals(OTHER,OdometerAutoSyncInbox.configuredBikeId(c));
   OdometerAutoSyncInbox.disable(c,DUKE);
   assertFalse(OdometerAutoSyncInbox.enabled(c,DUKE));
   assertTrue(OdometerAutoSyncInbox.enabled(c,OTHER));
   OdometerAutoSyncInbox.clearBike(c,OTHER);
   assertFalse(OdometerAutoSyncInbox.enabled(c,OTHER));
  }finally{OdometerAutoSyncInbox.suspendAfterRestore(c);}
 }
 @Test public void legacyBaselineMigratesWithoutDeletingQueuedDistance(){
  Context c=RuntimeEnvironment.getApplication();
  OdometerAutoSyncInbox.suspendAfterRestore(c);
  c.getSharedPreferences("odo_auto_sync",Context.MODE_PRIVATE).edit()
   .putBoolean("enabled",true).putString("bikeId",DUKE)
   .putString("baseline","4512.0").putString("pending","4512.2")
   .putString("last","4512.2").putLong("lastAt",10000).commit();
  try{
   assertTrue(OdometerAutoSyncInbox.enabled(c,DUKE));
   assertEquals(4512.2,OdometerAutoSyncInbox.pending(c,DUKE),0.001);
   assertEquals("4512.0",c.getSharedPreferences("odo_auto_sync",Context.MODE_PRIVATE)
    .getString(DUKE+".baseline",""));
   assertFalse(c.getSharedPreferences("odo_auto_sync",Context.MODE_PRIVATE).contains("bikeId"));
  }finally{OdometerAutoSyncInbox.suspendAfterRestore(c);}
 }
}
