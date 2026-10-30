package com.motohub.next;
import android.content.Context;
import android.util.AtomicFile;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;
import org.json.JSONObject;

/** Verifiable private JSON recovery point before the Android package installer runs. */
final class UpdateInstallSnapshot {
 private static final int LIMIT=8_000_000;
 private static final String PREFS="update_install_audit";
 private UpdateInstallSnapshot(){}

 static File create(Context context,AtomicFile active)throws Exception{
  // A brand-new empty installation may not have written its initial JSON yet.
  File base=active.getBaseFile();
  byte[] data=(base.isFile()||new File(base.getPath()+".bak").isFile())?
   active.readFully():Store.empty().toString().getBytes(StandardCharsets.UTF_8);
  if(data.length<2||data.length>LIMIT)throw new IOException("현재 앱 데이터 크기 또는 저장 상태를 확인할 수 없습니다.");
  Store.parse(new String(data,StandardCharsets.UTF_8));
  File directory=new File(new File(context.getFilesDir(),"local-safety-backup"),"updates");
  if(!directory.isDirectory()&&!directory.mkdirs())throw new IOException("업데이트 복구 지점 폴더를 만들지 못했습니다.");
  if(!LocalBackupFilePolicy.safeFolder(directory))throw new IOException("복구 지점 경로가 안전하지 않습니다.");
  File target=new File(directory,"motohub-"+System.currentTimeMillis()+"-"+UUID.randomUUID().toString().substring(0,8)+".json");
  AtomicFile atomic=new AtomicFile(target);
  FileOutputStream stream=null;
  try{
   stream=atomic.startWrite();
   stream.write(data);atomic.finishWrite(stream);stream=null;
   LocalSafetyBackup.read(target);
   if(!Arrays.equals(data,LocalBackupFilePolicy.readBounded(target)))
    throw new IOException("복구 지점 저장 후 검증에 실패했습니다.");
   return target;
  }catch(Exception failure){
   if(stream!=null)atomic.failWrite(stream);
   target.delete();throw failure;
  }
 }

 static void recordInstaller(Context context,int targetVersion,File recovery)throws IOException{
  if(recovery==null||!recovery.isFile()||!LocalSafetyBackup.verified(recovery))
   throw new IOException("설치 전 복구 지점 검증에 실패했습니다.");
  String baseline;
  try{baseline=UpdateDataFootprint.capture(context,LocalSafetyBackup.read(recovery));}
  catch(Exception e){throw new IOException("업데이트 전 GPS·사진 무결성 검사 실패",e);}
  if(!context.getSharedPreferences(PREFS,0).edit()
      .putInt("pending_version",targetVersion)
      .putString("recovery_file",recovery.getName())
      .putString("before_footprint",baseline)
      .commit())throw new IOException("복구 지점 정보 저장에 실패했습니다.");
 }

 /** A recovery point selected for update/last validated install is not subject to rotation. */
 static String lastReport(Context context){
  return context.getSharedPreferences(PREFS,0).getString("last_message",
   "이 기기에서 아직 앱 업데이트 후 데이터 비교 기록이 없습니다.");
 }
 static boolean lastProblem(Context context){return context.getSharedPreferences(PREFS,0).getBoolean("last_problem",false);}
 static boolean isPinned(Context context,String filename){
  android.content.SharedPreferences prefs=context.getSharedPreferences(PREFS,0);
  return UpdateRecoveryPolicy.isPinned(filename,
   prefs.getString("recovery_file",""),prefs.getString("last_good_snapshot",""));
 }
 /** Runs only after MainActivity has parsed the existing durable Store. */
 static String afterStartup(Context context,int installedVersion,boolean dataLoaded){
  android.content.SharedPreferences prefs=context.getSharedPreferences(PREFS,0);
  int target=prefs.getInt("pending_version",0);
  if(target<=0||installedVersion<target)return null;
  if(!dataLoaded){String error="업데이트 후 JSON 데이터를 읽을 수 없습니다. 원본 보호 중입니다.";prefs.edit().putString("last_message",error).putBoolean("last_problem",true).commit();return error;}
  String filename=prefs.getString("recovery_file","");
  File source=new File(new File(new File(context.getFilesDir(),"local-safety-backup"),"updates"),filename);
  boolean snapshotExists=filename.matches("motohub-[0-9]{13}-[a-f0-9]{8}\\.json")&&LocalSafetyBackup.verified(source);
  android.content.SharedPreferences.Editor audit=prefs.edit()
   .remove("pending_version").remove("recovery_file");
  if(snapshotExists)audit.putString("last_good_snapshot",filename);
  String original=prefs.getString("before_footprint","");
  boolean match=true;
  String changed="";
  if(!original.isEmpty()){
   try{
    android.util.AtomicFile live=new android.util.AtomicFile(new File(context.getFilesDir(),"motohub-v3.json"));
    JSONObject current=Store.parse(new String(live.readFully(),StandardCharsets.UTF_8));
    // An old installed APK writes a schema4 baseline before the new APK launches.
    // Compare it using the original scope to avoid a false data-loss warning.
    String after;
    if(original.startsWith("schema4;"))after=UpdateDataFootprint.captureLegacy(context,current);
    else if(original.startsWith("schema5;"))after=UpdateDataFootprint.capture(context,current);
    else throw new IOException("알 수 없는 업데이트 검사 형식");
    match=original.equals(after);
    if(!match)changed=UpdateFootprintDiff.describe(original,after);
   }catch(Exception e){match=false;changed="저장 데이터 또는 GPS·사진 파일 읽기 실패";}
  }
  String report=!match?
   "업데이트 검사 주의: "+changed+"\n기존 복구 지점은 보존합니다. 자동으로 기존 기록을 덮어쓰지 않습니다.":
   snapshotExists?"업데이트 전후 기록 검사 통과 · 복구 지점 보관 중":
   "현재 데이터는 읽었으나 설치 전 복구 지점을 찾지 못했습니다. 전체 ZIP 백업을 권장합니다.";
  audit.remove("before_footprint").putString("last_message",report)
   .putBoolean("last_problem",!match||!snapshotExists)
   .putInt("last_checked_version",installedVersion);
  if(!audit.commit())return "데이터는 읽었지만 업데이트 완료 기록을 저장하지 못했습니다.";
  return report;
 }
}
