package com.motohub.next;

import org.json.JSONObject;
import java.util.List;

/** Duplicate detection is advisory: identical entries are never silently overwritten or rejected. */
final class QuickLogDuplicate {
 private QuickLogDuplicate(){}
 static boolean same(JSONObject current,JSONObject candidate,String collection){
  if(current==null||candidate==null||!collection.equals("expenses")&&!collection.equals("services"))return false;
  if(current.optString("id").equals(candidate.optString("id")))return false;
  if(!current.optString("vehicleId").equals(candidate.optString("vehicleId"))
   ||!current.optString("date").equals(candidate.optString("date"))
   ||!current.optString("type").equals(candidate.optString("type")))return false;
  if(Math.abs(current.optDouble("mileage")-candidate.optDouble("mileage"))>0.001)return false;
  String charge=collection.equals("expenses")?"amount":"cost";
  if(Math.abs(current.optDouble(charge)-candidate.optDouble(charge))>0.001)return false;
  return !collection.equals("expenses")||Math.abs(current.optDouble("liters")-candidate.optDouble("liters"))<0.001;
 }
 static boolean exists(List<JSONObject> records,JSONObject candidate,String collection){
  if(records==null)return false;
  for(JSONObject current:records)if(same(current,candidate,collection))return true;
  return false;
 }
}
