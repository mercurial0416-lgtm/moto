package com.motohub.next;
import org.junit.Test;
import static org.junit.Assert.*;
public final class RideTimePolicyTest {
 @Test public void classifyGoodFixes(){
  assertTrue(RideTimePolicy.eligible(5_000,0.8,4,5));
  assertTrue(RideTimePolicy.eligible(5_000,20,4,5));
  assertTrue(RideTimePolicy.moving(20,5_000));
  assertFalse(RideTimePolicy.moving(1,5_000));
  assertEquals(40.0,RideTimePolicy.movingAverage(2.0,180_000),0.001);
 }
 @Test public void neverBridgeOutagesOrFakeSpeed(){
  assertFalse(RideTimePolicy.eligible(RideMotionGate.MAX_CONTINUOUS_FIX_MS+1,30,5,5));
  assertFalse(RideTimePolicy.eligible(200,900,5,5));
  assertFalse(RideTimePolicy.eligible(5_000,20,28,5));
  assertFalse(RideTimePolicy.eligible(5_000,Double.NaN,5,5));
  assertFalse(RideTimePolicy.eligible(0,1,1,1));
  assertEquals(0,RideTimePolicy.movingAverage(3.0,0),0.01);
 }
}
