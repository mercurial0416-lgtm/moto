package com.motohub.next;
import org.junit.Test;
import static org.junit.Assert.*;
public final class RideDiagnosticsTest {
 @Test public void showLocalChecksAndNoCoordinates(){
  String s=RideDiagnostics.summarize(true,false,true,false,false,"미연결",true);
  assertTrue(s.contains("GPS"));
  assertTrue(s.contains("배터리"));
  assertTrue(s.contains("OBD"));
  assertTrue(s.contains("백업")||s.contains("앱 데이터"));
  assertFalse(s.contains("35.135"));
 }
}
