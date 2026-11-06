package com.motohub.next;

import android.content.Context;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import static org.junit.Assert.*;

/** First ride must not report success without a real GPS coordinate. */
@RunWith(RobolectricTestRunner.class)
public final class RideCaptureHealthTest {
 Context context;

 @Before public void setup(){
  context=RuntimeEnvironment.getApplication();
  context.getSharedPreferences("ride_capture_health",Context.MODE_PRIVATE).edit().clear().commit();
 }

 @Test public void noFixCannotAppearAsCompletedRide(){
  RideCaptureHealth.started(context);
  assertTrue(RideCaptureHealth.summary(context,true).contains("수신 대기"));
  RideCaptureHealth.finished(context,false,0);
  assertEquals("no_fix",RideCaptureHealth.state(context));
  assertTrue(RideCaptureHealth.summary(context,false).contains("좌표가 0개"));
  assertFalse(RideCaptureHealth.summary(context,false).contains("저장 완료"));
 }

 @Test public void firstFixAndVerifiedCloseAreDistinctEvents(){
  RideCaptureHealth.started(context);
  RideCaptureHealth.firstFix(context);
  assertEquals("receiving",RideCaptureHealth.state(context));
  RideCaptureHealth.finished(context,true,3);
  assertEquals("saved",RideCaptureHealth.state(context));
  assertTrue(RideCaptureHealth.summary(context,false).contains("좌표 3개"));
 }

 @Test public void earlyForegroundServiceFailureShowsReason(){
  RideCaptureHealth.started(context);
  RideCaptureHealth.failedIfActive(context,"알림 권한이 차단됨");
  assertEquals("failed",RideCaptureHealth.state(context));
  assertTrue(RideCaptureHealth.summary(context,false).contains("알림 권한"));
 }

 @Test public void stoppedWithoutFinalizationIsNotReportedAsSaved(){
  RideCaptureHealth.started(context);
  assertTrue(RideCaptureHealth.summary(context,false).contains("정상 종료되지"));
 }
}
