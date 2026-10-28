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
  incoming.getJSONArray("vehicles").put(bike("a","덮어쓰면 안 됨")).put(bike("b","추가 차량"));
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
