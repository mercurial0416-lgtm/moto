package com.motohub.next;
import android.content.Context;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=35)
public final class OdoSyncInboxTest {
 Context c;
 final String bike="12345678-1234-1234-1234-123456789abc";
 final String other="87654321-4321-4321-4321-abcdefabcdef";
 @Before public void setup(){c=RuntimeEnvironment.getApplication();OdometerAutoSyncInbox.disable(c);}
 @After public void finish(){OdometerAutoSyncInbox.disable(c);}
 @Test public void explicitBikeOptInAndDurableQueue(){
  assertFalse(OdometerAutoSyncInbox.enabled(c,bike));
  assertFalse(OdometerAutoSyncInbox.enable(c,"wrong",500));
  assertTrue(OdometerAutoSyncInbox.enable(c,bike,500));
  assertTrue(OdometerAutoSyncInbox.enabled(c,bike));
  assertFalse(OdometerAutoSyncInbox.enabled(c,other));
  assertFalse(OdometerAutoSyncInbox.offer(c,other,500,500,1_000_000));
  assertFalse(OdometerAutoSyncInbox.offer(c,bike,700,700,1_000_000));
  assertFalse(OdometerAutoSyncInbox.offer(c,bike,501,503,1_000_000));
  assertTrue(OdometerAutoSyncInbox.offer(c,bike,501,501.1,1_000_000));
  assertEquals(501.1,OdometerAutoSyncInbox.pending(c,bike),0.01);
  assertTrue(OdometerAutoSyncInbox.offer(c,bike,503,503.1,1_060_000));
  OdometerAutoSyncInbox.acknowledged(c,bike,501.1);
  assertEquals(503.1,OdometerAutoSyncInbox.pending(c,bike),0.01);
  OdometerAutoSyncInbox.acknowledged(c,bike,503.1);
  assertTrue(Double.isNaN(OdometerAutoSyncInbox.pending(c,bike)));
  OdometerAutoSyncInbox.disable(c);
  assertFalse(OdometerAutoSyncInbox.enabled(c,bike));
 }
}
