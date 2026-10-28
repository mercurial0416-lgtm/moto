package com.motohub.next;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=35)
public final class JsonDataMergerTest {
 private static JSONObject bike(String id,String name)throws Exception{
  return new JSONObject().put("id",id).put("name",name).put("odo",500);
 }
 private static JSONObject service(String id,String bikeId)throws Exception{
  return new JSONObject().put("id",id).put("vehicleId",bikeId).put("type","엔진오일")
   .put("date","2026-10-26").put("cost",50000);
 }
 private static JSONObject listing(String id,String url)throws Exception{
  return new JSONObject().put("id",id).put("model","Duke 390").put("price",7000000).put("url",url);
 }
 @Test public void additiveMergeKeepsExistingIdsAndSettingsAndPreservesSources()throws Exception{
  JSONObject current=Store.empty().put("settings",new JSONObject().put("theme","dark"));
  current.getJSONArray("vehicles").put(bike("a","기존 바이크"));
  current.getJSONArray("services").put(service("old-service","a"));
  current.getJSONArray("listings").put(listing("old-list","https://example.org/duke"));
  JSONObject incoming=Store.empty().put("settings",new JSONObject().put("theme","light"));
  incoming.getJSONArray("vehicles").put(bike("a","기존 바이크").put("odo",999)).put(bike("b","추가 차량"));
  incoming.getJSONArray("services").put(service("old-service","a")).put(service("new-service","b"));
  incoming.getJSONArray("listings").put(listing("another-id","https://example.org/duke#details"));
  JsonDataMerger.Result result=JsonDataMerger.merge(current,incoming);
  assertEquals(2,result.added);
  assertEquals(3,result.skipped);
  assertEquals(2,result.data.getJSONArray("vehicles").length());
  assertEquals(2,result.data.getJSONArray("services").length());
  assertEquals(1,result.data.getJSONArray("listings").length());
  assertEquals("dark",result.data.getJSONObject("settings").getString("theme"));
  assertEquals("기존 바이크",result.data.getJSONArray("vehicles").getJSONObject(0).getString("name"));
  assertEquals("기존 바이크",current.getJSONArray("vehicles").getJSONObject(0).getString("name"));
  assertEquals(2,incoming.getJSONArray("services").length());
  JsonDataMerger.Result second=JsonDataMerger.merge(result.data,incoming);
  assertEquals(0,second.added);
  assertEquals(5,second.skipped);
  assertEquals(2,Store.parse(second.data.toString()).getJSONArray("vehicles").length());
 }
 @Test public void sharedBikeIdNeverSilentlyImportsForeignServiceOrFuel()throws Exception{
  JSONObject current=Store.empty();
  current.getJSONArray("vehicles").put(bike("same","Duke 390").put("year",2024));
  JSONObject imported=Store.empty();
  imported.getJSONArray("vehicles").put(bike("same","Duke 390").put("year",2024));
  imported.getJSONArray("services").put(service("foreign-service","same"));
  JsonDataMerger.Result r=JsonDataMerger.merge(current,imported);
  assertEquals(0,r.added);assertEquals(1,r.skipped);assertEquals(1,r.blocked);
  assertEquals(0,r.data.getJSONArray("services").length());
 }
 @Test public void collidingVehicleIdsNeverMisattributeServiceHistory()throws Exception{
  JSONObject existing=Store.empty();
  existing.getJSONArray("vehicles").put(bike("same-id","KTM Duke 390"));
  JSONObject imported=Store.empty();
  imported.getJSONArray("vehicles").put(bike("same-id","Honda Super Cub"));
  imported.getJSONArray("services").put(service("new-foreign-service","same-id"));
  JsonDataMerger.Result r=JsonDataMerger.merge(existing,imported);
  assertEquals(1,r.blocked);
  assertEquals(0,r.data.getJSONArray("services").length());
  assertEquals(0,existing.getJSONArray("services").length());
 }
 @Test public void yearMismatchAlsoFailsClosed()throws Exception{
  JSONObject current=Store.empty();
  current.getJSONArray("vehicles").put(bike("x","KTM").put("year",2024));
  JSONObject incoming=Store.empty();
  incoming.getJSONArray("vehicles").put(bike("x","KTM").put("year",2022));
  assertEquals(1,JsonDataMerger.conflicts(current,incoming).size());
  assertEquals(0,JsonDataMerger.merge(current,incoming).blocked);
 }
 @Test public void explicitBikeMappingOnlyImportsAcknowledgedRecords()throws Exception{
  JSONObject current=Store.empty();
  current.getJSONArray("vehicles").put(bike("shared","Honda"));
  current.getJSONArray("vehicles").put(bike("destination","KTM Duke"));
  JSONObject from=Store.empty();
  from.getJSONArray("vehicles").put(bike("shared","KTM Duke"));
  from.getJSONArray("services").put(service("imported-service","shared"));
  assertEquals(1,JsonDataMerger.merge(current,from).blocked);
  java.util.Map<String,String> mapping=new java.util.HashMap<>();
  mapping.put("shared","destination");
  JsonDataMerger.Result merged=JsonDataMerger.merge(current,from,mapping);
  assertEquals(0,merged.blocked);assertEquals(1,merged.added);
  assertEquals("destination",merged.data.getJSONArray("services").getJSONObject(0).getString("vehicleId"));
  assertEquals(0,current.getJSONArray("services").length());
 }
 @Test public void malformedBackupFailsWithoutTouchingCurrent()throws Exception{
  JSONObject current=Store.empty();
  JSONObject corrupt=Store.empty();
  corrupt.getJSONArray("services").put(service("orphan","missing-bike"));
  try{
   JsonDataMerger.merge(current,corrupt);
   fail("Orphan service may not be imported");
  }catch(org.json.JSONException expected){}
  assertEquals(0,current.getJSONArray("services").length());
 }
}
