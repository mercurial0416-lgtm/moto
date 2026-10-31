package com.motohub.next;
import android.content.Context;
import android.util.AtomicFile;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
/** Local safety snapshots. Never sent to a server. */
final class LocalSafetyBackup {
 private LocalSafetyBackup(){}
 /** A filename and size alone cannot prove a usable restore point. */
 static boolean verified(File file){
  // UI/retention previews can reuse recent results. Actual restore ALWAYS
  // goes through read(file) and parses the entire original again.
  return LocalBackupValidationCache.verify(file,()->{
   try{read(file);return true;}catch(Exception ignored){return false;}
  });
 }
 static File[] available(Context context){
  File folder=new File(context.getFilesDir(),"local-safety-backup");
  File[] files=LocalBackupFilePolicy.safeFolder(folder)?folder.listFiles((dir,name)->name.matches("motohub-[0-9]{4}-[0-9]{2}-[0-9]{2}\\.json") && verified(new File(dir,name))):null;
  if(files==null)return new File[0];
  java.util.Arrays.sort(files,(a,b)->b.getName().compareTo(a.getName()));
  return files;
 }
 /** Keep the immutable pre-edit daily backup AND a bounded list of post-edit restore points.
  * This protects several edits made on the same day without uploading private data. */
 static File[] recoveryPoints(Context context){
  java.util.ArrayList<File> result=new java.util.ArrayList<>(java.util.Arrays.asList(available(context)));
  File folder=new File(new File(context.getFilesDir(),"local-safety-backup"),"updates");
  File[] updates=LocalBackupFilePolicy.safeFolder(folder)?folder.listFiles((dir,name)->name.matches("motohub-[0-9]{13}-[a-f0-9]{8}\\.json") && verified(new File(dir,name))):null;
  if(updates!=null)java.util.Collections.addAll(result,updates);
  result.sort((a,b)->{int date=Long.compare(b.lastModified(),a.lastModified());return date!=0?date:b.getName().compareTo(a.getName());});
  return result.toArray(new File[0]);
 }
 static boolean checkpoint(Context context,JSONObject data){
  File file=null;FileOutputStream out=null;AtomicFile atomic=null;
  boolean confirmed=false;
  try{
   File folder=new File(new File(context.getFilesDir(),"local-safety-backup"),"updates");
   if(!folder.exists()&&!folder.mkdirs())return false;
   if(!LocalBackupFilePolicy.safeFolder(folder))return false;
   byte[] bytes=data.toString().getBytes(StandardCharsets.UTF_8);
   if(bytes.length>8_000_000)return false;
   // Validate input first. Never rotate a good checkpoint after a failed write.
   Store.parse(new String(bytes,StandardCharsets.UTF_8));
   String suffix=java.util.UUID.randomUUID().toString().substring(0,8);
   file=new File(folder,"motohub-"+System.currentTimeMillis()+"-"+suffix+".json");
   atomic=new AtomicFile(file);out=atomic.startWrite();out.write(bytes);atomic.finishWrite(out);out=null;
   read(file);
   if(!java.util.Arrays.equals(bytes,LocalBackupFilePolicy.readBounded(file)))throw new java.io.IOException("검증되지 않은 복원 지점");
   LocalBackupValidationCache.invalidate(file);
   confirmed=true; // Never erase a verified checkpoint if retention cleanup fails.
   File[] points=LocalBackupFilePolicy.eligible(folder,"motohub-[0-9]{13}-[a-f0-9]{8}\\.json",LocalSafetyBackup::verified);
   if(points!=null&&points.length>12){
    java.util.Arrays.sort(points,(a,b)->{int date=Long.compare(a.lastModified(),b.lastModified());return date!=0?date:a.getName().compareTo(b.getName());});
    // Never rotate the snapshot named by a pending/successful app upgrade.
    // Preserve the newest 12 ordinary checkpoints without counting that pinned copy.
    int toDelete=points.length-12;
    for(File candidate:points){
     if(toDelete<=0)break;
     if(UpdateInstallSnapshot.isPinned(context,candidate.getName()))continue;
     if(candidate.delete())toDelete--;
    }
   }
  }catch(Exception e){
   if(!confirmed){
    if(out!=null&&atomic!=null)atomic.failWrite(out);
    if(file!=null){LocalBackupValidationCache.invalidate(file);file.delete();}
   }
   return confirmed;
  }
  return true;
 }
 /** Differentiate absent backups from unreadable or corrupt recovery points. */
 static String recoveryAvailabilityMessage(Context context){
  File root=new File(context.getFilesDir(),"local-safety-backup");
  File updates=new File(root,"updates");
  boolean candidates=false;
  File[] folders={root,updates};
  String[] patterns={"motohub-[0-9]{4}-[0-9]{2}-[0-9]{2}\\.json",
                     "motohub-[0-9]{13}-[a-f0-9]{8}\\.json"};
  for(int i=0;i<folders.length;i++){
   File folder=folders[i];
   if(!folder.exists()&&!java.nio.file.Files.isSymbolicLink(folder.toPath()))continue;
   if(!LocalBackupFilePolicy.safeFolder(folder))
    return "자동 복원 폴더가 손상되었거나 접근할 수 없습니다. 원본 파일을 지우지 마세요.";
   final String pattern=patterns[i];
   File[] listed=folder.listFiles((dir,name)->name.matches(pattern));
   if(listed==null)return "자동 복원 파일 목록을 읽지 못했습니다. 앱 저장공간 상태를 확인하세요.";
   if(listed.length>0)candidates=true;
  }
  return candidates?"자동 복원 파일이 있지만 안전성 검사를 통과하지 못했습니다. 원본은 보존했습니다.":"자동 복원 파일이 없습니다. 전체 ZIP 백업이 있다면 수동 복원을 사용하세요.";
 }
 static JSONObject read(File file)throws Exception{
  byte[] bytes=LocalBackupFilePolicy.readBounded(file);
  return Store.parse(new String(bytes,StandardCharsets.UTF_8));
 }
 static void snapshot(Context context,JSONObject data){
  try{
   File folder=new File(context.getFilesDir(),"local-safety-backup");
   if(!folder.exists()&&!folder.mkdirs())return;
   if(!LocalBackupFilePolicy.safeFolder(folder))return;
   String day=java.time.LocalDate.now(java.time.ZoneId.of("Asia/Seoul")).toString();
   File destination=new File(folder,"motohub-"+day+".json");
   // Update the snapshot after each successful write.
   byte[] bytes=data.toString().getBytes(StandardCharsets.UTF_8);
   if(bytes.length>8_000_000)return;
   // Do not overwrite the only pre-edit snapshot of the day.
   // Daily snapshots are immutable; the latest state remains in the primary database.
   if(destination.exists()||java.nio.file.Files.isSymbolicLink(destination.toPath()))return;
   Store.parse(new String(bytes,StandardCharsets.UTF_8));
   AtomicFile atomic=new AtomicFile(destination);FileOutputStream out=null;
   boolean written=false;
   try{
    out=atomic.startWrite();out.write(bytes);atomic.finishWrite(out);out=null;
    // Verify the persisted snapshot before rotating older recovery points.
    read(destination); // Validate the backup through the normal parser.
    written=java.util.Arrays.equals(bytes,LocalBackupFilePolicy.readBounded(destination));
    LocalBackupValidationCache.invalidate(destination);
   }catch(Exception e){if(out!=null)atomic.failWrite(out);}
   if(!written){
    // A failed or unverifiable snapshot must not trigger deletion of older backups.
    destination.delete();
    return;
   }
   File[] snapshots=LocalBackupFilePolicy.eligible(folder,"motohub-[0-9]{4}-[0-9]{2}-[0-9]{2}\\.json",LocalSafetyBackup::verified);
   if(snapshots!=null&&snapshots.length>7){
    java.util.Arrays.sort(snapshots,(a,b)->a.getName().compareTo(b.getName()));
    for(int i=0;i<snapshots.length-7;i++)snapshots[i].delete();
   }
  }catch(Exception ignored){}
 }
}
