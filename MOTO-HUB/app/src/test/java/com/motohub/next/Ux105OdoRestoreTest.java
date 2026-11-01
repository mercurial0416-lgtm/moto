package com.motohub.next;
import android.content.Context;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

/** A restored garage must never be overwritten by an old ECU pending reading. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35)
public class Ux105OdoRestoreTest {
 private Context c;
 private static final String BIKE="01234567-89ab-4cde-8fab-0123456789ab";
 @Before public void setup(){
  c=RuntimeEnvironment.getApplication();
  OdometerAutoSyncInbox.disable(c);
 }
 @After public void cleanup(){OdometerAutoSyncInbox.disable(c);}
 @Test public void restoreInvalidatesOldPendingOnlyAfterCommit(){
  assertTrue(OdometerAutoSyncInbox.enable(c,BIKE,1000));
  assertTrue(OdometerAutoSyncInbox.offer(c,BIKE,1001.0,1001.1,100000L));
  assertTrue(Double.isFinite(OdometerAutoSyncInbox.pending(c,BIKE)));
  assertTrue(OdometerAutoSyncInbox.suspendAfterRestore(c));
  assertFalse(OdometerAutoSyncInbox.enabled(c,BIKE));
  assertTrue(Double.isNaN(OdometerAutoSyncInbox.pending(c,BIKE)));
  assertFalse(OdometerAutoSyncInbox.offer(c,BIKE,1050,1050,120000L));
 }
 @Test public void manualRebaselineDiscardPendingAndRequireFreshEcu(){
  assertTrue(OdometerAutoSyncInbox.enable(c,BIKE,1000));
  assertTrue(OdometerAutoSyncInbox.offer(c,BIKE,1001,1001,100000L));
  assertFalse(OdometerAutoSyncInbox.rebaseline(c,BIKE,999,1000));
  assertFalse(OdometerAutoSyncInbox.rebaseline(c,BIKE,3000,1000));
  assertTrue(OdometerAutoSyncInbox.rebaseline(c,BIKE,1012,1000));
  assertTrue(Double.isNaN(OdometerAutoSyncInbox.pending(c,BIKE)));
  assertFalse(OdometerAutoSyncInbox.offer(c,BIKE,1000,1000,110000L));
  assertTrue(OdometerAutoSyncInbox.display(c,BIKE).contains("기준값 재확인"));
  assertTrue(OdometerAutoSyncInbox.offer(c,BIKE,1012.1,1012.1,120000L));
  assertEquals(1012.1,OdometerAutoSyncInbox.pending(c,BIKE),0.1);
  OdometerAutoSyncInbox.acknowledged(c,BIKE,1012.1);
  assertTrue(Double.isNaN(OdometerAutoSyncInbox.pending(c,BIKE)));
 }
 @Test public void laterOdoRegressionDoesNotReplaceValidatedPending(){
  assertTrue(OdometerAutoSyncInbox.enable(c,BIKE,2000));
  assertTrue(OdometerAutoSyncInbox.offer(c,BIKE,2000.5,2000.5,100000L));
  assertFalse(OdometerAutoSyncInbox.offer(c,BIKE,1999,1999,130000L));
  assertEquals(2000.5,OdometerAutoSyncInbox.pending(c,BIKE),0.01);
 }
}
