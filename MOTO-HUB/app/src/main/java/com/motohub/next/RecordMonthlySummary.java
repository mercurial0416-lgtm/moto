package com.motohub.next;

import java.time.YearMonth;
import java.util.List;
import org.json.JSONObject;

/** Read-only, one-pass monthly summary from locally stored records. No telemetry or uploads. */
final class RecordMonthlySummary {
 static final int MONTHS=6;
 final double[] fuel=new double[MONTHS],other=new double[MONTHS],service=new double[MONTHS];
 private RecordMonthlySummary(){}
 static RecordMonthlySummary forBike(List<JSONObject> expenses,List<JSONObject> services,String bikeId,YearMonth month){
  RecordMonthlySummary totals=new RecordMonthlySummary();
  String[] months=new String[MONTHS];for(int i=0;i<MONTHS;i++)months[i]=month.minusMonths(i).toString();
  if(expenses!=null)for(JSONObject item:expenses){
   if(!bikeId.equals(item.optString("vehicleId")))continue;
   int i=index(item.optString("date"),months);if(i<0)continue;
   double amount=item.optDouble("amount",Double.NaN);
   if("주유".equals(item.optString("type")))totals.fuel[i]=CostDashboard.add(totals.fuel[i],amount);
   else totals.other[i]=CostDashboard.add(totals.other[i],amount);
  }
  if(services!=null)for(JSONObject item:services){
   if(!bikeId.equals(item.optString("vehicleId")))continue;
   int i=index(item.optString("date"),months);if(i<0)continue;
   totals.service[i]=CostDashboard.add(totals.service[i],item.optDouble("cost",Double.NaN));
  }
  return totals;
 }
 private static int index(String date,String[] months){
  if(date==null||date.length()<7)return -1;
  for(int i=0;i<months.length;i++)if(date.startsWith(months[i]+"-"))return i;
  return -1;
 }
 double sumFuelAndOther(){double result=0;for(int i=0;i<MONTHS;i++)result+=fuel[i]+other[i];return result;}
 double sumService(){double result=0;for(double value:service)result+=value;return result;}
 double monthTotal(int i){return fuel[i]+other[i]+service[i];}
}
