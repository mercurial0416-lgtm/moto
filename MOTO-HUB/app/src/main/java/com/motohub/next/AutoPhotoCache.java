package com.motohub.next;
import java.io.*;
import java.nio.file.*;
import java.util.*;
/** Bounded cleanup for re-downloadable model pictures, not the user's chosen photos. */
final class AutoPhotoCache {
 static final long LIMIT_BYTES=64L*1024*1024;
 static final long MIN_AGE_MS=7L*24*60*60*1000;
 private AutoPhotoCache(){}
 static int trim(File folder,long now){
  if(folder==null||!folder.isDirectory()||Files.isSymbolicLink(folder.toPath()))return 0;
  File[] all=folder.listFiles();if(all==null)return 0;
  List<File> auto=new ArrayList<>();long total=0;
  for(File photo:all){
   if(!photo.getName().matches("[a-f0-9]{24}\\.jpg")||!plainFile(photo,folder))continue;
   File meta=new File(folder,photo.getName()+".json");if(!plainFile(meta,folder)||meta.length()>8192||meta.length()<10)continue;
   String json;try{json=new String(Files.readAllBytes(meta.toPath()),java.nio.charset.StandardCharsets.UTF_8);}catch(Exception e){continue;}
   // Fail closed on missing/unknown metadata; never delete manually chosen images.
   if(!json.contains("\"imageUrl\"")||!json.contains("\"pageUrl\"")||!json.contains("\"exactYearCandidate\"") ||
    json.matches("(?s).*\"userSelected\"\\s*:\\s*true.*"))continue;
   auto.add(photo);total+=Math.max(0,photo.length());
  }
  if(total<=LIMIT_BYTES)return 0;
  auto.sort(Comparator.comparingLong(File::lastModified));int removed=0;
  for(File photo:auto){
   if(total<=LIMIT_BYTES)break;
   if(photo.lastModified()>now||now-photo.lastModified()<MIN_AGE_MS)continue;
   File meta=new File(folder,photo.getName()+".json");
   if(!plainFile(photo,folder)||!plainFile(meta,folder))continue;
   long size=photo.length();try{Files.delete(photo.toPath());Files.deleteIfExists(meta.toPath());total-=size;removed++;}catch(IOException ignored){}
  }
  return removed;
 }
 private static boolean plainFile(File file,File parent){
  return file.getParentFile().equals(parent)&&Files.isRegularFile(file.toPath(),LinkOption.NOFOLLOW_LINKS)&&!Files.isSymbolicLink(file.toPath());
 }
}
