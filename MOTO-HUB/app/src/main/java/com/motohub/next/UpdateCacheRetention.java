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
 /** Preserve the active fragment but discard other stale hash-labelled fragments only. */
 static int pruneStalePartials(File directory,String activeSha,long nowMillis){
  if(directory==null||!directory.isDirectory()||Files.isSymbolicLink(directory.toPath())||
    activeSha==null||!activeSha.matches("[a-f0-9]{64}"))return 0;
  File[] files=directory.listFiles();if(files==null)return 0;int removed=0;
  for(File file:files){
   if(!file.getName().matches("[a-f0-9]{64}\\.part\\.apk")||
      file.getName().equals(activeSha+".part.apk")||
      !file.getParentFile().equals(directory)||
      Files.isSymbolicLink(file.toPath())||
      !Files.isRegularFile(file.toPath(),LinkOption.NOFOLLOW_LINKS))continue;
   long modified=file.lastModified();
   if(modified<=0||modified>nowMillis||nowMillis-modified<7L*24*60*60*1000)continue;
   try{if(Files.deleteIfExists(file.toPath()))removed++;}catch(IOException ignored){}
  }
  return removed;
 }
}
