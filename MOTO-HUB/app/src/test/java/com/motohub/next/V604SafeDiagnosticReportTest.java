package com.motohub.next;
import org.junit.Test;
import static org.junit.Assert.*;
public class V604SafeDiagnosticReportTest {
 @Test public void masksBluetoothAddressesAndCoordinates(){
  String x="OBD AA:BB:CC:DD:EE:FF GPS 37.123456,127.123456";
  String y=SafeConnectionDiagnosticReport.redact(x);
  assertFalse(y.contains("AA:BB:CC"));
  assertFalse(y.contains("37.123456"));
  assertFalse(y.contains("127.123456"));
  assertTrue(y.contains("[Bluetooth 주소 숨김]"));
 }
 @Test public void limitsExportLength(){
  String s="TEST".repeat(2000);
  assertTrue(SafeConnectionDiagnosticReport.redact(s).length()<=5040);
 }
}
