package com.motohub.next;
import android.content.Context;
import android.content.SharedPreferences;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import org.json.JSONObject;
/** Proves an exact track was present in a readback-verified ZIP; filename/time alone never authorizes deletion. */
final class BackupGpsProof {
 private static final String PREFS="verified_full_export",KEY="track_sha256";
 private BackupGpsProof(){}
 static void record(Context context,Map<String,String> hashes)throws IOException{record(context,hashes,null,false);}
 static void record(Context context,Map<String,String> hashes,String currentJson,boolean full)throws IOException{
  JSONObject json=new JSONObject();
  try{
   for(Map.Entry<String,String> row:hashes.entrySet()){
    if(!row.getKey().matches(BackupFormat.TRACK)||!row.getValue().matches("[a-f0-9]{64}"))
     throw new IOException("백업 GPS 검증값이 유효하지 않습니다.");
    json.put(row.getKey(),row.getValue());
   }
  }catch(org.json.JSONException e){throw new IOException("백업 검증값 기록 실패",e);}
  SharedPreferences.Editor editor=context.getSharedPreferences(PREFS,0).edit()
    .putString(KEY,json.toString()).putLong("completed_at",System.currentTimeMillis())
    .putString("last_gps_zip_kind",full?"전체 ZIP":"GPS 분할 ZIP");
  if(full){
   if(currentJson==null||currentJson.isEmpty())throw new IOException("전체 백업 원본을 확인하지 못했습니다.");
   editor.putString("full_db_sha256",digest(currentJson)).putLong("full_completed_at",System.currentTimeMillis());
  }
  if(!editor.commit())throw new IOException("검증한 백업 GPS 정보를 저장하지 못했습니다.");
 }
 private static String digest(String value)throws IOException{
  try{
   byte[] bytes=java.security.MessageDigest.getInstance("SHA-256")
      .digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
   StringBuilder hex=new StringBuilder(64);
   for(byte b:bytes)hex.append(String.format(java.util.Locale.ROOT,"%02x",b&0xff));
   return hex.toString();
  }catch(java.security.NoSuchAlgorithmException e){throw new IOException("백업 무결성 검사 불가",e);}
 }
 /** A verified FULL ZIP containing this exact database snapshot is mandatory before cascade delete. */
 static void requireFullCurrent(Context context,String currentJson)throws IOException{
  SharedPreferences p=context.getSharedPreferences(PREFS,0);
  long at=p.getLong("full_completed_at",0);
  String expected=p.getString("full_db_sha256","");
  if(at<=0||!expected.matches("[a-f0-9]{64}"))
   throw new IOException("바이크와 연결 기록을 삭제하려면 먼저 전체 ZIP 백업을 새로 만들고 검증해 주세요.");
  if(!java.security.MessageDigest.isEqual(expected.getBytes(java.nio.charset.StandardCharsets.US_ASCII),
     digest(currentJson).getBytes(java.nio.charset.StandardCharsets.US_ASCII)))
   throw new IOException("마지막 전체 백업 이후 데이터가 변경됐습니다. 현재 기록으로 전체 ZIP을 다시 백업해 주세요.");
 }
 static String status(Context context,File track){
  SharedPreferences p=context.getSharedPreferences(PREFS,0);
  long timestamp=p.getLong("completed_at",0);
  if(timestamp<=0)return "백업 상태: 검증된 GPS 내보내기 없음";
  try{
   String hash=new JSONObject(p.getString(KEY,"{}")).optString(track.getName());
   if(!hash.matches("[a-f0-9]{64}"))return "백업 상태: 이 GPS는 검증된 ZIP에 없음";
   String date=new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm",java.util.Locale.KOREA).format(new java.util.Date(timestamp));
   String kind=p.getString("last_gps_zip_kind","검증 ZIP");
   return "백업 상태: "+date+" "+kind+"에 GPS 포함 (원본 SHA 최종 재검사 필요)";
  }catch(Exception e){return "백업 상태: 검증 정보 확인 불가";}
 }
 static String fullStatus(Context context){
  long timestamp=context.getSharedPreferences(PREFS,0).getLong("full_completed_at",0);
  if(timestamp<=0)return "검증된 전체 ZIP 백업 없음";
  String date=new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm",java.util.Locale.KOREA).format(new java.util.Date(timestamp));
  return "최근 검증 전체 ZIP: "+date+" · 이후 기록 변경 시 새 백업 필요";
 }
 static void requireExact(Context context,File track)throws IOException{
  if(track==null||!track.getName().matches(BackupFormat.TRACK))throw new IOException("GPS 파일 이름이 유효하지 않습니다.");
  String hash;
  try{hash=new JSONObject(context.getSharedPreferences(PREFS,0).getString(KEY,"{}")).optString(track.getName());}
  catch(org.json.JSONException e){throw new IOException("검증된 ZIP 내보내기 정보가 없습니다.",e);}
  if(!hash.matches("[a-f0-9]{64}"))throw new IOException("최근 검증된 ZIP에 이 GPS 원본이 포함됐다는 증거가 없습니다. 전체 ZIP이나 GPS 분할 ZIP을 새로 내보내세요.");
  String current=RideMileageIntegrity.fingerprint(track);
  if(!java.security.MessageDigest.isEqual(hash.getBytes(java.nio.charset.StandardCharsets.US_ASCII),
     current.getBytes(java.nio.charset.StandardCharsets.US_ASCII)))
   throw new IOException("백업 ZIP에 담긴 GPS와 현재 파일이 달라 삭제를 거부했습니다.");
 }
}
