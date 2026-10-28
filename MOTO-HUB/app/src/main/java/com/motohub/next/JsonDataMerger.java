package com.motohub.next;

import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Opt-in JSON-only additive import. Never modifies current records or settings,
 * and never attempts to merge GPS tracks, photos, or backup ZIP sidecars.
 */
final class JsonDataMerger {
 private JsonDataMerger(){}
 static final class Result {
  final JSONObject data;
  final int added, skipped, blocked;
  Result(JSONObject data,int added,int skipped,int blocked){
   this.data=data;this.added=added;this.skipped=skipped;this.blocked=blocked;
  }
 }
 static List<JSONObject> conflicts(JSONObject current,JSONObject imported)throws JSONException{
  JSONObject safeCurrent=Store.parse(current.toString());
  JSONObject safeIncoming=Store.parse(imported.toString());
  Set<String> ids=new HashSet<>();
  JSONArray bikes=safeCurrent.getJSONArray("vehicles");
  for(int i=0;i<bikes.length();i++)ids.add(bikes.getJSONObject(i).getString("id"));
  List<JSONObject> collisions=new ArrayList<>();
  JSONArray source=safeIncoming.getJSONArray("vehicles");
  for(int i=0;i<source.length();i++){
   JSONObject bike=source.getJSONObject(i);
   if(ids.contains(bike.getString("id")))collisions.add(bike);
  }
  return collisions;
 }
 static Result merge(JSONObject current,JSONObject imported)throws JSONException{
  return merge(current,imported,new HashMap<>());
 }
 /** No implicit attribution: source-bicycle ID must be explicitly mapped to an existing bike. */
 static Result merge(JSONObject current,JSONObject imported,Map<String,String> confirmed)throws JSONException {
  if(current==null||imported==null)throw new JSONException("병합할 기록이 없습니다.");
  JSONObject target=Store.parse(current.toString());
  JSONObject source=Store.parse(imported.toString());
  int added=0,skipped=0,blocked=0;
  Set<String> ambiguousBikeIds=new HashSet<>();
  // vehicles comes first: child service/expense records must retain vehicle IDs.
  // Two backups may reuse a vehicle UUID for DIFFERENT motorcycles.
  // Treat that as a conflict rather than silently connecting imported repairs
  // and fuel costs to an unrelated currently registered vehicle.
  JSONArray currentBikes=target.getJSONArray("vehicles");
  JSONArray importedBikes=source.getJSONArray("vehicles");
  java.util.Map<String,JSONObject> byId=new java.util.HashMap<>();
  for(int i=0;i<currentBikes.length();i++){
   JSONObject bike=currentBikes.getJSONObject(i);byId.put(bike.getString("id"),bike);
  }
  for(int i=0;i<importedBikes.length();i++){
   JSONObject incoming=importedBikes.getJSONObject(i);
   JSONObject existing=byId.get(incoming.getString("id"));
   if(existing==null)continue;
   // Identity is not proven by a matching local ID + name/year.
   // Both backups might have been generated from cloned profiles.
   // Import service/expense records for shared IDs only by deliberate manual restore,
   // never into a possibly different motorcycle.
   ambiguousBikeIds.add(incoming.getString("id"));
   String left=existing.optString("name").trim(),right=incoming.optString("name").trim();
   int oldYear=existing.optInt("year"),newYear=incoming.optInt("year");
   // A name or model-year difference is a collision, not a reason to discard the JSON.
   // Leave these records blocked until the user explicitly chooses a destination.
  }
  for(String collection:Store.COLLECTIONS){
   JSONArray dest=target.getJSONArray(collection);
   JSONArray input=source.getJSONArray(collection);
   Set<String> ids=new HashSet<>();
   Set<String> listingUrls=new HashSet<>();
   for(int j=0;j<dest.length();j++){
    JSONObject entry=dest.getJSONObject(j);
    ids.add(entry.getString("id"));
    if("listings".equals(collection)){
     String url=entry.optString("url","");
     if(!url.isEmpty())listingUrls.add(Store.canonicalUrl(url));
    }
   }
   for(int j=0;j<input.length();j++){
    JSONObject entry=input.getJSONObject(j);
    String id=entry.getString("id");
    String url="listings".equals(collection)?entry.optString("url",""):"";
    String normalized=url.isEmpty()?"":Store.canonicalUrl(url);
    // Matching IDs (all collections) or identical listing URLs are NOT overwritten.
    if(ids.contains(id)||(!normalized.isEmpty()&&listingUrls.contains(normalized))){
     skipped++;continue;
    }
    boolean child="services".equals(collection)||"expenses".equals(collection);
    String sourceBike=entry.optString("vehicleId");
    String mapped=confirmed==null?null:confirmed.get(sourceBike);
    if(child&&ambiguousBikeIds.contains(sourceBike)&&mapped==null){
     blocked++;continue;
    }
    if(child&&mapped!=null&&byId.get(mapped)==null)
     throw new JSONException("선택한 대상 바이크가 현재 데이터에 없습니다.");
    JSONObject copy=new JSONObject(entry.toString());
    if(child&&mapped!=null)copy.put("vehicleId",mapped);
    dest.put(copy);
    ids.add(id);
    if(!normalized.isEmpty())listingUrls.add(normalized);
    added++;
   }
  }
  // Revalidate referential integrity, per-collection count and 8MB schema limit.
  return new Result(Store.parse(target.toString()),added,skipped,blocked);
 }
}
