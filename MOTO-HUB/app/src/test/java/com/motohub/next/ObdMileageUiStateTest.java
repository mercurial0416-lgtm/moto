package com.motohub.next;

import org.junit.Test;
import static org.junit.Assert.*;

/** A display-priority test; no ECU or garage odometer writes are implied. */
public class ObdMileageUiStateTest {
 @Test public void actualEcuWinsWithoutMixingGpsDistance(){
  assertEquals(ObdMileageUiState.Source.ECU_VERIFIED,
    ObdMileageUiState.choose(true,true,true));
 }
 @Test public void dtcCounterEstimateBeatsGpsWhenEcuMissing(){
  assertEquals(ObdMileageUiState.Source.PID_0131_ESTIMATE,
    ObdMileageUiState.choose(false,true,true));
 }
 @Test public void counterAgeDoesNotMasqueradeAsLiveData(){
  long now=1_300_000L;
  assertFalse(ObdMileageUiState.recentCounter(0L,now));
  assertTrue(ObdMileageUiState.recentCounter(100_000L,1_300_000L));
  assertFalse(ObdMileageUiState.recentCounter(99_999L,1_300_000L));
  assertFalse(ObdMileageUiState.recentCounter(1_300_001L,now));
 }
 @Test public void gpsIsLastResortAndAllMissingIsClear(){
  assertEquals(ObdMileageUiState.Source.GPS_ESTIMATE,
    ObdMileageUiState.choose(false,false,true));
  assertEquals(ObdMileageUiState.Source.UNAVAILABLE,
    ObdMileageUiState.choose(false,false,false));
 }
}
