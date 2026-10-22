package com.motohub.next;

import org.json.JSONObject;

/** Shared rule for local notification, dashboard and newly completed intervals. */
final class MaintenanceAlertWindow {
 private MaintenanceAlertWindow(){}
 static int kmBefore(JSONObject s){
  int preferred=s.optInt("alertKmBefore",0);
  if(preferred<=0)preferred=500;
  int interval=s.optInt("intervalKm",0);
  if(interval>0)preferred=Math.min(preferred,(int)Math.max(50,Math.round(interval*0.20)));
  return Math.max(0,Math.min(preferred,5000));
 }
 static int daysBefore(JSONObject s){
  int preferred=s.optInt("alertDaysBefore",0);
  if(preferred<=0)preferred=30;
  int months=s.optInt("intervalMonths",0);
  if(months>0)preferred=Math.min(preferred,Math.max(3,months*6));
  return Math.max(0,Math.min(preferred,365));
 }
 static MaintenancePlanner.Status status(JSONObject service,String today,double odometer){
  double km=service.optDouble("nextKm",0);
  try{return MaintenancePlanner.status(service.optString("nextDate"),km,today,odometer,daysBefore(service),kmBefore(service));}
  catch(java.time.DateTimeException invalidDate){
   return MaintenancePlanner.status("",km,today,odometer,daysBefore(service),kmBefore(service));
  }
 }
}
