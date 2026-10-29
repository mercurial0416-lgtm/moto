package com.motohub.next;
import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONObject;

/** Per-bike, local-only unsent quick-fuel draft; never copies values to another bike. */
final class FuelDraft {
 private FuelDraft(){}
 static final long MAX_AGE=7L*24*60*60*1000;
 private static String key(String id){
  if(!RideBikeBinding.valid(id))throw new IllegalArgumentException("바이크 ID 오류");
  return "fuel_"+id;
 }
 static JSONObject read(Context c,String bike){
  try{
   String raw=c.getSharedPreferences("fuel_drafts",0).getString(key(bike),"");
   if(raw.length()>2048||raw.isEmpty())return null;
   JSONObject j=new JSONObject(raw);
   long time=j.optLong("savedAt",0);
   if(!bike.equals(j.optString("bikeId"))||time<=0||System.currentTimeMillis()-time>MAX_AGE||
       time>System.currentTimeMillis()+60_000)return null;
   return j;
  }catch(Exception invalid){return null;}
 }
 static void save(Context c,String bike,String amount,String liters,String unitPrice,
                  String odo,String date,boolean full){
  try{
   if(amount.length()>30||liters.length()>30||unitPrice.length()>30||odo.length()>30||date.length()>30)return;
   JSONObject j=new JSONObject().put("bikeId",bike).put("savedAt",System.currentTimeMillis())
     .put("amount",amount).put("liters",liters).put("unitPrice",unitPrice)
     .put("odo",odo).put("date",date).put("full",full);
   c.getSharedPreferences("fuel_drafts",0).edit().putString(key(bike),j.toString()).apply();
  }catch(Exception ignored){}
 }
 static void clear(Context c,String bike){
  c.getSharedPreferences("fuel_drafts",0).edit().remove(key(bike)).apply();
 }
}