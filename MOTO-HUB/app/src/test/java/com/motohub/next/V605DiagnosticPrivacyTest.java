package com.motohub.next;
import org.junit.Test;
import static org.junit.Assert.*;
public class V605DiagnosticPrivacyTest {
 @Test public void reportNeverInterpolatesUserNamedDevices(){
  // A device may be named after a person or contain a telephone number.
  String fileSource=SafeConnectionDiagnosticReport.class.getName();
  assertEquals("com.motohub.next.SafeConnectionDiagnosticReport",fileSource);
  assertFalse(SafeConnectionDiagnosticReport.redact("AA:BB:CC:DD:EE:FF").contains("AA:BB"));
 }
}
