package com.motohub.next;

import android.content.Context;
import android.content.SharedPreferences;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONObject;

/** Offline, bounded ride completion inbox. Never stores raw GPS coordinates. */
final class RideCompletionInbox {
 private static final String PREFS="ride_completion_inbox", KEY="entries";
 static final int LIMIT=10;
 private RideCompletionInbox(){}
 private static SharedPreferences prefs(Context c){return c.getSharedPreferences(PREFS,Context.MODE_PRIVATE);}
 private static JSONArray read(Context c){
  try{
   JSONArray entries=new JSONArray(prefs(c).getString(KEY,"[]"));
   return entries.length()<=LIMIT?entries:trim(entries);
  }catch(Exception e){return new JSONArray();}
 }
 private static JSONArray trim(JSONArray input){
  JSONArray bounded=new JSONArray();
  for(int i=0;i<input.length()&&i<LIMIT;i++)bounded.put(input.optJSONObject(i));
  return bounded;
 }
 static synchronized List<JSONObject> entries(Context c){
  JSONArray arr=read(c);List<JSONObject> result=new ArrayList<>();
  for(int i=0;i<arr.length();i++){
   JSONObject row=arr.optJSONObject(i);if(row!=null)result.add(row);
  }
  return result;
 }
 static synchronized JSONObject unread(Context c){
  for(JSONObject row:entries(c))if(!row.optBoolean("seen"))return row;
  return null;
 }
 static synchronized void remove(Context c,String id){
  if(id==null||id.isEmpty())return;
  JSONArray old=read(c),next=new JSONArray();
  for(int i=0;i<old.length();i++){
   JSONObject row=old.optJSONObject(i);
   if(row!=null&&!id.equals(row.optString("id")))next.put(row);
  }
  prefs(c).edit().putString(KEY,next.toString()).commit();
 }
 static synchronized boolean seen(Context c,String id){
  if(id==null||id.isEmpty())return false;
  JSONArray arr=read(c);boolean found=false;
  for(int i=0;i<arr.length();i++){
   JSONObject row=arr.optJSONObject(i);
   if(row!=null&&id.equals(row.optString("id"))){
    try{row.put("seen",true);found=true;}catch(Exception ignored){}
   }
  }
  return found&&prefs(c).edit().putString(KEY,arr.toString()).commit();
 }
 /** Called only after GPS .done commit; one entry per track, at most ten recent items. */
 static synchronized void record(Context context,File track){
  if(context==null||track==null||!RideMileageIntegrity.validCompleted(track))return;
  try{
   RideInsights.Result result=RideInsights.analyze(track);
   if(result.points<=0)return;
   String id=track.getName();
   String recap=String.format(Locale.KOREA,
    "저장된 GPS 좌표 %d개\n분석 이동거리 %.2f km\nGPS 첫·마지막 수신 간격 %d분\n관측 이동 %d분\nGPS 상태: %s\n\nGPS 수신 공백은 계산하지 않았으며 실제 계기판과 차이가 날 수 있습니다.",
    result.points,result.distanceKm,result.durationMillis/60000,
    result.movingMillis/60000,result.gpsQuality());
   SharedPreferences p=prefs(context);
   JSONArray current=read(context),next=new JSONArray();
   // Do not post another popup for an already recorded track.
   for(int i=0;i<current.length();i++)
    if(id.equals(current.optJSONObject(i)==null?"":current.optJSONObject(i).optString("id")))return;
   JSONObject row=new JSONObject();
   String boundBike=RideBikeBinding.readStrict(track);
   row.put("id",id).put("summary",recap).put("seen",false)
    .put("bikeId",boundBike)
    .put("time",System.currentTimeMillis());
   next.put(row);
   for(int i=0;i<current.length()&&next.length()<LIMIT;i++)next.put(current.getJSONObject(i));
   if(p.edit().putString(KEY,next.toString()).commit())RideSavedNotification.post(context);
  }catch(Exception ignored){/* Leave original completed GPS and .done untouched. */}
 }
}
