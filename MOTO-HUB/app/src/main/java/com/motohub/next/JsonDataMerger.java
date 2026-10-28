package com.motohub.next;

import java.util.HashSet;
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
 static Result merge(JSONObject current,JSONObject imported)throws JSONException {
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
   if(!left.equalsIgnoreCase(right)||(oldYear>0&&newYear>0&&oldYear!=newYear))
    throw new JSONException("차량 ID 충돌: 서로 다른 바이크가 같은 ID를 사용합니다. 해당 JSON은 자동 병합하지 않습니다.");
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
    if(("services".equals(collection)||"expenses".equals(collection))&&
       ambiguousBikeIds.contains(entry.optString("vehicleId"))){
     blocked++;continue;
    }
    JSONObject copy=new JSONObject(entry.toString());
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
