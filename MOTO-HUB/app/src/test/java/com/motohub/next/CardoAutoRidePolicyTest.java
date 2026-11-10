package com.motohub.next;
import org.junit.Test;
import static org.junit.Assert.*;
public class CardoAutoRidePolicyTest {
 @Test public void onlyAnExplicitMatchedHelmetCanStart(){
  assertTrue(CardoAutoRidePolicy.trigger(true,true,false,0,10000));
  assertFalse(CardoAutoRidePolicy.trigger(false,true,false,0,10000));
  assertFalse(CardoAutoRidePolicy.trigger(true,false,false,0,10000));
  assertFalse(CardoAutoRidePolicy.trigger(true,true,true,0,10000));
 }
 @Test public void duplicateAclAndCompanionSignalsAreDebounced(){
  assertFalse(CardoAutoRidePolicy.trigger(true,true,false,10000,10001));
  assertFalse(CardoAutoRidePolicy.trigger(true,true,false,10000,99999));
  assertTrue(CardoAutoRidePolicy.trigger(true,true,false,10000,100000));
  assertTrue(CardoAutoRidePolicy.trigger(true,true,false,999000,10)); // reboot elapsed reset
 }
 @Test public void disconnectDoesNotLoseActiveGpsRide(){
  assertTrue(CardoAutoRidePolicy.shouldStopWatcher(true,false,true));
  assertFalse(CardoAutoRidePolicy.shouldStopWatcher(true,true,true));
  assertFalse(CardoAutoRidePolicy.shouldStopWatcher(false,false,true));
 }
}