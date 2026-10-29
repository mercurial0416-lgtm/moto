package com.motohub.next;
import java.util.List;
import org.json.JSONObject;

/** Warn, never silently block, when a near-identical same-day fuel log exists. */
final class FuelNearDuplicate {
 private FuelNearDuplicate(){}
 static boolean near(List<JSONObject> entries,JSONObject candidate){
  if(entries==null||candidate==null)return false;
  for(JSONObject prior:entries){
   if(!"주유".equals(prior.optString("type")))continue;
   if(!prior.optString("vehicleId").equals(candidate.optString("vehicleId")))continue;
   if(!prior.optString("date").equals(candidate.optString("date")))continue;
   if(prior.optString("id").equals(candidate.optString("id")))continue;
   double amount=prior.optDouble("amount",Double.NaN),liters=prior.optDouble("liters",Double.NaN);
   double km=prior.optDouble("mileage",Double.NaN);
   if(!Double.isFinite(amount)||!Double.isFinite(liters)||!Double.isFinite(km))continue;
   if(Math.abs(amount-candidate.optDouble("amount"))<=1500
    &&Math.abs(liters-candidate.optDouble("liters"))<=0.25
    &&Math.abs(km-candidate.optDouble("mileage"))<=20)return true;
  }
  return false;
 }
}