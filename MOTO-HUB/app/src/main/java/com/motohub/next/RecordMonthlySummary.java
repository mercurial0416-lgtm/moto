package com.motohub.next;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.json.JSONObject;

/**
 * Read-only monthly summary of entered, already incurred expenses.
 * Future-dated entries and scheduling-only records must never appear as paid costs.
 */
final class RecordMonthlySummary {
 static final int MONTHS=6;
 final double[] fuel=new double[MONTHS],other=new double[MONTHS],service=new double[MONTHS];
 int scheduledOrFuture=0;
 private RecordMonthlySummary(){}
 static RecordMonthlySummary forBike(List<JSONObject> expenses,List<JSONObject> services,
                                     String bikeId,YearMonth month){
  return forBikeAsOf(expenses,services,bikeId,month,LocalDate.now(ZoneId.of("Asia/Seoul")));
 }
 /** Injected day allows strict boundary tests without relying on the device clock. */
 static RecordMonthlySummary forBikeAsOf(List<JSONObject> expenses,List<JSONObject> services,
                                         String bikeId,YearMonth month,LocalDate today){
  if(month==null||today==null)throw new IllegalArgumentException("기준 날짜 없음");
  RecordMonthlySummary totals=new RecordMonthlySummary();
  if(bikeId==null||bikeId.isEmpty())return totals;
  String[] months=new String[MONTHS];
  for(int i=0;i<MONTHS;i++)months[i]=month.minusMonths(i).toString();
  if(expenses!=null)for(JSONObject item:expenses){
   if(item==null||!bikeId.equals(item.optString("vehicleId")))continue;
   int index=index(item.optString("date"),months);
   if(index<0)continue;
   if(item.optBoolean("scheduleOnly")||future(item.optString("date"),today)){
    totals.scheduledOrFuture++;continue;
   }
   double amount=item.optDouble("amount",Double.NaN);
   if("주유".equals(item.optString("type")))totals.fuel[index]=CostDashboard.add(totals.fuel[index],amount);
   else totals.other[index]=CostDashboard.add(totals.other[index],amount);
  }
  if(services!=null)for(JSONObject item:services){
   if(item==null||!bikeId.equals(item.optString("vehicleId")))continue;
   int index=index(item.optString("date"),months);
   if(index<0)continue;
   if(item.optBoolean("scheduleOnly")||future(item.optString("date"),today)){
    totals.scheduledOrFuture++;continue;
   }
   totals.service[index]=CostDashboard.add(totals.service[index],item.optDouble("cost",Double.NaN));
  }
  return totals;
 }
 private static boolean future(String date,LocalDate today){
  try{return LocalDate.parse(date).isAfter(today);}
  catch(DateTimeParseException|NullPointerException invalid){return true;}
 }
 private static int index(String date,String[] months){
  if(date==null||date.length()!=10)return -1;
  try{LocalDate.parse(date);}catch(DateTimeParseException invalid){return -1;}
  for(int i=0;i<months.length;i++)if(date.startsWith(months[i]+"-"))return i;
  return -1;
 }
 double sumFuelAndOther(){double result=0;for(int i=0;i<MONTHS;i++)result+=fuel[i]+other[i];return result;}
 double sumService(){double result=0;for(double value:service)result+=value;return result;}
 double monthTotal(int i){return fuel[i]+other[i]+service[i];}
}
