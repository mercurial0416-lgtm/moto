package com.motohub.next;

import java.util.List;
import org.json.JSONObject;

/** Most recent usable refueling entry for the selected bike only; keeps tie order deterministic. */
final class RecentFuelRecord {
 private RecentFuelRecord(){}
 static JSONObject latest(List<JSONObject> records,String bikeId){
  if(records==null||bikeId==null||bikeId.isEmpty())return null;
  JSONObject selected=null;
  for(JSONObject record:records){
   if(record==null||!"주유".equals(record.optString("type"))||
    !bikeId.equals(record.optString("vehicleId")))continue;
   double amount=record.optDouble("amount",Double.NaN),liters=record.optDouble("liters",Double.NaN);
   if(!Double.isFinite(amount)||amount<=0||amount>1_000_000_000||
    !Double.isFinite(liters)||liters<=0||liters>1000)continue;
   if(selected==null||!RecentRecordReuse.isNewer(
     selected.optString("date"),selected.optDouble("mileage",Double.NaN),
     record.optString("date"),record.optDouble("mileage",Double.NaN)))selected=record;
  }
  return selected;
 }
}
