package com.motohub.next;
import org.junit.Test;
import static org.junit.Assert.*;
public class ConnectionIssueGuideTest {
 private ConnectionIssueGuide.Issue check(ConnectionSetupMode.Mode mode,boolean obd,boolean cardo,
  boolean fallback,boolean bt,boolean btPermission,boolean location,boolean alerts,boolean gps,
  boolean active,boolean running,boolean gpsFresh,boolean ecuFresh,String error,boolean seen){
  return ConnectionIssueGuide.diagnose(mode,obd,cardo,fallback,bt,btPermission,location,alerts,
    gps,active,running,gpsFresh,ecuFresh,error,seen);
 }
 @Test public void demandsOnlyChosenDevicesAndActualConsent(){
  assertEquals(ConnectionIssueGuide.Issue.MODE,check(ConnectionSetupMode.Mode.NONE,false,false,false,true,true,true,true,true,false,false,false,false,"",false));
  assertEquals(ConnectionIssueGuide.Issue.OBD_DEVICE,check(ConnectionSetupMode.Mode.CARDO_OBD,false,false,false,true,true,true,true,true,false,false,false,false,"",false));
  assertEquals(ConnectionIssueGuide.Issue.CARDO_DEVICE,check(ConnectionSetupMode.Mode.CARDO_GPS,false,false,false,true,true,true,true,true,false,false,false,false,"",false));
  assertEquals(ConnectionIssueGuide.Issue.GPS_CONSENT,check(ConnectionSetupMode.Mode.CARDO_GPS,false,true,false,true,true,true,true,true,false,false,false,false,"",true));
  assertEquals(ConnectionIssueGuide.Issue.LOCATION_PERMISSION,check(ConnectionSetupMode.Mode.CARDO_GPS,false,true,true,true,true,false,true,true,false,false,false,false,"",true));
 }
 @Test public void neverCallsRegistrationECUProof(){
  assertEquals(ConnectionIssueGuide.Issue.OBD_UNVERIFIED,check(ConnectionSetupMode.Mode.OBD_ONLY,true,false,false,true,true,true,true,true,true,false,false,false,"",false));
  assertEquals(ConnectionIssueGuide.Issue.GPS_NO_FIX,check(ConnectionSetupMode.Mode.CARDO_GPS,false,true,true,true,true,true,true,true,true,true,false,false,"",true));
  assertEquals(ConnectionIssueGuide.Issue.ECU_NO_REPLY,check(ConnectionSetupMode.Mode.OBD_ONLY,true,false,false,true,true,true,true,true,true,false,false,false,"OBD ECU 응답 실패",false));
 }
}
