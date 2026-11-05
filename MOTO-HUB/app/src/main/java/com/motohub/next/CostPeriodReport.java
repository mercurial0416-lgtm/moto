package com.motohub.next;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.json.JSONObject;

/** Paid ledger comparison over a user-selected inclusive period; no GPS/ODO mutation. */
final class CostPeriodReport {
 static final class Bike {
  final String id,name;final Map<String,BigDecimal> categories=new LinkedHashMap<>();int records,excluded;BigDecimal total=BigDecimal.ZERO;
  final List<JSONObject> points=new ArrayList<>();
  Bike(String id,String name){this.id=id;this.name=name;for(String key:new String[]{"주유","정비","보험","부품","튜닝","기타"})categories.put(key,BigDecimal.ZERO);}
  private double distance=Double.NaN;
  double observedKm(){return distance;}
  private double calculateKm(){
   if(points.size()<2)return Double.NaN;points.sort((a,b)->{int day=a.optString("date").compareTo(b.optString("date"));return day!=0?day:Double.compare(a.optDouble("mileage"),b.optDouble("mileage"));});
   double first=Double.NaN,last=Double.NaN;
   for(JSONObject point:points){double value=point.optDouble("mileage",Double.NaN);if(!Double.isFinite(value)||value<0||value>1e9)return Double.NaN;if(Double.isFinite(last)&&value<last)return Double.NaN;if(!Double.isFinite(first))first=value;last=value;}
   return last>first?last-first:Double.NaN;
  }
 }
 static List<Bike> build(List<JSONObject> vehicles,List<JSONObject> services,List<JSONObject> expenses,LocalDate from,LocalDate to,LocalDate today){
  if(from==null||to==null||today==null||to.isBefore(from))throw new IllegalArgumentException("시작일과 종료일을 확인하세요.");
  Map<String,Bike> result=new LinkedHashMap<>();for(JSONObject v:vehicles){String id=v.optString("id");if(!id.isEmpty())result.put(id,new Bike(id,v.optString("name")));}
  add(result,services,true,from,to,today);add(result,expenses,false,from,to,today);for(Bike bike:result.values())bike.distance=bike.calculateKm();return new ArrayList<>(result.values());
 }
 private static void add(Map<String,Bike> bikes,List<JSONObject> records,boolean service,LocalDate from,LocalDate to,LocalDate today){
  for(JSONObject item:records){if(item==null)continue;Bike bike=bikes.get(item.optString("vehicleId"));if(bike==null)continue;
   LocalDate day;try{day=LocalDate.parse(item.optString("date"));}catch(Exception invalid){bike.excluded++;continue;}
   if(day.isBefore(from)||day.isAfter(to))continue;
   double amount=item.optDouble(service?"cost":"amount",Double.NaN);
   if(item.optBoolean("scheduleOnly")||day.isAfter(today)||!Double.isFinite(amount)||amount<0||amount>1e13){bike.excluded++;continue;}
   String category=service?"정비":item.optString("type");if(!bike.categories.containsKey(category))category="기타";
   BigDecimal paid=BigDecimal.valueOf(amount);bike.categories.put(category,bike.categories.get(category).add(paid));bike.total=bike.total.add(paid);bike.records++;bike.points.add(item);
  }
 }
}
