package com.motohub.next;

import java.util.*;
import org.json.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

/** Protect cross-bike privacy and separate scheduled work from completed jobs. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35)
public final class Ux107PortfolioTest {
 private static final String A="11111111-2222-4333-8444-555555555555";
 private static final String B="aaaa1111-bbbb-4ccc-8ddd-eeee11111111";
 JSONObject data()throws Exception{
  JSONObject root=Store.empty();
  root.getJSONArray("vehicles").put(new JSONObject()
    .put("id",A).put("name","390 DUKE").put("year",2024).put("odo",1405));
  root.getJSONArray("vehicles").put(new JSONObject()
    .put("id",B).put("name","Other Bike").put("year",2022).put("odo",30000));
  root.getJSONArray("services").put(new JSONObject()
    .put("id","oil").put("vehicleId",A).put("type","엔진오일")
    .put("date","2026-10-30").put("mileage",1400).put("cost",50000));
  root.getJSONArray("services").put(new JSONObject()
    .put("id","scheduled").put("vehicleId",A).put("type","타이어")
    .put("date","2026-11-15").put("scheduleOnly",true).put("cost",900000));
  root.getJSONArray("expenses").put(new JSONObject()
    .put("id","fuel").put("vehicleId",A).put("type","주유")
    .put("date","2026-11-02").put("mileage",1405).put("amount",21000).put("liters",8));
  root.getJSONArray("expenses").put(new JSONObject()
    .put("id","tuning").put("vehicleId",A).put("type","튜닝")
    .put("date","2026-10-22").put("amount",120000));
  root.getJSONArray("expenses").put(new JSONObject()
    .put("id","foreign").put("vehicleId",B).put("type","보험")
    .put("date","2026-11-02").put("amount",950000));
  return Store.parse(root.toString());
 }
 @Test public void unifiedTimelineShowsOnlyBoundBikeAndSortsSafely()throws Exception{
  List<MotoTimeline.Event> feed=MotoTimeline.ledger(data(),A);
  assertEquals(4,feed.size());
  assertEquals("fuel",feed.get(0).recordId);
  assertEquals("scheduled",feed.get(1).recordId);
  assertEquals("oil",feed.get(2).recordId);
  assertEquals(1,MotoTimeline.filtered(feed,MotoTimeline.FUEL,20).size());
  assertEquals(2,MotoTimeline.filtered(feed,MotoTimeline.SERVICE,20).size());
  assertEquals(1,MotoTimeline.filtered(feed,MotoTimeline.OTHER,20).size());
  assertEquals(2,MotoTimeline.filtered(feed,MotoTimeline.ALL,2).size());
  assertFalse(feed.toString().contains("Other Bike"));
  assertEquals(-1,MotoTimeline.startOfDay("9999-bad"));
 }
 @Test public void ownershipReportSkipsSchedulesAndOtherBikes()throws Exception{
  MotoOwnershipReport.Report report=MotoOwnershipReport.prepare(data(),A);
  assertEquals("390 DUKE",report.bikeName);
  assertEquals(1,report.completedServiceCount);
  assertEquals(1,report.fuelCount);
  assertEquals(1,report.otherCount);
  assertEquals(50000,report.serviceCost,.01);
  assertEquals(21000,report.fuelCost,.01);
  assertEquals(120000,report.otherCost,.01);
  assertEquals(3,report.totalCount);
  String all=String.join("\n",report.records);
  assertTrue(all.contains("엔진오일"));
  assertTrue(all.contains("튜닝"));
  assertFalse(all.contains("타이어"));
  assertFalse(all.contains("950,000"));
  assertFalse(all.contains("Other Bike"));
  assertEquals(0,report.records.stream().filter(x->x.contains("GPS 좌표")).count());
 }
 @Test public void userFacingTextCannotInsertNewPdfLines()throws Exception{
  assertEquals("memo added",MotoOwnershipReport.clean("memo\nadded",30));
  assertEquals("one two",MotoTimeline.safeText("one\ttwo",30));
  try{MotoOwnershipReport.prepare(data(),"bad-bike-id");fail("invalid bike");}
  catch(java.io.IOException expected){}
 }
}
