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
 private static boolean noCompletionMarker(File file){
  for(String suffix:new String[]{".done",".imported",".recovered"})
   if(new File(file.getPath()+suffix).exists())return false;
  return true;
 }
 /** Re-reads the actual on-device GPS bytes after ZIP readback, before any proof is persisted.
  * This is deliberately worker-only IO: a source changed while exporting must not be blessed.
  */
 static void recordVerifiedZip(Context context,Map<String,String> hashes,Map<String,String> sidecars,
                   String currentJson,boolean full)throws IOException{
  if(hashes==null||sidecars==null)throw new IOException("검증된 GPS 및 부속 파일 해시가 없습니다.");
  File rides=new File(context.getFilesDir(),"rides");
  if(java.nio.file.Files.isSymbolicLink(rides.toPath())||
     rides.exists()&&!rides.isDirectory())throw new IOException("GPS 저장 폴더가 안전하지 않습니다.");
  if(full){
   File[] local=rides.isDirectory()?rides.listFiles((dir,name)->name.matches(BackupFormat.TRACK)):new File[0];
   if(local==null)throw new IOException("GPS 목록을 읽지 못했습니다.");
   int included=0;
   for(File file:local){
    if(file.length()==0&&noCompletionMarker(file))continue;
    included++;
    if(!hashes.containsKey(file.getName()))
     throw new IOException("ZIP 이후 새 GPS 원본이 있습니다. 다시 백업하세요.");
   }
   if(included!=hashes.size())throw new IOException("ZIP 이후 GPS 원본 목록이 변경됐습니다. 다시 백업하세요.");
  }
  for(Map.Entry<String,String> row:hashes.entrySet()){
   String name=row.getKey(),expected=row.getValue();
   if(!name.matches(BackupFormat.TRACK)||expected==null||!expected.matches("[a-f0-9]{64}"))
    throw new IOException("GPS 검증값이 유효하지 않습니다.");
   File track=new File(rides,name);
   if(!expected.equals(hashLocalFile(track,TrackFile.MAX_BYTES)))
    throw new IOException("ZIP 내보내기 도중 GPS 원본이 변경됐습니다: "+name);
   for(String suffix:RideTrash.SIDECARS){
    File sidecar=new File(track.getPath()+suffix);
    String path="rides/"+name+suffix,hash=sidecars.get(path);
    if(!java.nio.file.Files.exists(sidecar.toPath(),java.nio.file.LinkOption.NOFOLLOW_LINKS)){
     if(hash!=null)throw new IOException("ZIP에 백업한 GPS 부속 파일이 사라졌습니다: "+suffix);
     continue;
    }
    if(hash==null||!hash.matches("[a-f0-9]{64}"))
     throw new IOException("ZIP 내보내기 후 GPS 부속 파일이 추가되거나 손상됐습니다: "+suffix);
    long limit=suffix.equals(".mergedinto")?100:suffix.equals(".bike")||suffix.equals(".category")?64:0;
    if(!hash.equals(hashLocalFile(sidecar,limit)))
     throw new IOException("ZIP 내보내기 도중 GPS 부속 파일이 변경됐습니다: "+suffix);
   }
  }
  record(context,hashes,sidecars,currentJson,full);
 }
 private static String hashLocalFile(File file,long limit)throws IOException{
  if(file==null||java.nio.file.Files.isSymbolicLink(file.toPath())||
     !java.nio.file.Files.isRegularFile(file.toPath(),java.nio.file.LinkOption.NOFOLLOW_LINKS)||
     file.length()>limit)throw new IOException("백업 원본이 없거나 안전하지 않습니다: "+(file==null?"GPS":file.getName()));
  try{
   java.security.MessageDigest digest=java.security.MessageDigest.getInstance("SHA-256");
   try(java.io.InputStream in=new java.io.BufferedInputStream(new java.io.FileInputStream(file))){
    byte[] buffer=new byte[32768];long total=0;int n;
    while((n=in.read(buffer))!=-1){
     total+=n;if(total>limit)throw new IOException("GPS 백업 원본 크기 제한 초과");
     digest.update(buffer,0,n);
    }
   }
   StringBuilder out=new StringBuilder(64);
   for(byte b:digest.digest())out.append(String.format(java.util.Locale.ROOT,"%02x",b&255));
   return out.toString();
  }catch(java.security.NoSuchAlgorithmException e){throw new IOException("GPS SHA-256 검증 불가",e);}
 }
 /** Used by destructive actions only after the UI has captured an immutable DB snapshot. */
 static void requireFullDeepCurrent(Context context,String currentJson)throws IOException{
  requireFullCurrent(context,currentJson);
  String verdict=fullGpsDeepStatus(context,currentJson);
  if(!verdict.startsWith("전체 ZIP과 현재 DB·GPS 원본 및 부속 파일 "))
   throw new IOException("GPS 전체 백업 정밀 검사 미통과 · "+verdict);
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
   JSONObject fileDetails=new JSONObject();
   File folder=new File(context.getFilesDir(),"rides");
   for(String name:hashes.keySet()){
    File file=new File(folder,name);
    if(!file.isFile()||java.nio.file.Files.isSymbolicLink(file.toPath()))
     throw new IOException("전체 백업 GPS 원본 파일이 사라졌습니다. 새로 내보내세요.");
    try{fileDetails.put(name,new JSONObject().put("size",file.length()).put("modified",file.lastModified()));}
    catch(org.json.JSONException malformed){throw new IOException("백업 GPS 파일 정보 저장 실패",malformed);}
   }
   editor.putString("full_db_sha256",digest(currentJson))
    .putString("full_track_names",json.toString())
    .putString("full_track_details",fileDetails.toString())
    // Snapshot of the verified FULL ZIP, not the most recent split GPS ZIP.
    .putString("full_sidecar_sha256",sidecars==null?"":metadata.toString())
    .putLong("full_completed_at",System.currentTimeMillis());
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
    ?fullGpsStatus(context,prefs)
    :"마지막 전체 ZIP 이후 데이터 변경됨 · 새 전체 백업 권장";
  }catch(IOException error){return "전체 백업 검증 상태 확인 실패 · 원본 유지";}
 }
 /** Fast metadata check for the settings screen. Byte-for-byte verification is a separate worker action. */
 private static String fullGpsStatus(Context context,SharedPreferences prefs){
  String raw=prefs.getString("full_track_names",null),detailsRaw=prefs.getString("full_track_details",null);
  if(raw==null)return "DB 백업 일치 · 이전 ZIP의 GPS 정보 없음 · 새 전체 백업 권장";
  try{
   JSONObject exported=new JSONObject(raw);
   if(detailsRaw==null)return "DB 백업 일치 · GPS 변경 검사 정보 없음 · 새 전체 백업 권장";
   JSONObject details=new JSONObject(detailsRaw);
   File rides=new File(context.getFilesDir(),"rides");
   if(rides.exists()&&!rides.isDirectory())return "GPS 저장 폴더 확인 불가 · 새 백업 필요";
   File[] local=rides.isDirectory()?rides.listFiles((dir,name)->name.matches(BackupFormat.TRACK)):new File[0];
   if(local==null)return "GPS 파일 목록 읽기 실패 · 새 백업 필요";
   int checked=0;
   for(File file:local){
    if(file.length()==0&&noCompletionMarker(file))continue;
    if(java.nio.file.Files.isSymbolicLink(file.toPath())||!file.isFile())return "GPS 원본 파일 접근 오류 · 새 백업 필요";
    if(!exported.has(file.getName()))return "마지막 전체 ZIP 이후 새 GPS 기록 있음 · 새 전체 백업 권장";
    JSONObject expected=details.optJSONObject(file.getName());
    if(expected==null||!expected.has("size")||!expected.has("modified"))
     return "GPS 백업 정보 불완전 · 새 전체 백업 권장";
    if(file.length()!=expected.optLong("size",-1)||file.lastModified()!=expected.optLong("modified",-1))
     return "전체 ZIP 이후 GPS 원본이 변경됨 · 새 전체 백업 권장";
    checked++;
   }
   if(checked!=exported.length())return "전체 ZIP 이후 GPS 원본이 삭제됨 · 백업 재확인 필요";
   return "DB·GPS 목록 및 파일 정보 일치 · 정확한 내용은 GPS 해시 검사에서 확인";
  }catch(org.json.JSONException invalid){return "GPS 백업 검사 정보 손상 · 새 전체 백업 권장";}
 }
 /** Full GPS SHA-256 check, call only from a background worker while no ride is recording. */
 static String fullGpsDeepStatus(Context context,String currentJson){
  SharedPreferences prefs=context.getSharedPreferences(PREFS,0);
  String raw=prefs.getString("full_track_names",null);
  String expectedDb=prefs.getString("full_db_sha256","");
  if(raw==null||expectedDb==null||!expectedDb.matches("[a-f0-9]{64}"))
   return "검증된 전체 ZIP의 GPS 해시 기록이 없습니다. 전체 ZIP을 다시 만들어 주세요.";
  try{
   if(currentJson==null||!expectedDb.equals(digest(currentJson)))
    return "전체 ZIP 이후 DB 내용이 변경됐습니다. 새 전체 백업을 만들어 주세요.";
   JSONObject exported=new JSONObject(raw);
   String fullSidecarsRaw=prefs.getString("full_sidecar_sha256",null);
   if(fullSidecarsRaw==null||fullSidecarsRaw.isEmpty())
    return "이전 전체 ZIP의 GPS 부속 파일 검증 기록이 없습니다. 새 전체 백업이 필요합니다.";
   JSONObject savedSidecars=new JSONObject(fullSidecarsRaw);
   File rides=new File(context.getFilesDir(),"rides");
   if(rides.exists()&&!rides.isDirectory())return "GPS 폴더를 읽지 못했습니다.";
   File[] local=rides.isDirectory()?rides.listFiles((dir,name)->name.matches(BackupFormat.TRACK)):new File[0];
   if(local==null)return "GPS 파일 목록을 읽지 못했습니다.";
   int verified=0;
   for(File file:local){
    if(file.length()==0&&noCompletionMarker(file))continue;
    if(java.nio.file.Files.isSymbolicLink(file.toPath())||!file.isFile())
     return "안전하지 않거나 읽을 수 없는 GPS 원본이 있습니다.";
    String expected=exported.optString(file.getName(),"");
    if(!expected.matches("[a-f0-9]{64}"))return "전체 ZIP 이후 추가된 GPS 원본이 있습니다. 새 백업을 만들어 주세요.";
    if(file.length()>TrackFile.MAX_BYTES)return "GPS 원본 크기 제한 초과 · 검사 중단";
    java.security.MessageDigest digest=java.security.MessageDigest.getInstance("SHA-256");
    try(java.io.InputStream input=new java.io.BufferedInputStream(new java.io.FileInputStream(file))){
     byte[] buffer=new byte[32768];int read;
     while((read=input.read(buffer))!=-1)digest.update(buffer,0,read);
    }
    StringBuilder actual=new StringBuilder(64);
    for(byte b:digest.digest())actual.append(String.format(java.util.Locale.ROOT,"%02x",b&0xff));
    if(!expected.equals(actual.toString()))return "전체 ZIP 이후 GPS 원본의 내용이 변경됐습니다. 새 전체 백업이 필요합니다.";
    JSONObject tracked=savedSidecars.optJSONObject(file.getName());
    if(tracked==null)return "전체 ZIP의 GPS 부속 파일 목록이 불완전합니다. 다시 백업해 주세요.";
    for(String suffix:RideTrash.SIDECARS){
     File sidecar=new File(file.getPath()+suffix);
     String expectedSidecar=tracked.optString(suffix,"");
     if(!expectedSidecar.isEmpty()&&!expectedSidecar.matches("[a-f0-9]{64}"))
      return "GPS 부속 파일 검증값이 잘못됐습니다: "+suffix;
     boolean exists=java.nio.file.Files.exists(sidecar.toPath(),java.nio.file.LinkOption.NOFOLLOW_LINKS);
     if(!exists){
      if(!expectedSidecar.isEmpty())return "전체 ZIP 이후 GPS 부속 파일이 삭제됐습니다: "+suffix;
      continue;
     }
     if(java.nio.file.Files.isSymbolicLink(sidecar.toPath())||
       !java.nio.file.Files.isRegularFile(sidecar.toPath(),java.nio.file.LinkOption.NOFOLLOW_LINKS))
      return "안전하지 않은 GPS 부속 파일이 있습니다: "+suffix;
     if(expectedSidecar.isEmpty())return "전체 ZIP 이후 GPS 부속 파일이 추가됐습니다: "+suffix;
     long max=suffix.equals(".mergedinto")?100:suffix.equals(".bike")||suffix.equals(".category")?64:0;
     if(sidecar.length()>max)return "GPS 부속 파일 크기 초과: "+suffix;
     if(!expectedSidecar.equals(digestBytes(java.nio.file.Files.readAllBytes(sidecar.toPath()))))
      return "전체 ZIP 이후 GPS 부속 파일 내용이 변경됐습니다: "+suffix;
    }
    verified++;
   }
   if(verified!=exported.length())return "전체 ZIP 이후 GPS 원본이 삭제됐습니다. 백업을 확인해 주세요.";
   return "전체 ZIP과 현재 DB·GPS 원본 및 부속 파일 "+verified+"건의 SHA-256이 일치합니다. ZIP 외부 보관은 별도 확인하세요.";
  }catch(Exception error){return "GPS 원본 해시 검사 실패 · 원본 기록은 변경하지 않았습니다.";}
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
