package com.motohub.next;
import android.content.Context;
import org.junit.Before;import org.junit.Test;
import org.junit.runner.RunWith;import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=35)
public class CardoPresenceReliabilityTest {
 static final String BIKE="01234567-89ab-4cde-8fab-0123456789ab";
 static final String OBD="11:22:33:44:55:66",CARDO="AA:BB:CC:DD:EE:FF";
 Context c;
 @Before public void setup(){
  c=RuntimeEnvironment.getApplication();
  c.getSharedPreferences("cardo_autoride_v595",0).edit().clear().commit();
  BikeObdPreferences.clearBike(c,BIKE);BikeObdPreferences.setDevice(c,BIKE,OBD,"ELM");
  assertTrue(CardoAutoRidePrefs.enable(c,BIKE,CARDO,"Freecom 2X"));
 }
 @Test public void unrelatedBluetoothEventNeverFakesHelmetPresence(){
  CardoAutoRidePrefs.notePresence(c,OBD,true);
  assertFalse(CardoAutoRidePrefs.recentlyConnected(c));
 }
 @Test public void disconnectRevokesEarlierTapAndClaim(){
  CardoAutoRidePrefs.notePresence(c,CARDO,true);
  CardoAutoRidePrefs.notePresence(c,CARDO,false);
  assertFalse(CardoAutoRidePrefs.recentlyConnected(c));
  assertFalse(c.getSharedPreferences("cardo_autoride_v595",0).contains("triggerAt"));
 }
 @Test public void rebootOrStaleEventCannotAuthorizeNotification(){
  CardoAutoRidePrefs.notePresence(c,CARDO,true);
  c.getSharedPreferences("cardo_autoride_v595",0).edit()
   .putLong("presenceAt",Long.MAX_VALUE).commit();
  assertFalse(CardoAutoRidePrefs.recentlyConnected(c));
 }
 @Test public void foregroundStartFailureReleasesReservation(){
  c.getSharedPreferences("cardo_autoride_v595",0).edit().putLong("triggerAt",321L).commit();
  CardoAutoRidePrefs.releaseClaim(c,CARDO);
  assertFalse(c.getSharedPreferences("cardo_autoride_v595",0).contains("triggerAt"));
 }
}