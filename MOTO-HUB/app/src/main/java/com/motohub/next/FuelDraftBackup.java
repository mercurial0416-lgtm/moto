package com.motohub.next;

import android.content.Context;
import android.content.SharedPreferences;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import org.json.JSONObject;
import org.json.JSONException;

/** Bounded, local-only unsent fuel forms inside opt-in FULL/encrypted ZIPs.
 * No credentials, app secrets or unrelated preferences are exported. */
final class FuelDraftBackup {
 static final int MAX_DRAFTS=32,MAX_BYTES=24_000;
 private FuelDraftBackup(){}
 static JSONObject snapshot(Context c)throws IOException{
  JSONObject result=new JSONObject();
  SharedPreferences prefs=c.getSharedPreferences("fuel_drafts",0);
  try{
   for(Map.Entry<String,?> row:prefs.getAll().entrySet()){
    String key=row.getKey();
    if(!key.startsWith("fuel_"))continue;
    String bike=key.substring(5);
    if(!RideBikeBinding.valid(bike))continue;
    JSONObject draft=FuelDraft.read(c,bike);
    if(draft==null)continue; // Ignore stale and invalid unsent forms.
    if(result.length()>=MAX_DRAFTS)throw new IOException("임시 주유 기록이 너무 많아 ZIP에 보관할 수 없습니다.");
    result.put(bike,draft);
   }
   validate(result);
   return result;
  }catch(JSONException failure){throw new IOException("주유 임시기록 백업 생성 실패",failure);}
 }
 static void validate(JSONObject drafts)throws IOException{
  if(drafts==null)return;
  if(drafts.length()>MAX_DRAFTS||drafts.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8).length>MAX_BYTES)
   throw new IOException("주유 임시기록 ZIP 크기 또는 개수 초과");
  java.util.Iterator<String> bikes=drafts.keys();
  while(bikes.hasNext()){
   String bike=bikes.next();
   if(!RideBikeBinding.valid(bike))throw new IOException("주유 초안의 바이크 ID 오류");
   try{
    JSONObject d=drafts.getJSONObject(bike);
    if(!bike.equals(d.getString("bikeId")))throw new IOException("주유 초안의 바이크가 일치하지 않습니다.");
    long time=d.getLong("savedAt");
    if(time<=0||time>System.currentTimeMillis()+60_000)throw new IOException("주유 초안 저장 시각 오류");
    for(String field:new String[]{"amount","liters","unitPrice","odo","date"}){
     String value=d.getString(field);
     if(value.length()>30)throw new IOException("주유 초안 항목 크기 초과");
    }
    d.getBoolean("full");
    if(d.toString().length()>2048)throw new IOException("주유 초안 크기 초과");
   }catch(JSONException failure){throw new IOException("주유 초안 JSON 내용 오류",failure);}
  }
 }
 /** Time-independent installer audit of the exact stored draft bytes. A draft
  * crossing the seven-day UI expiry threshold must not signal data loss. */
 static String auditFingerprint(Context c)throws IOException{
  try{
   java.security.MessageDigest digest=java.security.MessageDigest.getInstance("SHA-256");
   Map<String,?> all=c.getSharedPreferences("fuel_drafts",0).getAll();
   java.util.ArrayList<String> keys=new java.util.ArrayList<>();
   for(String key:all.keySet())if(key.startsWith("fuel_"))keys.add(key);
   if(keys.size()>512)throw new IOException("저장된 임시 주유 기록 개수가 검사 한도를 초과했습니다.");
   java.util.Collections.sort(keys);
   long total=0;
   for(String key:keys){
    Object stored=all.get(key);
    if(!(stored instanceof String)||key.length()>128)
     throw new IOException("저장된 임시 주유 기록 형식 오류");
    byte[] name=key.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    byte[] value=((String)stored).getBytes(java.nio.charset.StandardCharsets.UTF_8);
    total+=name.length+value.length;
    if(value.length>4096||total>1_000_000)throw new IOException("임시 주유 기록 검사 크기 초과");
    digest.update(name);digest.update((byte)0);
    digest.update(value);digest.update((byte)0);
   }
   StringBuilder hex=new StringBuilder(64);
   for(byte b:digest.digest())hex.append(String.format(java.util.Locale.ROOT,"%02x",b&255));
   return hex.toString();
  }catch(java.security.NoSuchAlgorithmException unavailable){
   throw new IOException("임시 주유 기록 검증 알고리즘 오류",unavailable);
  }
 }
 /** Restore exactly previous allowlisted per-bike draft strings on an interrupted
  * UI-preference transaction; never mutate unrelated app preferences. */
 static boolean rollback(Context c,Map<String,?> previous){
  SharedPreferences prefs=c.getSharedPreferences("fuel_drafts",0);
  SharedPreferences.Editor editor=prefs.edit();
  for(String key:prefs.getAll().keySet()){
   if(key.startsWith("fuel_")&&RideBikeBinding.valid(key.substring(5))&&!previous.containsKey(key))
    editor.remove(key);
  }
  for(Map.Entry<String,?> entry:previous.entrySet()){
   String key=entry.getKey();
   if(!key.startsWith("fuel_")||!RideBikeBinding.valid(key.substring(5)))continue;
   if(!(entry.getValue() instanceof String))return false;
   editor.putString(key,(String)entry.getValue());
  }
  return editor.commit();
 }
 /** Restore ONLY if its motorcycle exists in the restored database; never
  * overwrite a newer in-device draft, and never clear unrelated local drafts. */
 static int restore(Context c,JSONObject drafts,JSONObject restoredDb)throws IOException{
  if(drafts==null||restoredDb==null)return 0;
  validate(drafts);
  final JSONObject safe;
  try{safe=Store.parse(restoredDb.toString());}
  catch(JSONException failure){throw new IOException("주유 초안 연결 대상 확인 실패",failure);}
  SharedPreferences prefs=c.getSharedPreferences("fuel_drafts",0);
  SharedPreferences.Editor edit=prefs.edit();
  int count=0;
  java.util.Iterator<String> bikes=drafts.keys();
  while(bikes.hasNext()){
   String bike=bikes.next();
   try{
    if(Store.find(safe,"vehicles",bike)==null)continue;
    JSONObject incoming=drafts.getJSONObject(bike);
    long timestamp=incoming.getLong("savedAt");
    if(System.currentTimeMillis()-timestamp>FuelDraft.MAX_AGE)continue;
    JSONObject existing=FuelDraft.read(c,bike);
    if(existing!=null&&existing.optLong("savedAt")>timestamp)continue;
    edit.putString("fuel_"+bike,incoming.toString());count++;
   }catch(JSONException failure){throw new IOException("주유 초안 가져오기 실패",failure);}
  }
  if(count>0&&!edit.commit())throw new IOException("주유 초안 저장 실패: 기존 앱 기록은 보존됩니다.");
  return count;
 }
}
