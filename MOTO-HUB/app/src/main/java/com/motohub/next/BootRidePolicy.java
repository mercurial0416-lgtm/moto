package com.motohub.next;
import java.io.File;
import java.io.FileInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
/** Pure policy: notifications are only offered after boot with explicit notification permission. */
final class BootRidePolicy {
 static boolean shouldOffer(boolean boot,boolean notificationsAllowed,boolean unfinishedTrack){
  return boot && notificationsAllowed && unfinishedTrack;
 }
 /** Fast metadata-only scan for boot receiver's short execution budget. Full verification happens in the app. */
 static boolean hasRecoverable(File filesDir){
  if(filesDir==null)return false;
  File[] entries=new File(filesDir,"rides").listFiles((dir,name)->name.endsWith(".track"));
  if(entries==null)return false;
  for(File file:entries){
   if(!file.isFile()||!file.getName().matches("[0-9]{10,15}-[a-f0-9-]{36}\\.track"))continue;
   if(file.length()==0||file.length()>25_000_000L)continue;
   if(new File(file.getPath()+".done").exists()||new File(file.getPath()+".imported").exists()||new File(file.getPath()+".recovered").exists())continue;
   if(hasCommittedPoint(file))return true;
  }
  return false;
 }
 /** Bounded read on boot: no false recovery notice for zero complete GPS rows. */
 static boolean hasCommittedPoint(File track){
  try(FileInputStream input=new FileInputStream(track)){
   ByteArrayOutputStream line=new ByteArrayOutputStream();
   int c;
   while(line.size()<=256&&(c=input.read())!=-1){
    if(c=='\n'){
     String[] fields=new String(line.toByteArray(),StandardCharsets.UTF_8).split(",",-1);
     if(fields.length!=4)return false;
     return TrackFile.valid(Double.parseDouble(fields[0]),Double.parseDouble(fields[1]),
        Double.parseDouble(fields[2]),Long.parseLong(fields[3]));
    }
    line.write(c);
   }
  }catch(Exception ignored){/* No data changes in recovery discovery. */}
  return false;
 }
 private BootRidePolicy(){}
}
