package com.motohub.next;

import java.util.*;
import org.json.JSONObject;

/** Most recent batch from the selected bike only; never imports its date or odometer. */
final class ServiceBatchPreset {
 private ServiceBatchPreset(){}
 static List<ServiceBatch.Item> latest(List<JSONObject> records,String bikeId){
  if(records==null||!ReceiptSlots.validId(bikeId))return Collections.emptyList();
  JSONObject newest=null;
  for(JSONObject r:records){
   if(!bikeId.equals(r.optString("vehicleId")))continue;
   String bundle=r.optString("serviceBatchId");
   if(!ReceiptSlots.validId(bundle))continue;
   if(newest==null||RecentRecordReuse.isNewer(r.optString("date"),r.optDouble("mileage"),newest.optString("date"),newest.optDouble("mileage")))newest=r;
  }
  if(newest==null)return Collections.emptyList();
  String id=newest.optString("serviceBatchId");List<ServiceBatch.Item> result=new ArrayList<>();Set<String> seen=new HashSet<>();
  for(JSONObject row:records){
   if(!bikeId.equals(row.optString("vehicleId"))||!id.equals(row.optString("serviceBatchId")))continue;
   String type=row.optString("type");double amount=row.optDouble("cost",Double.NaN);
   if(!seen.add(type)||!Double.isFinite(amount)||amount<0||amount>1_000_000_000||result.size()>=ServiceBatch.MAX_ITEMS)continue;
   result.add(new ServiceBatch.Item(type,String.valueOf(Math.round(amount)),""));
  }
  return result;
 }
}
