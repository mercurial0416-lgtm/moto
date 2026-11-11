package com.motohub.next;
import org.junit.Test;
import static org.junit.Assert.*;
public class V604PermissionDecisionTest {
 @Test public void bluetoothPermissionWinsOverPowerState(){
  assertEquals(ConnectionIssueGuide.Issue.BT_PERMISSION,
   ConnectionIssueGuide.diagnose(ConnectionSetupMode.Mode.CARDO_OBD,true,true,
    false,false,false,true,true,true,false,false,false,false,"",false));
 }
 @Test public void CardoBackgroundLocationIsRequired(){
  assertEquals(ConnectionIssueGuide.Issue.BACKGROUND_LOCATION,
   ConnectionIssueGuide.diagnose(ConnectionSetupMode.Mode.CARDO_GPS,false,true,
    true,true,true,true,true,true,false,false,false,false,"",true,true,false));
 }
 @Test public void ObdOnlyDoesNotNeedBackgroundLocation(){
  assertNotEquals(ConnectionIssueGuide.Issue.BACKGROUND_LOCATION,
   ConnectionIssueGuide.diagnose(ConnectionSetupMode.Mode.OBD_ONLY,true,false,
    false,true,true,true,true,true,false,false,false,false,"",false,false,false));
 }
}
