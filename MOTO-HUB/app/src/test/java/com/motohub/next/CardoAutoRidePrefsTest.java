package com.motohub.next;
import android.content.Context;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35)
public class CardoAutoRidePrefsTest {
 private static final String A="01234567-89ab-4cde-8fab-0123456789ab";
 private static final String B="01234567-89ab-4cde-8fab-1123456789ab";
 private static final String OBD="11:22:33:44:55:66";
 private static final String CARDO="AA:BB:CC:DD:EE:FF";
 Context c;
 @Before public void setup(){
  c=RuntimeEnvironment.getApplication();
  c.getSharedPreferences("cardo_autoride_v595",0).edit().clear().commit();
  BikeObdPreferences.clearBike(c,A);BikeObdPreferences.clearBike(c,B);
 }
 @Test public void cannotEnableWithoutRegisteredBikeObd(){
  assertFalse(CardoAutoRidePrefs.enable(c,A,CARDO,"Freecom 2X"));
  assertFalse(CardoAutoRidePrefs.enabled(c));
 }
 @Test public void exactAddressMatchAndBikeIsolation(){
  BikeObdPreferences.setDevice(c,A,OBD,"iCar Pro BT3");
  assertFalse(CardoAutoRidePrefs.enable(c,A,OBD,"bad same adapter"));
  assertTrue(CardoAutoRidePrefs.enable(c,A,CARDO,"Cardo FREECOM 2X"));
  assertTrue(CardoAutoRidePrefs.forBike(c,A));
  assertFalse(CardoAutoRidePrefs.forBike(c,B));
  assertTrue(CardoAutoRidePrefs.matches(c,"aa:bb:cc:dd:ee:ff"));
  assertFalse(CardoAutoRidePrefs.matches(c,OBD));
  assertFalse(CardoAutoRidePrefs.matches(c,"11:22:33:44:55:67"));
 }
 @Test public void cardoModeDisablesOldOvernightPolling(){
  BikeObdPreferences.setDevice(c,A,OBD,"iCar Pro");
  BikeObdPreferences.setAutoRideEnabled(c,A,true);
  assertTrue(CardoAutoRidePrefs.enable(c,A,CARDO,"Freecom"));
  assertFalse(BikeObdPreferences.autoRideEnabled(c,A));
 }
 @Test public void disablingIsScopedAndDoesNotTouchGarageOdo(){
  BikeObdPreferences.setDevice(c,A,OBD,"iCar Pro");
  assertTrue(CardoAutoRidePrefs.enable(c,A,CARDO,"Freecom"));
  CardoAutoRidePrefs.disable(c);
  assertFalse(CardoAutoRidePrefs.enabled(c));
  assertEquals(OBD,BikeObdPreferences.address(c,A));
 }
}