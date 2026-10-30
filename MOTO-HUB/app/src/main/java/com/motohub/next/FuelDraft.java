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
 private static String encoded(String bike,String amount,String liters,String unitPrice,
                               String odo,String date,boolean full)throws Exception{
  if(amount==null||liters==null||unitPrice==null||odo==null||date==null||
     amount.length()>30||liters.length()>30||unitPrice.length()>30||odo.length()>30||date.length()>30)
   throw new IllegalArgumentException("임시 주유 기록의 입력이 너무 깁니다.");
  JSONObject j=new JSONObject().put("bikeId",bike).put("savedAt",System.currentTimeMillis())
    .put("amount",amount).put("liters",liters).put("unitPrice",unitPrice)
    .put("odo",odo).put("date",date).put("full",full);
  return j.toString();
 }
 /** Non-blocking save on each keystroke; commit durably on pause or form dismissal. */
 static void save(Context c,String bike,String amount,String liters,String unitPrice,
                  String odo,String date,boolean full){
  try{
   String raw=encoded(bike,amount,liters,unitPrice,odo,date,full);
   c.getSharedPreferences("fuel_drafts",0).edit().putString(key(bike),raw).apply();
  }catch(Exception ignored){}
 }
 /** Synchronous last-value flush for Android process loss while app is backgrounded. */
 static boolean saveDurable(Context c,String bike,String amount,String liters,String unitPrice,
                            String odo,String date,boolean full){
  try{
   String raw=encoded(bike,amount,liters,unitPrice,odo,date,full);
   return c.getSharedPreferences("fuel_drafts",0).edit().putString(key(bike),raw).commit();
  }catch(Exception invalid){return false;}
 }
 static void clear(Context c,String bike){
  // A successful form submission must not resurrect an already-saved draft.
  c.getSharedPreferences("fuel_drafts",0).edit().remove(key(bike)).commit();
 }
}