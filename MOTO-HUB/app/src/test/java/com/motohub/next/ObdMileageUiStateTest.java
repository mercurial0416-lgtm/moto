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
 @Test public void gpsIsLastResortAndAllMissingIsClear(){
  assertEquals(ObdMileageUiState.Source.GPS_ESTIMATE,
    ObdMileageUiState.choose(false,false,true));
  assertEquals(ObdMileageUiState.Source.UNAVAILABLE,
    ObdMileageUiState.choose(false,false,false));
 }
}
