package com.motohub.next;

import android.content.Context;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import static org.junit.Assert.*;

/** Guard device-local month selection and persistent, non-sensitive diagnostic summaries. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35)
public final class Ux116StatsZoneRestoreObdTest {
 @Test public void gpsMonthsUseTheSameDeviceLocalZoneAsRideHistory(){
  long at=Instant.parse("2026-06-30T15:30:00Z").toEpochMilli();
  ZoneId tokyo=ZoneId.of("Asia/Tokyo"),losAngeles=ZoneId.of("America/Los_Angeles");
  RideStatsSeries japan=new RideStatsSeries(YearMonth.of(2026,7),tokyo);
  RideStatsSeries california=new RideStatsSeries(YearMonth.of(2026,7),losAngeles);
  assertTrue(japan.add(at,12.5,300_000));
  assertTrue(california.add(at,12.5,300_000));
  assertEquals(1,japan.count[5]);
  assertEquals(1,california.count[4]);
  assertEquals(0,california.count[5]);
  assertEquals(0,japan.count[4]);
  assertTrue(japan.earliestStartMs()!=california.earliestStartMs());
  assertTrue(japan.inDisplayedPeriod(at,at+1000));
  assertTrue(california.inDisplayedPeriod(at,at+1000));
 }
 @Test public void restoreAuditPersistsOnlyStatusTimeAndCounts(){
  Context context=RuntimeEnvironment.getApplication();
  context.getSharedPreferences("full_restore_verification",0).edit().clear().commit();
  assertTrue(RestoreVerificationAudit.summary(context).contains("결과 없음"));
  assertTrue(RestoreVerificationAudit.record(context,true,23,5,7,2));
  String good=RestoreVerificationAudit.summary(context);
  assertTrue(good.contains("재검증 통과"));
  assertTrue(good.contains("파일 23개"));
  assertTrue(good.contains("GPS 5"));
  assertFalse(RestoreVerificationAudit.failed(context));
  assertTrue(RestoreVerificationAudit.record(context,false,5,2,1,1));
  assertTrue(RestoreVerificationAudit.failed(context));
  assertTrue(RestoreVerificationAudit.summary(context).contains("재검증 실패"));
  assertEquals(0,context.getSharedPreferences("full_restore_verification",0).getInt("files",-1));
 }
 @Test public void obdSessionShowsRealRepliesLatencyAndFailuresWithoutAdapterIdentity(){
  Context c=RuntimeEnvironment.getApplication();
  ObdLocalDiagnosticLog.clear(c);
  ObdLocalDiagnosticLog.probeMetrics(c,true,7,3,84,System.currentTimeMillis());
  String status=ObdLocalDiagnosticLog.summary(c);
  assertTrue(status.contains("센서 응답 7회"));
  assertTrue(status.contains("실패 3회"));
  assertTrue(status.contains("84ms"));
  assertTrue(status.contains("마지막 정상 센서 수신"));
  assertTrue(status.contains("자동 재연결 0회"));
  assertFalse(status.contains("00:11:22:33:44:55"));
 }
}
