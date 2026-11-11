package com.motohub.next;
import org.junit.Test;
import static org.junit.Assert.*;
public class FirstRideSetupProofTest {
 @Test public void requiresVerifiedTrackAndMeasuredTravel(){
  assertFalse(FirstRideSetupProof.sufficient(false,40,2));
  assertFalse(FirstRideSetupProof.sufficient(true,1,2));
  assertFalse(FirstRideSetupProof.sufficient(true,20,0));
  assertFalse(FirstRideSetupProof.sufficient(true,20,.099));
  assertFalse(FirstRideSetupProof.sufficient(true,20,Double.NaN));
  assertTrue(FirstRideSetupProof.sufficient(true,20,.101));
 }
}
