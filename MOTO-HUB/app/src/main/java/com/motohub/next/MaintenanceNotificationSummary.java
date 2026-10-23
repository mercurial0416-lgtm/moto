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
  String selected="";
  StringBuilder signature=new StringBuilder();
  int bestRank=99;
  // Index once in saved order. The previous implementation scanned all service
  // rows separately for each vehicle, causing O(bikes * services) work.
  Map<String,List<JSONObject>> byBike=new HashMap<>();
  for(int j=0;j<services.length();j++){
   JSONObject record=services.optJSONObject(j);
   if(record==null)continue;
   String bikeId=record.optString("vehicleId");
   if(!bikeId.isEmpty())byBike.computeIfAbsent(bikeId,k->new ArrayList<>()).add(record);
  }
  for(int i=0;i<bikes.length();i++){
   JSONObject bike=bikes.optJSONObject(i);
   if(bike==null||GarageBikeStatus.sold(bike.optString("soldDate")))continue;
   String bikeId=bike.optString("id");
   List<JSONObject> history=byBike.getOrDefault(bikeId,Collections.emptyList());
   // Retain original insertion order and latest-record tie breaking.
   for(JSONObject service:LatestServiceByType.latest(history,bikeId).values()){
    MaintenancePlanner.Status status=MaintenanceAlertWindow.status(service,today,bike.optDouble("odo"));
    if(status!=MaintenancePlanner.Status.OVERDUE&&status!=MaintenancePlanner.Status.SOON)continue;
    if(status==MaintenancePlanner.Status.OVERDUE)overdue++;else approaching++;
    String item=service.optString("type","정비");
    String remaining=MaintenanceNoticeText.detail(service.optString("nextDate"),
     service.optDouble("nextKm"),today,bike.optDouble("odo"));
    int rank=MaintenancePlanner.order(status);
    String specific=item+(remaining.isEmpty()?"":" · "+remaining);
    if(rank<bestRank){bestRank=rank;selected=specific;}
    // Detect a change to any service even if due/soon item counts stay the same.
    // Rescheduling a still-SOON item is meaningful even if the count, type
    // and ID stay unchanged. Odometer readings are excluded to avoid spam.
    signature.append(bikeId).append(':').append(service.optString("id"))
     .append(':').append(item).append(':').append(status)
     .append(':').append(service.optString("nextDate"))
     .append(':').append(service.optDouble("nextKm"))
     .append(':').append(service.optInt("alertKmBefore"))
     .append(':').append(service.optInt("alertDaysBefore")).append(';');
   }
  }
  return new MaintenanceNotificationSummary(overdue,approaching,selected,
   Integer.toHexString(signature.toString().hashCode())+":"+overdue+":"+approaching);
 }
 String title(){return due>0?"정비 시기가 지났어요":soon>0?"정비가 곧 필요해요":"정비 일정 없음";}
 boolean hasNotice(){return due>0||soon>0;}
}
