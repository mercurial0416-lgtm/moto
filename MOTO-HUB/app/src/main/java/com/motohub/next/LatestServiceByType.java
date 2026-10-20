package com.motohub.next;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONObject;

/** The reminder on MY GARAGE must use the latest service per bike and service type. */
final class LatestServiceByType {
 private LatestServiceByType(){}
 static Map<String,JSONObject> latest(List<JSONObject> records,String bikeId){
  Map<String,JSONObject> selected=new LinkedHashMap<>();
  if(records==null||bikeId==null||bikeId.isEmpty())return selected;
  for(JSONObject item:records){
   if(item==null||!bikeId.equals(item.optString("vehicleId")))continue;
   String type=item.optString("type").trim();
   if(type.isEmpty())continue;
   JSONObject previous=selected.get(type);
   if(previous==null||RecentRecordReuse.isNewer(
    item.optString("date"),item.optDouble("mileage",Double.NaN),
    previous.optString("date"),previous.optDouble("mileage",Double.NaN))){
    selected.put(type,item);
   }
  }
  return selected;
 }
}
