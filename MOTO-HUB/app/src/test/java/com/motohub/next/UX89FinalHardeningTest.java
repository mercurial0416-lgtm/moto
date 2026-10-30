package com.motohub.next;

import android.content.Context;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.nio.file.Files;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(manifest=Config.NONE,sdk=28)
public class UX89FinalHardeningTest {
 @Test public void draftsChangeOnlyNewUpdateFingerprintAndLegacyScopesStayStable()throws Exception{
  Context c=RuntimeEnvironment.getApplication();
  String bike=UUID.randomUUID().toString();
  FuelDraft.clear(c,bike);
  try{
   String s4=UpdateDataFootprint.captureLegacy(c,Store.empty());
   String s5=UpdateDataFootprint.captureLegacy5(c,Store.empty());
   String s6=UpdateDataFootprint.capture(c,Store.empty());
   assertTrue(s4.startsWith("schema4;"));
   assertTrue(s5.startsWith("schema5;"));
   assertTrue(s6.startsWith("schema6;"));
   assertTrue(s6.contains("fuelDraftSha256="));
   assertTrue(FuelDraft.saveDurable(c,bike,"10000","5","2000","905","2026-10-30",false));
   assertEquals(s4,UpdateDataFootprint.captureLegacy(c,Store.empty()));
   assertEquals(s5,UpdateDataFootprint.captureLegacy5(c,Store.empty()));
   assertNotEquals(s6,UpdateDataFootprint.capture(c,Store.empty()));
  }finally{FuelDraft.clear(c,bike);}
 }
 @Test public void rollbackRestoresOldFuelWithoutTouchingUnrelatedPrefs()throws Exception{
  Context c=RuntimeEnvironment.getApplication();
  String original=UUID.randomUUID().toString(),added=UUID.randomUUID().toString();
  FuelDraft.clear(c,original);FuelDraft.clear(c,added);
  try{
   assertTrue(FuelDraft.saveDurable(c,original,"12000","6","2000","300","2026-10-30",false));
   Map<String,?> old=new HashMap<>(c.getSharedPreferences("fuel_drafts",0).getAll());
   assertTrue(FuelDraft.saveDurable(c,original,"22000","11","2000","320","2026-10-30",true));
   assertTrue(FuelDraft.saveDurable(c,added,"5000","2","2500","10","2026-10-30",true));
   c.getSharedPreferences("fuel_drafts",0).edit().putString("unrelated","keep").commit();
   assertTrue(FuelDraftBackup.rollback(c,old));
   assertEquals("12000",FuelDraft.read(c,original).optString("amount"));
   assertNull(FuelDraft.read(c,added));
   assertEquals("keep",c.getSharedPreferences("fuel_drafts",0).getString("unrelated",""));
  }finally{
   FuelDraft.clear(c,original);FuelDraft.clear(c,added);
   c.getSharedPreferences("fuel_drafts",0).edit().remove("unrelated").commit();
  }
 }
 @Test public void GPSCleanupAfterRestoreDoesNotReportInterruptedRepair()throws Exception{
  Context c=RuntimeEnvironment.getApplication();
  String name="1690000000000-"+UUID.randomUUID()+".track";
  File root=new File(c.getFilesDir(),"rides-trash");
  File empty=new File(root,name);
  try{
   assertTrue(empty.isDirectory()||empty.mkdirs());
   assertEquals(0,RideTrash.recoverInterrupted(c.getFilesDir()));
  }finally{empty.delete();}
 }
 @Test public void sameDateSortUsesRealTimesAndUnknownTimestampsStayStable()throws Exception{
  List<HubScreens.UnifiedSearchHit> hits=new ArrayList<>();
  hits.add(new HubScreens.UnifiedSearchHit("2026-10-30","unknown","no time","",()->{}));
  hits.add(new HubScreens.UnifiedSearchHit("2026-10-30","morning","08:00","",()->{},100));
  hits.add(new HubScreens.UnifiedSearchHit("2026-10-30","evening","19:00","",()->{},200));
  HubScreens.newestFirst(hits);
  assertEquals("evening",hits.get(0).type);
  assertEquals("morning",hits.get(1).type);
  assertEquals("unknown",hits.get(2).type);
  JSONObject record=new JSONObject().put("created","2026-10-30T07:01:00Z");
  assertTrue(HubScreens.ledgerSortTime(record,"2026-10-30")>0);
  assertEquals(0,HubScreens.ledgerSortTime(record,"2026-10-29"));
 }
}
