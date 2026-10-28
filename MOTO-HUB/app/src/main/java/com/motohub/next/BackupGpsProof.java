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
 static void record(Context context,Map<String,String> hashes)throws IOException{
  JSONObject json=new JSONObject();
  try{
   for(Map.Entry<String,String> row:hashes.entrySet()){
    if(!row.getKey().matches(BackupFormat.TRACK)||!row.getValue().matches("[a-f0-9]{64}"))
     throw new IOException("백업 GPS 검증값이 유효하지 않습니다.");
    json.put(row.getKey(),row.getValue());
   }
  }catch(org.json.JSONException e){throw new IOException("백업 검증값 기록 실패",e);}
  SharedPreferences.Editor editor=context.getSharedPreferences(PREFS,0).edit()
    .putString(KEY,json.toString()).putLong("completed_at",System.currentTimeMillis());
  if(!editor.commit())throw new IOException("검증한 백업 GPS 정보를 저장하지 못했습니다.");
 }
 static void requireExact(Context context,File track)throws IOException{
  if(track==null||!track.getName().matches(BackupFormat.TRACK))throw new IOException("GPS 파일 이름이 유효하지 않습니다.");
  String hash;
  try{hash=new JSONObject(context.getSharedPreferences(PREFS,0).getString(KEY,"{}")).optString(track.getName());}
  catch(org.json.JSONException e){throw new IOException("검증된 ZIP 내보내기 정보가 없습니다.",e);}
  if(!hash.matches("[a-f0-9]{64}"))throw new IOException("최근에 검증된 ZIP에 이 GPS 원본이 포함됐다는 증거가 없습니다. 전체 ZIP을 새로 내보내세요.");
  String current=RideMileageIntegrity.fingerprint(track);
  if(!java.security.MessageDigest.isEqual(hash.getBytes(java.nio.charset.StandardCharsets.US_ASCII),
     current.getBytes(java.nio.charset.StandardCharsets.US_ASCII)))
   throw new IOException("백업 ZIP에 담긴 GPS와 현재 파일이 달라 삭제를 거부했습니다.");
 }
}
