package com.motohub.next;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/** Invalid local backup lookalikes must not evict real recovery points. */
public final class Ux11BackupRetentionTest {
 static int checks;
 static void yes(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
 static void write(Path root,String name,String json)throws Exception{
  Files.writeString(root.resolve(name),json,StandardCharsets.UTF_8);
 }
 public static void main(String[] args)throws Exception{
  Path root=Files.createTempDirectory("moto-ux11-retain-");
  Path outsider=Files.createTempFile("moto-user-outside-", ".json");
  String day="motohub-[0-9]{4}-[0-9]{2}-[0-9]{2}\\.json";
  String update="motohub-[0-9]{13}-[a-f0-9]{8}\\.json";
  try{
   write(root,"motohub-2026-10-20.json","{\"bikes\":[]}");
   write(root,"motohub-2026-10-19.json","");
   write(root,"motohub-2026-10-18.json","{\"bikes\":[1]}");
   write(root,"unrelated.json","{\"bikes\":[]}");
   File large=root.resolve("motohub-2026-10-17.json").toFile();
   try(java.io.RandomAccessFile raf=new java.io.RandomAccessFile(large,"rw")){
    raf.setLength(LocalBackupFilePolicy.MAX_BYTES+1);
   }
   File[] daily=LocalBackupFilePolicy.eligible(root.toFile(),day);
   yes(daily.length==2,"invalid date-labelled files excluded from seven-day quota");
   yes(Arrays.stream(daily).allMatch(LocalBackupFilePolicy::readable),"only safe snapshots counted");
   yes(Arrays.stream(daily).noneMatch(f->f.getName().equals("motohub-2026-10-19.json")),"empty backup excluded");
   write(root,"motohub-1792500000000-1234abcd.json","{\"bikes\":[]}");
   write(root,"motohub-1792500000001-1234abcd.json","");
   yes(LocalBackupFilePolicy.eligible(root.toFile(),update).length==1,"checkpoint quota ignores empty file");
   try{
    Files.createSymbolicLink(root.resolve("motohub-2026-10-16.json"),outsider);
    yes(LocalBackupFilePolicy.eligible(root.toFile(),day).length==2,"external link does not count");
    Path alias=Files.createTempDirectory("moto-ux11-root-");
    try{
     Files.createSymbolicLink(alias.resolve("updates"),root);
     yes(LocalBackupFilePolicy.eligible(alias.resolve("updates").toFile(),day).length==0,"linked backup folder not read");
    }finally{Files.deleteIfExists(alias.resolve("updates"));Files.deleteIfExists(alias);}
   }catch(UnsupportedOperationException|SecurityException ignored){/* Unsupported filesystem. */}
   yes(LocalBackupFilePolicy.eligible(root.toFile(),null).length==0,"bad pattern fails closed");
  }finally{
   File[] all=root.toFile().listFiles();
   if(all!=null)for(File f:all)Files.deleteIfExists(f.toPath());
   Files.deleteIfExists(root);Files.deleteIfExists(outsider);
  }
  System.out.println("PASS UX11 safe backup retention candidates: "+checks+" checks");
 }
}
