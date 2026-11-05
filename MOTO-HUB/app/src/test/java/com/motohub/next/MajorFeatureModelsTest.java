package com.motohub.next;

import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.io.*;
import java.nio.file.*;
import org.json.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(manifest=Config.NONE,sdk=35)
public class MajorFeatureModelsTest {
 final String bike="11111111-2222-4333-8444-555555555555",other="22222222-2222-4333-8444-555555555555";
 JSONObject base()throws Exception{JSONObject db=Store.empty();db.getJSONArray("vehicles").put(new JSONObject().put("id",bike).put("name","A").put("odo",15000));return db;}
 JSONObject record(String id,String bike,String type,String date,double mileage,double paid)throws Exception{return new JSONObject().put("id",id).put("vehicleId",bike).put("type",type).put("date",date).put("mileage",mileage).put("cost",paid).put("amount",paid);}
 @Test public void componentHistoryExcludesPlansFutureAndOtherBikes()throws Exception{
  List<JSONObject> rows=Arrays.asList(record("old",bike,"타이어","2026-01-01",10000,200000),record("new",bike,"타이어","2026-05-01",12000,250000),record("plan",bike,"타이어","2026-06-01",14000,0).put("scheduleOnly",true),record("future",bike,"타이어","2027-01-01",18000,300000),record("other",other,"체인","2026-02-01",10000,100));
  List<ComponentHistory.Component> groups=ComponentHistory.inspect(rows,bike,LocalDate.of(2026,6,1));assertEquals(1,groups.size());assertEquals(2,groups.get(0).history.size());assertEquals("new",groups.get(0).last().getString("id"));assertEquals(3000,groups.get(0).usedKm(15000),0);assertTrue(Double.isNaN(groups.get(0).usedKm(11000)));
 }
 @Test public void sameDaySameOdometerUsesTheLatestSavedReplacement()throws Exception{
  List<JSONObject> rows=Arrays.asList(record("first",bike,"타이어","2026-05-01",12000,100),record("second",bike,"타이어","2026-05-01",12000,200));assertEquals("second",ComponentHistory.inspect(rows,bike,LocalDate.of(2026,5,2)).get(0).last().getString("id"));
 }
 @Test public void componentAnnotationPreservesRecordAndUnknownFieldsThroughV3Backup()throws Exception{
  JSONObject original=record("service",bike,"엔진오일","2026-05-01",12000,80000).put("componentDetails",new JSONObject().put("future",true)).put("custom",123);String before=original.toString();JSONObject annotated=ComponentHistory.annotate(original,"Oil","10W-40","Shop","memo");assertEquals(before,original.toString());assertEquals(123,annotated.getInt("custom"));assertTrue(annotated.getJSONObject("componentDetails").getBoolean("future"));JSONObject db=base();db.getJSONArray("services").put(annotated);assertEquals("10W-40",Store.parse(db.toString()).getJSONArray("services").getJSONObject(0).getJSONObject("componentDetails").getString("spec"));
 }
 @Test public void componentAnnotationRejectsPlannedWorkAndOversizedValues()throws Exception{
  JSONObject source=record("service",bike,"엔진오일","2026-05-01",12000,0);assertThrows(IllegalArgumentException.class,()->ComponentHistory.annotate(new JSONObject(source.toString()).put("scheduleOnly",true),"x","","",""));assertThrows(IllegalArgumentException.class,()->ComponentHistory.annotate(source,"x".repeat(121),"","",""));
 }
 @Test public void periodComparisonIsBikeScopedInclusiveAndExcludesUnpaid()throws Exception{
  List<JSONObject> vehicles=Arrays.asList(new JSONObject().put("id",bike).put("name","A"),new JSONObject().put("id",other).put("name","B"));List<JSONObject> expenses=Arrays.asList(record("one",bike,"주유","2026-05-01",10000,23500.5),record("two",bike,"보험","2026-05-31",11000,100000),record("three",other,"주유","2026-05-01",1000,1000),record("future",bike,"부품","2026-06-01",12000,50000),record("planned",bike,"기타","2026-05-30",11000,90000).put("scheduleOnly",true));
  List<CostPeriodReport.Bike> report=CostPeriodReport.build(vehicles,Collections.emptyList(),expenses,LocalDate.of(2026,5,1),LocalDate.of(2026,6,1),LocalDate.of(2026,5,31));assertEquals(new BigDecimal("123500.5"),report.get(0).total.stripTrailingZeros());assertEquals(new BigDecimal("1000.0"),report.get(1).total);assertEquals(2,report.get(0).records);assertEquals(2,report.get(0).excluded);assertEquals(1000,report.get(0).observedKm(),0);
 }
 @Test public void oneOrReversedMileageNeverCreatesAKmCost()throws Exception{
  JSONObject vehicle=new JSONObject().put("id",bike).put("name","A");List<JSONObject> rows=Arrays.asList(record("one",bike,"주유","2026-05-01",11000,1000),record("two",bike,"주유","2026-05-02",10000,1000));CostPeriodReport.Bike report=CostPeriodReport.build(Arrays.asList(vehicle),Collections.emptyList(),rows,LocalDate.of(2026,5,1),LocalDate.of(2026,5,31),LocalDate.of(2026,5,31)).get(0);assertTrue(Double.isNaN(report.observedKm()));assertThrows(IllegalArgumentException.class,()->CostPeriodReport.build(Arrays.asList(vehicle),rows,rows,LocalDate.of(2026,6,1),LocalDate.of(2026,5,1),LocalDate.of(2026,5,31)));
 }
 @Test public void expenseTotalsAreNotSilentlyCappedAtOneBillion()throws Exception{
  List<JSONObject> rows=Arrays.asList(record("one",bike,"보험","2026-05-01",10000,900000000),record("two",bike,"부품","2026-05-02",10100,900000000));CostPeriodReport.Bike report=CostPeriodReport.build(Arrays.asList(new JSONObject().put("id",bike).put("name","A")),Collections.emptyList(),rows,LocalDate.of(2026,5,1),LocalDate.of(2026,5,31),LocalDate.of(2026,5,31)).get(0);assertEquals(1800000000,report.total.doubleValue(),0);
 }
}
