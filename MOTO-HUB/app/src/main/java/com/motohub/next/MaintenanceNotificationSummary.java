package com.motohub.next;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.*;
/** Use latest service per type; summarize only actually imminent maintenance checks. */
final class MaintenanceNotificationSummary {
 final int due,soon;
 final String detail,fingerprint;
 private MaintenanceNotificationSummary(int due,int soon,String detail,String fingerprint){
  this.due=due;this.soon=soon;this.detail=detail;this.fingerprint=fingerprint;
 }
 static MaintenanceNotificationSummary from(JSONObject db,String today){
  JSONArray bikes=db.optJSONArray("vehicles"),services=db.optJSONArray("services");
  if(bikes==null||services==null)return new MaintenanceNotificationSummary(0,0,"","");
  int overdue=0,approaching=0;
  String selected="",signature="";
  int bestRank=99;
  for(int i=0;i<bikes.length();i++){
   JSONObject bike=bikes.optJSONObject(i);
   if(bike==null||GarageBikeStatus.sold(bike.optString("soldDate")))continue;
   List<JSONObject> history=new ArrayList<>();
   // Latest appended record must win when dates and odometer match.
   for(int j=services.length()-1;j>=0;j--){
    JSONObject record=services.optJSONObject(j);
    if(record!=null&&bike.optString("id").equals(record.optString("vehicleId")))history.add(record);
   }
   for(JSONObject service:LatestServiceByType.latest(history,bike.optString("id")).values()){
    int daysBefore=30;double kmBefore=500;
    if(service.optBoolean("scheduleOnly",false)){
     // Short 500 km / 1 month schedules must not alert immediately on save.
     int periodKm=service.optInt("intervalKm",0),periodMonths=service.optInt("intervalMonths",0);
     if(periodKm>0)kmBefore=Math.min(500,Math.max(50,periodKm*0.20));
     if(periodMonths>0)daysBefore=Math.min(30,Math.max(3,periodMonths*6));
    }
    MaintenancePlanner.Status status;
    try{status=MaintenancePlanner.status(service.optString("nextDate"),service.optDouble("nextKm"),
     today,bike.optDouble("odo"),daysBefore,kmBefore);}
    catch(java.time.DateTimeException invalidDate){
     status=MaintenancePlanner.status("",service.optDouble("nextKm"),today,bike.optDouble("odo"),
      daysBefore,kmBefore);
    }
    if(status!=MaintenancePlanner.Status.OVERDUE&&status!=MaintenancePlanner.Status.SOON)continue;
    if(status==MaintenancePlanner.Status.OVERDUE)overdue++;else approaching++;
    String item=service.optString("type","정비");
    String remaining=MaintenanceNoticeText.detail(service.optString("nextDate"),
     service.optDouble("nextKm"),today,bike.optDouble("odo"));
    int rank=MaintenancePlanner.order(status);
    String specific=item+(remaining.isEmpty()?"":" · "+remaining);
    if(rank<bestRank){bestRank=rank;selected=specific;}
    // Detect a change to any service even if due/soon item counts stay the same.
    signature+=bike.optString("id")+":"+service.optString("id")+":"+item+":"+status+";";
   }
  }
  return new MaintenanceNotificationSummary(overdue,approaching,selected,
   Integer.toHexString(signature.hashCode())+":"+overdue+":"+approaching);
 }
 String title(){return due>0?"정비 시기가 지났어요":soon>0?"정비가 곧 필요해요":"정비 일정 없음";}
 boolean hasNotice(){return due>0||soon>0;}
}
