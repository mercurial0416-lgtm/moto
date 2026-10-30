package com.motohub.next;

import java.util.Arrays;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(manifest=Config.NONE,sdk=28)
public final class UX91EverydayReliabilityTest {
 @Test public void offlineUpdateFailureRetriesSoonWithoutSpammingResumes(){
  long start=1_800_000L;
  assertFalse(UpdatePromptPolicy.checkDue(start+10_000L,start,false));
  assertTrue(UpdatePromptPolicy.checkDue(start+15*60_000L,start,false));
  assertFalse(UpdatePromptPolicy.checkDue(start+15*60_000L,start,true));
  assertTrue(UpdatePromptPolicy.checkDue(start+6*60*60_000L,start,true));
 }
 @Test public void fuelSummaryHandlesUnselectedBikeAndDamagedImportRows()throws Exception{
  assertEquals(0,FuelStats.forBike(null,null).fills);
  assertEquals(0,FuelStats.forBike(Arrays.asList((JSONObject)null),"").fills);
  JSONObject legitimate=new JSONObject().put("id","a").put("vehicleId","duke")
   .put("type","주유").put("date","2026-10-31").put("amount",12000)
   .put("liters",6.0).put("mileage",850.0);
  JSONObject invalid=new JSONObject().put("vehicleId","duke").put("type","주유")
   .put("amount",20000).put("liters",0);
  JSONObject otherBike=new JSONObject().put("vehicleId","cub").put("type","주유")
   .put("amount",9000).put("liters",4);
  FuelStats stats=FuelStats.forBike(Arrays.asList(null,legitimate,invalid,otherBike),"duke");
  assertEquals(1,stats.fills);
  assertEquals(6.0,stats.liters,0.001);
  assertEquals(12000,stats.cost,0.001);
  assertEquals(2000,stats.averageWonPerLiter,0.001);
  assertTrue(Double.isNaN(stats.kmPerLiter));
 }
}
