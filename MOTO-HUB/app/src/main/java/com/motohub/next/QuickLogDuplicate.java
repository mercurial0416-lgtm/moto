package com.motohub.next;

import org.json.JSONObject;
import java.util.List;

/** Duplicate warnings are advisory; saving always remains the user's decision. */
final class QuickLogDuplicate {
 private QuickLogDuplicate(){}
 static boolean same(JSONObject current,JSONObject candidate,String collection){
  if(current==null||candidate==null||(!"expenses".equals(collection)&&!"services".equals(collection)))return false;
  String currentId=current.optString("id"),candidateId=candidate.optString("id");
  // Editing an existing record is not a duplicate; two unsaved (blank-id) records may be.
  if(!currentId.isEmpty()&&currentId.equals(candidateId))return false;
  String bike=current.optString("vehicleId"),date=current.optString("date"),type=current.optString("type");
  if(bike.isEmpty()||date.isEmpty()||type.isEmpty())return false;
  if(!bike.equals(candidate.optString("vehicleId"))||!date.equals(candidate.optString("date"))
   ||!type.equals(candidate.optString("type")))return false;
  double km=current.optDouble("mileage",Double.NaN),otherKm=candidate.optDouble("mileage",Double.NaN);
  if(!Double.isFinite(km)||!Double.isFinite(otherKm)||km<0||otherKm<0||Math.abs(km-otherKm)>0.001)return false;
  String charge="expenses".equals(collection)?"amount":"cost";
  double amount=current.optDouble(charge,Double.NaN),otherAmount=candidate.optDouble(charge,Double.NaN);
  if(!Double.isFinite(amount)||!Double.isFinite(otherAmount)||amount<0||otherAmount<0
   ||amount>1_000_000_000||otherAmount>1_000_000_000||Math.abs(amount-otherAmount)>0.001)return false;
  if("services".equals(collection))return true;
  double liters=current.optDouble("liters",0),otherLiters=candidate.optDouble("liters",0);
  if(!Double.isFinite(liters)||!Double.isFinite(otherLiters)||liters<0||otherLiters<0
   ||liters>1000||otherLiters>1000)return false;
  if("주유".equals(type)&&(liters<=0||otherLiters<=0))return false;
  return Math.abs(liters-otherLiters)<0.001;
 }
 static boolean exists(List<JSONObject> records,JSONObject candidate,String collection){
  if(records==null)return false;
  for(JSONObject current:records)if(same(current,candidate,collection))return true;
  return false;
 }
}
