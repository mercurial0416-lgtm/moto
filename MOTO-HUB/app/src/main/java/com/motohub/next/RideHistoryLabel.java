package com.motohub.next;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Human-readable ride start time. Imported GPX uses the recorded point time, not import time. */
final class RideHistoryLabel {
 private static final DateTimeFormatter FORMAT=DateTimeFormatter.ofPattern("yyyy.MM.dd (E) HH:mm",Locale.KOREAN);
 private RideHistoryLabel(){}

 static String title(File track,ZoneId zone){
  Long start=startTime(track);
  if(start==null)return "날짜 정보 없음";
  try{return FORMAT.withZone(zone).format(Instant.ofEpochMilli(start));}
  catch(RuntimeException ignored){return "날짜 정보 없음";}
 }

 static Long startTime(File track){
  return new File(track.getPath()+".imported").isFile()?firstRecordedTime(track):filenameTime(track.getName());
 }

 static Long filenameTime(String name){
  if(!name.endsWith(".track"))return null;
  int dash=name.indexOf('-');if(dash<=0)return null;
  try{long time=Long.parseLong(name.substring(0,dash));return time>0?time:null;}
  catch(NumberFormatException ignored){return null;}
 }

 /** Read only the first newline-committed point; never load a GPS file on the UI thread. */
 static Long firstRecordedTime(File track){
  try(FileInputStream in=new FileInputStream(track)){
   byte[] line=new byte[256];int n=0,b;
   while(n<line.length&&(b=in.read())!=-1){
    if(b=='\n'){
     String[] parts=new String(line,0,n,java.nio.charset.StandardCharsets.UTF_8).split(",",-1);
     if(parts.length!=4)return null;
     double lat=Double.parseDouble(parts[0]),lon=Double.parseDouble(parts[1]),accuracy=Double.parseDouble(parts[2]);
     long time=Long.parseLong(parts[3]);
     return TrackFile.valid(lat,lon,accuracy,time)?time:null;
    }
    line[n++]=(byte)b;
   }
  }catch(IOException|NumberFormatException ignored){}
  return null;
 }
}
