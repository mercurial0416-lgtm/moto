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
 static void requireReadable(File file)throws IOException{
  if(!readable(file))throw new IOException("안전하지 않거나 손상된 로컬 복원 파일입니다.");
 }
}
