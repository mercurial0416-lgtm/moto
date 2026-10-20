package com.motohub.next;

import org.json.JSONObject;
import java.util.List;

/** Look up the last matching service, never a service for another bike or a different kind. */
final class QuickServiceHistory {
 private QuickServiceHistory(){}
 static JSONObject latest(List<JSONObject> records,String bikeId,String type){
  if(records==null||bikeId==null||type==null||type.trim().isEmpty())return null;
  JSONObject latest=null;
  for(JSONObject record:records){
   if(!bikeId.equals(record.optString("vehicleId"))||!type.trim().equals(record.optString("type")))continue;
   if(latest==null||RecentRecordReuse.isNewer(record.optString("date"),record.optDouble("mileage"),latest.optString("date"),latest.optDouble("mileage")))latest=record;
  }
  return latest;
 }
}
