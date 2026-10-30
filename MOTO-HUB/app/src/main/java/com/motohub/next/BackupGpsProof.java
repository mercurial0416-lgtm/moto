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
  record(context,hashes,null,currentJson,full);
 }
 /** Hashes must come from reopening and verifying the completed ZIP. */
 static void record(Context context,Map<String,String> hashes,Map<String,String> sidecars,
                   String currentJson,boolean full)throws IOException{
  JSONObject json=new JSONObject(),metadata=new JSONObject();
  try{
   for(Map.Entry<String,String> row:hashes.entrySet()){
    if(!row.getKey().matches(BackupFormat.TRACK)||!row.getValue().matches("[a-f0-9]{64}"))
     throw new IOException("백업 GPS 검증값이 유효하지 않습니다.");
    json.put(row.getKey(),row.getValue());
    if(sidecars!=null)metadata.put(row.getKey(),new JSONObject());
   }
   if(sidecars!=null)for(Map.Entry<String,String> row:sidecars.entrySet()){
    String path=row.getKey(),suffix=BackupFormat.sidecarSuffix(path);
    if(!BackupFormat.isSidecar(path)||!row.getValue().matches("[a-f0-9]{64}"))
     throw new IOException("GPS 부속 파일 검증값 오류");
    String track=path.substring("rides/".length(),path.length()-suffix.length());
    if(!hashes.containsKey(track))throw new IOException("GPS 원본 없는 부속 파일");
    metadata.getJSONObject(track).put(suffix,row.getValue());
   }
  }catch(org.json.JSONException e){throw new IOException("백업 검증값 기록 실패",e);}
  SharedPreferences.Editor editor=context.getSharedPreferences(PREFS,0).edit()
    .putString(KEY,json.toString()).putString("sidecar_sha256",sidecars==null?"":metadata.toString())
    .putLong("completed_at",System.currentTimeMillis())
    .putString("last_gps_zip_kind",full?"전체 ZIP":"GPS 분할 ZIP");
  if(full){
   if(currentJson==null||currentJson.isEmpty())throw new IOException("전체 백업 원본을 확인하지 못했습니다.");
   editor.putString("full_db_sha256",digest(currentJson)).putLong("full_completed_at",System.currentTimeMillis());
  }
  if(!editor.commit())throw new IOException("검증한 백업 GPS 정보를 저장하지 못했습니다.");
 }
 private static String digest(String value)throws IOException{
  return digestBytes(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
 }
 private static String digestBytes(byte[] bytes)throws IOException{
  try{
   bytes=java.security.MessageDigest.getInstance("SHA-256").digest(bytes);
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
   String metadata=p.getString("sidecar_sha256","");
   if(metadata==null||metadata.isEmpty())return "백업 상태: GPS 원본 검증만 완료 · 부속 파일 검증을 위해 새 ZIP 필요";
   return "백업 상태: "+date+" "+kind+"에 GPS·부속 파일 포함 (이동 전 SHA 재검사)";
  }catch(Exception e){return "백업 상태: 검증 정보 확인 불가";}
 }
 /** Does not prove the exported ZIP was saved outside the phone. */
 static String fullCurrentStatus(Context context,String currentJson){
  SharedPreferences prefs=context.getSharedPreferences(PREFS,0);
  String saved=prefs.getString("full_db_sha256","");
  if(saved==null||!saved.matches("[a-f0-9]{64}"))return "전체 백업 미검증 · ZIP 내보내기 필요";
  if(currentJson==null)return "현재 기록 확인 불가 · 외부 백업 확인 필요";
  try{
   String current=digest(currentJson);
   return java.security.MessageDigest.isEqual(saved.getBytes(java.nio.charset.StandardCharsets.US_ASCII),
      current.getBytes(java.nio.charset.StandardCharsets.US_ASCII))
    ?"현재 데이터와 일치하는 전체 ZIP 검증 기록 있음 · 외부 파일 보관은 별도 확인"
    :"마지막 전체 ZIP 이후 데이터 변경됨 · 새 전체 백업 권장";
  }catch(IOException error){return "전체 백업 검증 상태 확인 실패 · 원본 유지";}
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
  final JSONObject expectedSidecars;
  try{
   String raw=context.getSharedPreferences(PREFS,0).getString("sidecar_sha256","");
   if(raw==null||raw.isEmpty())throw new IOException("GPS 부속 파일 검증값이 없습니다. 전체 ZIP 또는 GPS 분할 ZIP을 새로 만드세요.");
   expectedSidecars=new JSONObject(raw).optJSONObject(track.getName());
   if(expectedSidecars==null)throw new IOException("GPS 부속 파일 검증값이 없습니다. ZIP을 다시 백업하세요.");
  }catch(org.json.JSONException malformed){throw new IOException("GPS 부속 파일 검증 정보 오류",malformed);}
  for(String suffix:RideTrash.SIDECARS){
   File sidecar=new File(track.getPath()+suffix);
   String expected=expectedSidecars.optString(suffix,"");
   boolean present=java.nio.file.Files.exists(sidecar.toPath(),java.nio.file.LinkOption.NOFOLLOW_LINKS);
   if(!present){
    if(!expected.isEmpty())throw new IOException("백업된 GPS 부속 파일이 사라졌습니다: "+suffix);
    continue;
   }
   if(!java.nio.file.Files.isRegularFile(sidecar.toPath(),java.nio.file.LinkOption.NOFOLLOW_LINKS)||
      java.nio.file.Files.isSymbolicLink(sidecar.toPath()))
    throw new IOException("안전하지 않은 GPS 부속 파일");
   if(expected.isEmpty())throw new IOException("GPS 부속 파일이 백업 후 추가됐습니다: "+suffix);
   long limit=suffix.equals(".mergedinto")?100:suffix.equals(".bike")||suffix.equals(".category")?64:0;
   if(sidecar.length()>limit)throw new IOException("GPS 부속 파일 크기 초과: "+suffix);
   String actual=digestBytes(java.nio.file.Files.readAllBytes(sidecar.toPath()));
   if(!java.security.MessageDigest.isEqual(expected.getBytes(java.nio.charset.StandardCharsets.US_ASCII),
      actual.getBytes(java.nio.charset.StandardCharsets.US_ASCII)))
    throw new IOException("GPS 부속 파일이 백업 이후 변경됐습니다: "+suffix);
  }
  String current=RideMileageIntegrity.fingerprint(track);
  if(!java.security.MessageDigest.isEqual(hash.getBytes(java.nio.charset.StandardCharsets.US_ASCII),
     current.getBytes(java.nio.charset.StandardCharsets.US_ASCII)))
   throw new IOException("백업 ZIP에 담긴 GPS와 현재 파일이 달라 삭제를 거부했습니다.");
 }
}
