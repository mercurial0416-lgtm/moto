package com.motohub.next;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
/** Keep only the verified, active installer APK; never touches local records or arbitrary files. */
final class UpdateCacheRetention {
 private UpdateCacheRetention(){}
 static int prune(File directory,String activeSha){
  if(directory==null||!directory.isDirectory()||Files.isSymbolicLink(directory.toPath())||
    activeSha==null||!activeSha.matches("[0-9a-f]{64}"))return 0;
  File[] files=directory.listFiles();if(files==null)return 0;int removed=0;
  for(File f:files){
   if(!f.getName().matches("[0-9a-f]{64}\\.apk")||f.getName().equals(activeSha+".apk")||
      !f.getParentFile().equals(directory)||Files.isSymbolicLink(f.toPath())||
      !Files.isRegularFile(f.toPath(),LinkOption.NOFOLLOW_LINKS))continue;
   try{if(Files.deleteIfExists(f.toPath()))removed++;}catch(IOException ignored){}
  }
  return removed;
 }
}
