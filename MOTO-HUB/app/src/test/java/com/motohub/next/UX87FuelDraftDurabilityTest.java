package com.motohub.next;

import android.content.Context;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import java.util.UUID;
import static org.junit.Assert.*;

/** Quick fuel uses apply while typing, but a lifecycle flush is synchronous and scoped. */
@RunWith(RobolectricTestRunner.class)
@Config(manifest=Config.NONE,sdk=28)
public class UX87FuelDraftDurabilityTest {
 @Test public void lifecycleFlushCommitsNewestValuesAndStaysBikeScoped(){
  Context c=RuntimeEnvironment.getApplication();
  String bike=UUID.randomUUID().toString(),other=UUID.randomUUID().toString();
  FuelDraft.save(c,bike,"10000","4","2500","810","2026-10-30",false);
  assertTrue(FuelDraft.saveDurable(c,bike,"11000","4.4","2500","815","2026-10-30",true));
  org.json.JSONObject restored=FuelDraft.read(c,bike);
  assertNotNull(restored);
  assertEquals("11000",restored.optString("amount"));
  assertEquals("815",restored.optString("odo"));
  assertTrue(restored.optBoolean("full"));
  assertNull("a bike draft must not leak to another motorcycle",FuelDraft.read(c,other));
  FuelDraft.clear(c,bike);
  assertNull("committed clear must not resurrect saved fuel",FuelDraft.read(c,bike));
 }
 @Test public void invalidPayloadCannotReplacePreviouslyCommittedDraft(){
  Context c=RuntimeEnvironment.getApplication();
  String bike=UUID.randomUUID().toString();
  assertTrue(FuelDraft.saveDurable(c,bike,"25000","10","2500","950","2026-10-30",false));
  assertFalse(FuelDraft.saveDurable(c,bike,"9".repeat(50),"10","2500","950","2026-10-30",false));
  assertEquals("25000",FuelDraft.read(c,bike).optString("amount"));
  FuelDraft.clear(c,bike);
 }
}
