package com.motohub.next;
import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONObject;
import java.io.File;
/** Derived distance for the current completed, bike-bound GPS collection; never an odometer. */
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
   // A track may move to the recovery bin or change bikes while storage was
   // opening. Never add a stale/removed source after that intervening edit.
   if(!RideMileageIntegrity.validCompleted(track)||!bikeId.equals(RideBikeBinding.readStrict(track)))return false;
   return p.edit().putString("ledger",ledger.toString()).commit();
  }catch(Exception ignored){return false;}
 }
 /** Reconcile a complete derived snapshot on a worker. Membership, bike bindings
  * and GPS bytes may change after merge/undo, trash/restore or reassignment.
  * Read preferences once and commit once; never publish a partial rebuild.
  * Distance cache hits still verify SHA-256, including same-size/mtime replacements.
  * Return changed entries (including removals), so callers refresh for every edit.
  */
 static synchronized int rebuild(Context c)throws java.io.IOException {
  SharedPreferences prefs=c.getSharedPreferences(PREF,Context.MODE_PRIVATE);
  JSONObject previous;
  try{previous=new JSONObject(prefs.getString("ledger","{}"));}
  catch(Exception e){throw new java.io.IOException("GPS 누적 통계 파일을 읽을 수 없습니다.",e);}
  JSONObject ledger=new JSONObject();
  int changed=0;
  for(File track:TrackFile.tracks(c.getFilesDir())){
   if(!RideMileageIntegrity.validCompleted(track))continue;
   String bike=RideBikeBinding.readStrict(track);
   if(!RideBikeBinding.valid(bike))continue;
   try{
    RideDistanceCache.Summary measured=RideDistanceCache.read(track);
    if(measured.points<=0||!Double.isFinite(measured.distanceKm)||measured.distanceKm<0||measured.distanceKm>500)continue;
    if(!bike.equals(RideBikeBinding.readStrict(track)))
     throw new java.io.IOException("GPS 바이크 연결이 확인 중 변경됐습니다.");
    JSONObject entry=new JSONObject().put("bike",bike).put("km",measured.distanceKm);
    ledger.put(track.getName(),entry);
    JSONObject old=previous.optJSONObject(track.getName());
    if(old==null||!bike.equals(old.optString("bike"))||
       Double.compare(old.optDouble("km",Double.NaN),measured.distanceKm)!=0)changed++;
   }catch(Exception invalid){
    throw new java.io.IOException("GPS 원본 확인 실패 · 이전 추정 합계를 유지했습니다.",invalid);
   }
  }
  java.util.Iterator<String> oldKeys=previous.keys();
  while(oldKeys.hasNext())if(!ledger.has(oldKeys.next()))changed++;
  if(changed>0){
   String snapshot=ledger.toString();
   if(snapshot.length()>250_000)
    throw new java.io.IOException("GPS 추정 통계 용량 초과 · 이전 합계와 원본을 유지했습니다.");
   if(!prefs.edit().putString("ledger",snapshot).commit())
    throw new java.io.IOException("GPS 누적거리 갱신을 저장할 수 없습니다. GPS 원본은 유지했습니다.");
  }
  return changed;
 }
 /** Full ZIP restore replaces the track collection; the derived old ledger
  * must not show distance from deleted or replaced rides. Rebuild after commit. */
 static synchronized boolean resetAfterFullRestore(Context c){
  return c.getSharedPreferences(PREF,Context.MODE_PRIVATE).edit().remove("ledger").commit();
 }
 /**
  * Stable, bike-bound completed ride entries for an odometer fallback anchor.
  * Return null for a corrupt ledger rather than treating it as 0 km.
  * Only copies derived distances; GPS coordinates never leave local storage.
  */
 static JSONObject bikeEntries(Context c,String bikeId){
  if(!RideBikeBinding.valid(bikeId))return null;
  try{
   String raw=c.getSharedPreferences(PREF,Context.MODE_PRIVATE).getString("ledger","{}");
   if(raw==null||raw.length()>250_000)return null;
   JSONObject ledger=new JSONObject(raw);
   JSONObject entries=new JSONObject();
   java.util.Iterator<String> keys=ledger.keys();
   while(keys.hasNext()){
    String key=keys.next();
    JSONObject item=ledger.optJSONObject(key);
    if(item==null)continue;
    if(!bikeId.equals(item.optString("bike")))continue;
    if(!key.matches("[0-9]{10,15}-[a-f0-9-]{36}\\.track"))return null;
    double km=item.optDouble("km",Double.NaN);
    if(!Double.isFinite(km)||km<0||km>500)return null;
    entries.put(key,km);
   }
   return entries;
  }catch(Exception e){return null;}
 }
 // SharedPreferences publishes one atomic snapshot. Do not acquire the writer
 // monitor here: the dashboard must remain usable during a full GPS scan/hash.
 static double total(Context c,String bikeId){
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
