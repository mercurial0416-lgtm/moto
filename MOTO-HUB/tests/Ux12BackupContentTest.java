package com.motohub.next;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/** Reject syntactically valid but structurally unusable local JSON checkpoints. */
public final class Ux12BackupContentTest {
 static int checks;
 static void yes(boolean condition,String message){checks++;if(!condition)throw new AssertionError(message);}
 static boolean valid(File file){
  try{Store.parse(Files.readString(file.toPath(),StandardCharsets.UTF_8));return true;}
  catch(Exception failure){return false;}
 }
 static void put(Path dir,String filename,String data)throws Exception{
  Files.writeString(dir.resolve(filename),data,StandardCharsets.UTF_8);
 }
 public static void main(String[] args)throws Exception{
  Path dir=Files.createTempDirectory("moto-ux12-backup-");
  Path external=Files.createTempFile("moto-outside-app-", ".json");
  String pattern="motohub-[0-9]{4}-[0-9]{2}-[0-9]{2}\\.json";
  try{
   String valid=Store.empty().toString();
   put(dir,"motohub-2026-10-21.json",valid);
   put(dir,"motohub-2026-10-20.json",valid.substring(0,valid.length()/2));
   put(dir,"motohub-2026-10-19.json",Store.empty().put("vehicles","not-an-array").toString());
   put(dir,"motohub-2026-10-18.json",Store.empty().put("version",999).toString());
   put(dir,"motohub-2026-10-17.json",valid);
   put(dir,"motohub-2026-10-16.json","");
   File[] formatOnly=LocalBackupFilePolicy.eligible(dir.toFile(),pattern);
   yes(formatOnly.length==5,"shape-only filter must surface intentionally corrupted entries for comparison");
   File[] verified=LocalBackupFilePolicy.eligible(dir.toFile(),pattern,Ux12BackupContentTest::valid);
   yes(verified.length==2,"only real parsable Store backups should count");
   yes(Arrays.stream(verified).allMatch(Ux12BackupContentTest::valid),"every selected backup reparses correctly");
   yes(Arrays.stream(verified).noneMatch(f->f.getName().endsWith("20.json")),"partial JSON cannot evict complete snapshots");
   yes(Arrays.stream(verified).noneMatch(f->f.getName().endsWith("19.json")),"wrong collection schema excluded");
   yes(LocalBackupFilePolicy.eligible(dir.toFile(),pattern,f->{throw new IllegalArgumentException("corrupt");}).length==0,"validation failures are fail-closed");
   yes(LocalBackupFilePolicy.eligible(dir.toFile(),pattern,null).length==0,"null validator is fail-closed");
   try{
    Files.writeString(external,valid,StandardCharsets.UTF_8);
    Files.createSymbolicLink(dir.resolve("motohub-2026-10-15.json"),external);
    yes(LocalBackupFilePolicy.eligible(dir.toFile(),pattern,Ux12BackupContentTest::valid).length==2,"symlink to valid external JSON still excluded");
   }catch(UnsupportedOperationException|SecurityException ignored){/* host does not support symlinks */}
  }finally{
   File[] files=dir.toFile().listFiles();
   if(files!=null)for(File f:files)Files.deleteIfExists(f.toPath());
   Files.deleteIfExists(dir);Files.deleteIfExists(external);
  }
  System.out.println("PASS UX12 validated backup retention: "+checks+" checks");
 }
}
