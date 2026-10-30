package com.motohub.next;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import java.util.Collections;
import java.util.Map;
import static org.junit.Assert.*;

public class JsonOrphanRepairTest {
 private static JSONObject bike(String id,String name)throws Exception{
  return new JSONObject().put("id",id).put("name",name).put("year",2024).put("odo",1200);
 }
 private static JSONObject service(String id,String bikeId)throws Exception{
  return new JSONObject().put("id",id).put("vehicleId",bikeId)
    .put("type","엔진오일").put("date","2026-10-30")
    .put("mileage",800).put("cost",30000);
 }
 @Test public void missingBikeMustBeManuallyMappedAndNeverCreatesPhantomVehicle()throws Exception{
  JSONObject current=Store.empty();
  current.getJSONArray("vehicles").put(bike("actual-bike","실제 등록 바이크"));
  JSONObject incoming=Store.empty();
  incoming.getJSONArray("services").put(service("imported-service","unknown-bike"));
  JsonOrphanRepair.Staged staged=JsonOrphanRepair.stage(incoming.toString());
  assertEquals(Collections.singletonList("unknown-bike"),staged.missing);
  JsonDataMerger.Result merged=JsonDataMerger.merge(current,staged.source,Map.of("unknown-bike","actual-bike"));
  assertEquals(1,merged.added);
  assertEquals(1,merged.data.getJSONArray("vehicles").length());
  assertEquals(1,merged.data.getJSONArray("services").length());
  assertEquals("actual-bike",merged.data.getJSONArray("services").getJSONObject(0).getString("vehicleId"));
  assertNull(Store.find(merged.data,"vehicles","unknown-bike"));
 }
 @Test public void missingMappingCannotSilentlyAddUnownedServices()throws Exception{
  JSONObject current=Store.empty();
  current.getJSONArray("vehicles").put(bike("actual-bike","실제 등록 바이크"));
  JSONObject incoming=Store.empty();
  incoming.getJSONArray("services").put(service("service-1","lost-id"));
  JsonOrphanRepair.Staged stage=JsonOrphanRepair.stage(incoming.toString());
  // The UI never offers this action; still prove no auto-linking to actual-bike.
  JsonDataMerger.Result result=JsonDataMerger.merge(current,stage.source,Collections.emptyMap());
  assertEquals("lost-id",result.data.getJSONArray("services").getJSONObject(0).getString("vehicleId"));
  assertNotEquals("actual-bike",result.data.getJSONArray("services").getJSONObject(0).getString("vehicleId"));
 }
 @Test public void missingOrBlankIdsAreNotGuessed()throws Exception{
  JSONObject incoming=Store.empty();
  incoming.getJSONArray("expenses").put(new JSONObject().put("id","expense-1").put("vehicleId",""));
  try{JsonOrphanRepair.stage(incoming.toString());fail("Blank vehicle id must be rejected");}
  catch(org.json.JSONException expected){assertTrue(expected.getMessage().contains("차량 ID"));}
 }
 @Test public void duplicateOriginalIdDoesNotOverwriteExistingService()throws Exception{
  JSONObject current=Store.empty();
  current.getJSONArray("vehicles").put(bike("target","내 차"));
  current.getJSONArray("services").put(service("existing","target"));
  JSONObject imported=Store.empty();
  imported.getJSONArray("services").put(service("existing","missing-bike"));
  JsonOrphanRepair.Staged stage=JsonOrphanRepair.stage(imported.toString());
  JsonDataMerger.Result result=JsonDataMerger.merge(current,stage.source,Map.of("missing-bike","target"));
  assertEquals(0,result.added);
  assertEquals(1,result.data.getJSONArray("services").length());
 }
}
