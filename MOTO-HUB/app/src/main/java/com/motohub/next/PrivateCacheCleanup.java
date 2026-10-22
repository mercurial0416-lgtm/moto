package com.motohub.next;
import java.io.*;
import java.nio.file.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Cleanup only our private staging artifacts. Never touch user GPS or rollback backups. */
final class PrivateCacheCleanup {
 static final AtomicBoolean ONCE=new AtomicBoolean(false);
 static final long STAGE_EXPIRY_MS=6L*60*60*1000;
 private PrivateCacheCleanup(){}
 static int atProcessStart(File cache,File files,long now){
  if(!ONCE.compareAndSet(false,true))return 0;
  return cleanup(cache,files,now);
 }
 static int cleanup(File cache,File files,long now){
  int removed=0;
  if(cache!=null&&cache.isDirectory()){
   File[] candidates=cache.listFiles();
   if(candidates!=null)for(File item:candidates){
    // These ZIPs are plaintext created ONLY by a completed or aborted decrypt.
    if(item.getName().matches("moto-secure-[A-Za-z0-9_-]+\\.zip") && safeFile(item,cache)){
     try{if(Files.deleteIfExists(item.toPath()))removed++;}catch(IOException ignored){}
    }
   }
  }
  if(files!=null&&files.isDirectory()){
   File[] candidates=files.listFiles();
   if(candidates!=null)for(File item:candidates){
    String name=item.getName();
    if(!name.matches("moto-stage-[0-9a-fA-F-]{36}"))continue;
    if(now<item.lastModified()||now-item.lastModified()<STAGE_EXPIRY_MS)continue;
    if(!safeDirectory(item,files))continue;
    // Only recognized stage contents; never delete the rollback directory.
    if(cleanStage(item))removed++;
   }
  }
  return removed;
 }
 private static boolean safeFile(File file,File parent){
  return !Files.isSymbolicLink(file.toPath())&&Files.isRegularFile(file.toPath(),LinkOption.NOFOLLOW_LINKS)
   &&file.getParentFile().equals(parent);
 }
 private static boolean safeDirectory(File item,File parent){
  return !Files.isSymbolicLink(item.toPath())&&Files.isDirectory(item.toPath(),LinkOption.NOFOLLOW_LINKS)
   &&item.getParentFile().equals(parent);
 }
 private static boolean cleanStage(File dir){
  File[] entries=dir.listFiles();if(entries==null)return false;
  // Staging can hold a rides subfolder and restored bike/receipt photos.
  for(File entry:entries){
   if(Files.isSymbolicLink(entry.toPath()))return false;
   if(entry.isDirectory()){
    if(!entry.getName().equals("rides"))return false;
    File[] tracks=entry.listFiles();if(tracks==null)return false;
    for(File f:tracks)if(!safeFile(f,entry)||!recognizedTrack(f.getName()))return false;
   }else if(!safeFile(entry,dir)||!(entry.getName().matches(BackupFormat.PHOTO)||
       entry.getName().matches(BackupFormat.RECEIPT)))return false;
  }
  for(File entry:entries){
   try{
    if(entry.isDirectory()){
     File[] tracks=entry.listFiles();if(tracks!=null)for(File f:tracks)Files.deleteIfExists(f.toPath());
    }
    if(!entry.isDirectory())Files.deleteIfExists(entry.toPath());
   }catch(IOException e){return false;}
   if(entry.isDirectory())try{Files.deleteIfExists(entry.toPath());}catch(IOException e){return false;}
  }
  try{return Files.deleteIfExists(dir.toPath());}catch(IOException e){return false;}
 }
 private static boolean recognizedTrack(String name){return name.matches(BackupFormat.TRACK+"(\\.(done|imported|recovered|mergedinto|unmerged|category|bike))?");}
}
