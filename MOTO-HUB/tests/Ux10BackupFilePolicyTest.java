package com.motohub.next;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** No local snapshot must alias user files outside the app private directory. */
public final class Ux10BackupFilePolicyTest {
 private static int checks;
 private static void check(boolean success,String message){checks++;if(!success)throw new AssertionError(message);}
 private static void reject(File file,String message)throws Exception{
  try{LocalBackupFilePolicy.requireReadable(file);throw new AssertionError(message);}
  catch(java.io.IOException expected){checks++;}
 }
 public static void main(String[] args)throws Exception{
  Path root=Files.createTempDirectory("moto-ux10-backups-");
  Path outside=Files.createTempFile("moto-ux10-outside-", ".json");
  try{
   File good=root.resolve("motohub-2026-10-20.json").toFile();
   Files.writeString(good.toPath(),"{\"bikes\":[]}",StandardCharsets.UTF_8);
   check(LocalBackupFilePolicy.safeFolder(root.toFile()),"private recovery folder");
   check(LocalBackupFilePolicy.readable(good),"small valid backup is readable");
   LocalBackupFilePolicy.requireReadable(good);checks++;
   File missing=root.resolve("not-found.json").toFile();
   reject(missing,"missing file accepted");
   File empty=root.resolve("motohub-empty.json").toFile();
   Files.createFile(empty.toPath());reject(empty,"empty file accepted");
   File tooLarge=root.resolve("motohub-large.json").toFile();
   try(java.io.RandomAccessFile f=new java.io.RandomAccessFile(tooLarge,"rw")){
    f.setLength(LocalBackupFilePolicy.MAX_BYTES+1);
   }
   reject(tooLarge,"oversized backup accepted");
   check(!LocalBackupFilePolicy.safeFolder(good),"file cannot be a recovery folder");
   try{
    Path shortcut=root.resolve("motohub-2026-10-19.json");
    Files.createSymbolicLink(shortcut,outside);
    check(!LocalBackupFilePolicy.readable(shortcut.toFile()),"external symlink ignored");
    reject(shortcut.toFile(),"linked backup accepted");
    Path folderLink=root.resolve("linked-updates");
    Files.createSymbolicLink(folderLink,root);
    check(!LocalBackupFilePolicy.safeFolder(folderLink.toFile()),"linked folder rejected");
    Files.delete(folderLink);Files.delete(shortcut);
   }catch(UnsupportedOperationException|SecurityException ignored){/* Not all JVM filesystems allow symlinks. */}
  }finally{
   File[] all=root.toFile().listFiles();
   if(all!=null)for(File f:all)Files.deleteIfExists(f.toPath());
   Files.deleteIfExists(root);Files.deleteIfExists(outside);
  }
  System.out.println("PASS UX10 local backup path policy: "+checks+" checks");
 }
}
