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
 static File[] available(Context context){
  File folder=new File(context.getFilesDir(),"local-safety-backup");
  File[] files=folder.listFiles((dir,name)->name.matches("motohub-[0-9]{4}-[0-9]{2}-[0-9]{2}\\.json"));
  if(files==null)return new File[0];
  java.util.Arrays.sort(files,(a,b)->b.getName().compareTo(a.getName()));
  return files;
 }
 static JSONObject read(File file)throws Exception{
  if(!file.isFile()||file.length()>8_000_000)throw new java.io.IOException("백업 파일 오류");
  byte[] bytes=java.nio.file.Files.readAllBytes(file.toPath());
  return Store.parse(new String(bytes,StandardCharsets.UTF_8));
 }
 static void snapshot(Context context,JSONObject data){
  try{
   File folder=new File(context.getFilesDir(),"local-safety-backup");
   if(!folder.exists()&&!folder.mkdirs())return;
   String day=java.time.LocalDate.now(java.time.ZoneId.of("Asia/Seoul")).toString();
   File destination=new File(folder,"motohub-"+day+".json");
   // Update the snapshot after each successful write.
   byte[] bytes=data.toString().getBytes(StandardCharsets.UTF_8);
   if(bytes.length>8_000_000)return;
   // Do not overwrite the only pre-edit snapshot of the day.
   // Daily snapshots are immutable; the latest state remains in the primary database.
   if(destination.isFile())return;
   new JSONObject(new String(bytes,StandardCharsets.UTF_8));
   AtomicFile atomic=new AtomicFile(destination);FileOutputStream out=null;
   boolean written=false;
   try{
    out=atomic.startWrite();out.write(bytes);atomic.finishWrite(out);out=null;
    // Verify the persisted snapshot before rotating older recovery points.
    read(destination); // Validate the backup through the normal parser.
    written=java.util.Arrays.equals(bytes,java.nio.file.Files.readAllBytes(destination.toPath()));
   }catch(Exception e){if(out!=null)atomic.failWrite(out);}
   if(!written){
    // A failed or unverifiable snapshot must not trigger deletion of older backups.
    destination.delete();
    return;
   }
   File[] snapshots=folder.listFiles((dir,name)->name.matches("motohub-[0-9]{4}-[0-9]{2}-[0-9]{2}\\.json"));
   if(snapshots!=null&&snapshots.length>7){
    java.util.Arrays.sort(snapshots,(a,b)->a.getName().compareTo(b.getName()));
    for(int i=0;i<snapshots.length-7;i++)snapshots[i].delete();
   }
  }catch(Exception ignored){}
 }
}
