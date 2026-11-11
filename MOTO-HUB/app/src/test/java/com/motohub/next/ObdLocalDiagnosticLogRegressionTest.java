package com.motohub.next;

import android.content.Context;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=35)
public class ObdLocalDiagnosticLogRegressionTest {
 private Context context;
 @Before public void setup(){
  context=RuntimeEnvironment.getApplication();
  ObdLocalDiagnosticLog.clear(context);
 }
 @Test public void automaticSessionUsesRealLineBreaks(){
  ObdLocalDiagnosticLog.automaticSession(context,2,1,1,3,14,"NO DATA");
  String summary=ObdLocalDiagnosticLog.summary(context);
  assertTrue(summary.contains("\n\n마지막 OBD 자동 연결: "));
  assertTrue(summary.contains("\n연결 시도 2회"));
  assertTrue(summary.contains("\n속도 응답 14회"));
  assertFalse(summary.contains("\\n연결 시도"));
 }
 @Test public void invalidLegacyRecordsDoNotConsumeRingCapacity(){
  StringBuilder old=new StringBuilder();
  for(int i=0;i<30;i++)old.append("untrusted raw bluetooth AA:BB:CC:DD:EE:FF\n");
  old.append("11-10 09:10 · 정상 연결");
  context.getSharedPreferences("obd_diagnostics_local",0).edit()
   .putString("events",old.toString()).commit();
  ObdLocalDiagnosticLog.record(context,"PID");
  String events=context.getSharedPreferences("obd_diagnostics_local",0).getString("events","");
  assertFalse(events.contains("AA:BB"));
  assertTrue(events.contains("11-10 09:10 · 정상 연결"));
 }
}