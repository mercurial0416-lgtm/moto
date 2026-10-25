package com.motohub.next;
import android.content.Context;
import android.util.AtomicFile;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;

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
  if(!context.getSharedPreferences(PREFS,0).edit()
      .putInt("pending_version",targetVersion)
      .putString("recovery_file",recovery.getName())
      .commit())throw new IOException("복구 지점 정보 저장에 실패했습니다.");
 }

 /** Runs only after MainActivity has parsed the existing durable Store. */
 static String afterStartup(Context context,int installedVersion,boolean dataLoaded){
  android.content.SharedPreferences prefs=context.getSharedPreferences(PREFS,0);
  int target=prefs.getInt("pending_version",0);
  if(target<=0||installedVersion<target)return null;
  if(!dataLoaded)return "업데이트 후 저장 데이터 확인에 실패했습니다. 기존 백업을 보존하고 쓰기 작업을 차단합니다.";
  String filename=prefs.getString("recovery_file","");
  File source=new File(new File(new File(context.getFilesDir(),"local-safety-backup"),"updates"),filename);
  boolean snapshotExists=filename.matches("motohub-[0-9]{13}-[a-f0-9]{8}\\.json")&&LocalSafetyBackup.verified(source);
  if(!prefs.edit().remove("pending_version").remove("recovery_file").commit())
   return "데이터는 읽었지만 업데이트 완료 기록을 저장하지 못했습니다.";
  return snapshotExists?
   "앱 업데이트 후 저장 데이터 확인 완료 · 설치 전 복구 지점 보관 중":
   "앱 데이터는 읽었지만 설치 전 복구 지점을 찾지 못했습니다. 전체 백업을 권장합니다.";
 }
}
