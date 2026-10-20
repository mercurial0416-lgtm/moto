package com.motohub.next;
import org.json.JSONObject;
import java.util.*;
import java.time.YearMonth;
public final class RecordMonthlySummaryTest {
 static int count;
 static void eq(double expected,double actual,String name){count++;if(Math.abs(expected-actual)>.001)throw new AssertionError(name+": "+actual+" != "+expected);}
 static JSONObject expense(String bike,String date,String type,double amount){return new JSONObject().put("vehicleId",bike).put("date",date).put("type",type).put("amount",amount);}
 static JSONObject service(String bike,String date,double cost){return new JSONObject().put("vehicleId",bike).put("date",date).put("cost",cost);}
 public static void main(String[] args){
  List<JSONObject> costs=Arrays.asList(expense("cub","2026-10-02","주유",20000),expense("cub","2026-10-10","주유",40000),expense("cub","2026-10-13","보험",12000),expense("cub","2026-09-29","부품",2500),expense("duke","2026-10-02","주유",200000),expense("cub","2026-01-02","주유",99000),expense("cub","2026-10-04","주유",-15000),expense("cub","2026-10-03","주유",Double.NaN));
  List<JSONObject> services=Arrays.asList(service("cub","2026-10-08",15000),service("cub","2026-09-10",55000),service("duke","2026-10-08",80000),service("cub","2026-01-01",20000));
  RecordMonthlySummary s=RecordMonthlySummary.forBike(costs,services,"cub",YearMonth.of(2026,10));
  eq(60000,s.fuel[0],"monthly fuel categorized");eq(12000,s.other[0],"other separate");eq(15000,s.service[0],"service monthly");
  eq(2500,s.other[1],"previous month other");eq(55000,s.service[1],"previous month service");
  eq(87000,s.monthTotal(0),"current total");eq(57500,s.monthTotal(1),"previous total");
  eq(74500,s.sumFuelAndOther(),"six months expenses");eq(70000,s.sumService(),"six months service");
  RecordMonthlySummary other=RecordMonthlySummary.forBike(costs,services,"duke",YearMonth.of(2026,10));eq(200000,other.fuel[0],"isolation of motorcycles");eq(80000,other.service[0],"different bike service");
  RecordMonthlySummary empty=RecordMonthlySummary.forBike(null,null,"cub",YearMonth.of(2026,10));eq(0,empty.monthTotal(0),"empty safe");
  RecordMonthlySummary winter=RecordMonthlySummary.forBike(Arrays.asList(expense("cub","2025-12-29","주유",12000)),Arrays.asList(service("cub","2025-11-30",45000)),"cub",YearMonth.of(2026,1));
  eq(12000,winter.fuel[1],"year boundary");eq(45000,winter.service[2],"november boundary");
  System.out.println("PASS six-month per-bike maintenance/fuel insights: "+count+" assertions");
 }
}
