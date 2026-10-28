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
  final int added, skipped;
  Result(JSONObject data,int added,int skipped){
   this.data=data;this.added=added;this.skipped=skipped;
  }
 }
 static Result merge(JSONObject current,JSONObject imported)throws JSONException {
  if(current==null||imported==null)throw new JSONException("병합할 기록이 없습니다.");
  JSONObject target=Store.parse(current.toString());
  JSONObject source=Store.parse(imported.toString());
  int added=0,skipped=0;
  // vehicles comes first: child service/expense records must retain vehicle IDs.
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
    JSONObject copy=new JSONObject(entry.toString());
    dest.put(copy);
    ids.add(id);
    if(!normalized.isEmpty())listingUrls.add(normalized);
    added++;
   }
  }
  // Revalidate referential integrity, per-collection count and 8MB schema limit.
  return new Result(Store.parse(target.toString()),added,skipped);
 }
}
