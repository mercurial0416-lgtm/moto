package com.motohub.next;
import org.junit.Test;
import static org.junit.Assert.*;
public class ConnectionSetupModeTest {
 @Test public void modesOnlyAskForRequiredDevices(){
  assertTrue(ConnectionSetupMode.Mode.CARDO_OBD.needsCardo());
  assertTrue(ConnectionSetupMode.Mode.CARDO_OBD.needsObd());
  assertFalse(ConnectionSetupMode.Mode.OBD_ONLY.needsCardo());
  assertTrue(ConnectionSetupMode.Mode.OBD_ONLY.needsObd());
  assertTrue(ConnectionSetupMode.Mode.CARDO_GPS.needsCardo());
  assertFalse(ConnectionSetupMode.Mode.CARDO_GPS.needsObd());
 }
}
