package com.motohub.next;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import android.content.Context;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=35)
public final class Ux122ObdSessionSummaryTest {
 @Test public void completedAutoSessionShowsCountersButNeverAdapterIdentifiers(){
  Context c=RuntimeEnvironment.getApplication();
  ObdLocalDiagnosticLog.clear(c);
  ObdLocalDiagnosticLog.automaticSession(c,5,3,2,9,48,"Bluetooth failed at 00:11:22:33:44:55");
  String summary=ObdLocalDiagnosticLog.summary(c);
  assertTrue(summary.contains("연결 시도 5회"));
  assertTrue(summary.contains("재시도 2회"));
  assertTrue(summary.contains("속도 응답 48회"));
  assertTrue(summary.contains("쿼리 오류 9회"));
  assertFalse(summary.contains("00:11:22:33:44:55"));
  assertFalse(summary.contains("MOTO-HUB-FIXED"));
 }
}
