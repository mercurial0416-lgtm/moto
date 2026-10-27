package com.motohub.next;
import org.junit.Test;
import static org.junit.Assert.*;
public final class RideMapOverlayPolicyTest {
 @Test public void noFakePeakMarkers(){
  assertFalse(RideMapOverlayPolicy.validPeak(0,37.0,127.0));
  assertFalse(RideMapOverlayPolicy.validPeak(Double.NaN,37.0,127.0));
  assertFalse(RideMapOverlayPolicy.validPeak(300,37.0,127.0));
  assertFalse(RideMapOverlayPolicy.validPeak(90,Double.NaN,127.0));
  assertTrue(RideMapOverlayPolicy.validPeak(90,37.0,127.0));
 }
}
