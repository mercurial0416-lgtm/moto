package com.motohub.next;
import org.junit.Test;
import static org.junit.Assert.*;
public class V606MissingRideReasonTest {
 @Test public void missingRegistrationDoesNotClaimSuccess(){
  assertTrue(RideMissingRecordingGuide.probableReason(
   ConnectionSetupMode.Mode.CARDO_OBD,false,false,false,false,false).contains("OBD"));
  assertTrue(RideMissingRecordingGuide.probableReason(
   ConnectionSetupMode.Mode.CARDO_OBD,true,false,false,false,false).contains("카르도"));
 }
 @Test public void speedWithoutGpsFallbackIsClearlyExplained(){
  String reason=RideMissingRecordingGuide.probableReason(
   ConnectionSetupMode.Mode.CARDO_OBD,true,true,false,false,false);
  assertTrue(reason.contains("010D"));
  assertTrue(reason.contains("GPS"));
 }
 @Test public void obdOnlyDisabledStandbyIsNotReady(){
  String reason=RideMissingRecordingGuide.probableReason(
   ConnectionSetupMode.Mode.OBD_ONLY,true,false,false,false,false);
  assertTrue(reason.contains("상시 대기"));
 }
}
