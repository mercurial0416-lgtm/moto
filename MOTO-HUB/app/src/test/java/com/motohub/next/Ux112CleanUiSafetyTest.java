package com.motohub.next;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.json.JSONObject;
import java.time.YearMonth;
import java.util.*;
import static org.junit.Assert.*;

/** Playbacks and displays remain read-only; financial summaries reject planned work. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35)
public final class Ux112CleanUiSafetyTest {
 @Test public void replayUsesRecordedTimeRatherThanPointCount(){
  List<Long> times=Arrays.asList(1_000L,2_000L,10_000L,11_000L);
  assertEquals(0,RideReplayTimeline.indexAt(times,0));
  assertEquals(1,RideReplayTimeline.indexAt(times,500));
  assertEquals(2,RideReplayTimeline.indexAt(times,900));
  assertEquals(3,RideReplayTimeline.indexAt(times,1000));
  assertEquals(-1,RideReplayTimeline.indexAt(Collections.emptyList(),10));
  assertEquals(30_000L,RideReplayTimeline.durationMs(times));
  assertEquals(180_000L,RideReplayTimeline.durationMs(Arrays.asList(1L,1_000_000_000L)));
  assertEquals(750,RideReplayTimeline.progressAt(1000,2000,500));
 }
 @Test public void scheduledServiceNeverCountsAsMoneyAlreadySpent()throws Exception{
  String bike="11111111-2222-4333-8444-555555555555";
  String other="bbbbbbbb-2222-4333-8444-555555555555";
  List<JSONObject> services=Arrays.asList(
   new JSONObject().put("vehicleId",bike).put("date","2026-11-03").put("cost",30000),
   new JSONObject().put("vehicleId",bike).put("date","2026-11-10").put("cost",300000).put("scheduleOnly",true),
   new JSONObject().put("vehicleId",other).put("date","2026-11-03").put("cost",500000));
  List<JSONObject> fuel=Arrays.asList(
   new JSONObject().put("vehicleId",bike).put("date","2026-11-01").put("type","주유").put("amount",20000),
   new JSONObject().put("vehicleId",other).put("date","2026-11-01").put("type","주유").put("amount",100000));
  RecordMonthlySummary result=RecordMonthlySummary.forBike(fuel,services,bike,YearMonth.of(2026,11));
  assertEquals(30000,result.sumService(),0.01);
  assertEquals(20000,result.sumFuelAndOther(),0.01);
  assertTrue(RideCostPerKm.caption(bike,50000,100,0).contains("500원/GPS km"));
  assertTrue(RideCostPerKm.caption(bike,50000,100,1).contains("비교를 생략"));
 }
}
