package com.motohub.next;
import org.junit.Test;
import static org.junit.Assert.*;
public final class MapPathContinuityTest{
 @Test public void breakOnBadAccuracyJumpOrOutage(){
  assertTrue(MapPathContinuity.validLink(37,127,5,1000,37.0001,127.0001,5,6000));
  assertFalse(MapPathContinuity.validLink(37,127,5,1000,38,127,5,6000));
  assertFalse(MapPathContinuity.validLink(37,127,55,1000,37.001,127,5,6000));
  assertFalse(MapPathContinuity.validLink(37,127,5,1000,37.001,127,5,32000));
  assertFalse(MapPathContinuity.validLink(37,127,5,1000,37.001,127,5,1000));
 }
}
