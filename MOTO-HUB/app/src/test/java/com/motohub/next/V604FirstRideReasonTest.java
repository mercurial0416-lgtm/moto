package com.motohub.next;
import org.junit.Test;
import static org.junit.Assert.*;
public class V604FirstRideReasonTest {
 @Test public void genuineFailureReasonsAreDistinct(){
  assertTrue(FirstRideSetupProof.attemptReason(null,false,12,2).contains("저장 검증 실패"));
  assertTrue(FirstRideSetupProof.attemptReason(new java.io.File("missing.track"),true,12,2).contains("저장 검증 실패"));
 }
 @Test public void eventLabelsContainNoRawDeviceOrLocationData(){
  for(ConnectionEventLog.Event event:ConnectionEventLog.Event.values()){
   assertFalse(event.text.contains("00:00:"));
   assertFalse(event.text.contains("37.0,"));
  }
 }
}
