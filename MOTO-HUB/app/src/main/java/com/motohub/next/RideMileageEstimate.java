package com.motohub.next;
import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONObject;
import java.io.File;
/** Independently accumulates only verified completed, bike-bound GPS rides since v567. */
final class RideMileageEstimate {
 private static final String PREF="gps_distance_estimate_v567";
 private RideMileageEstimate(){}
 static synchronized boolean record(Context c,File track,String bikeId,double distanceKm){
  if(track==null||!RideBikeBinding.valid(bikeId)||!RideMileageIntegrity.validCompleted(track)
    ||!Double.isFinite(distanceKm)||distanceKm<0||distanceKm>500)return false;
  try{
   if(!bikeId.equals(RideBikeBinding.readStrict(track)))return false;
   SharedPreferences p=c.getSharedPreferences(PREF,Context.MODE_PRIVATE);
   JSONObject ledger=new JSONObject(p.getString("ledger","{}"));
   String key=track.getName();
   if(!key.matches("[0-9]{10,15}-[a-f0-9-]{36}\\.track"))return false;
   if(ledger.has(key))return false;
   JSONObject entry=new JSONObject().put("bike",bikeId).put("km",distanceKm);
   ledger.put(key,entry);
   if(ledger.toString().length()>250_000)return false;
   return p.edit().putString("ledger",ledger.toString()).commit();
  }catch(Exception ignored){return false;}
 }
 static synchronized double total(Context c,String bikeId){
  if(!RideBikeBinding.valid(bikeId))return 0;
  try{
   JSONObject ledger=new JSONObject(c.getSharedPreferences(PREF,Context.MODE_PRIVATE).getString("ledger","{}"));
   double sum=0;
   java.util.Iterator<String> keys=ledger.keys();
   while(keys.hasNext()){
    JSONObject e=ledger.optJSONObject(keys.next());
    if(e!=null&&bikeId.equals(e.optString("bike"))){
     double x=e.optDouble("km",Double.NaN);
     if(Double.isFinite(x)&&x>=0&&x<=500)sum+=x;
    }
   }
   return sum;
  }catch(Exception ignored){return Double.NaN;}
 }
}
