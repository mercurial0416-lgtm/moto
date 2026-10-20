package com.motohub.next;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;

/** Private, bounded, non-symlink JSON restore points; never follow outside-app aliases. */
final class LocalBackupFilePolicy {
 static final long MAX_BYTES=8_000_000L;
 private LocalBackupFilePolicy(){}
 static boolean safeFolder(File folder){
  return folder!=null && Files.isDirectory(folder.toPath(),LinkOption.NOFOLLOW_LINKS)
   && !Files.isSymbolicLink(folder.toPath());
 }
 static boolean readable(File file){
  return file!=null && Files.isRegularFile(file.toPath(),LinkOption.NOFOLLOW_LINKS)
   && !Files.isSymbolicLink(file.toPath())
   && file.length()>0 && file.length()<=MAX_BYTES;
 }
 /** Only truly usable local files may count toward the retention quota.
  *  Never let a linked, empty or oversized fake checkpoint evict real backups.
  */
 static File[] eligible(File folder,String namePattern){
  if(!safeFolder(folder)||namePattern==null)return new File[0];
  File[] files=folder.listFiles((parent,name)->name.matches(namePattern)
    &&readable(new File(parent,name)));
  return files==null?new File[0]:files;
 }
 static void requireReadable(File file)throws IOException{
  if(!readable(file))throw new IOException("안전하지 않거나 손상된 로컬 복원 파일입니다.");
 }
}
