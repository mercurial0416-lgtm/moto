package com.motohub.next;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.*;

public final class RideHistoryLabelTest {
 static int assertions=0;
 static void check(boolean b,String why){assertions++;if(!b)throw new AssertionError(why);}
 static void write(File f,String text)throws Exception{Files.writeString(f.toPath(),text,StandardCharsets.UTF_8);}
 public static void main(String[] args)throws Exception{
  File dir=Files.createTempDirectory("moto-history-test").toFile();
  try{
   ZoneId zone=ZoneId.of("Asia/Seoul");
   long recorded=Instant.parse("2024-05-12T01:23:00Z").toEpochMilli();
   long imported=Instant.parse("2026-10-17T07:00:00Z").toEpochMilli();
   File gp=new File(dir,imported+"-import.track");
   write(gp,"37.1,127.2,4.0,"+recorded+"\n37.2,127.3,4.0,"+(recorded+5000)+"\n");
   check(gp.createNewFile()==false,"test fixture exists");
   check(new File(gp.getPath()+".imported").createNewFile(),"import marker");
   check(RideHistoryLabel.title(gp,zone).equals("2024.05.12 (일) 10:23"),"GPX must show actual ride time, not import date");
   check(RideHistoryLabel.title(gp,ZoneId.of("UTC")).equals("2024.05.12 (일) 01:23"),"timezone follows device");
   File nativeRide=new File(dir,imported+"-native.track");write(nativeRide,"37.1,127.2,4.0,"+recorded+"\n");
   check(RideHistoryLabel.title(nativeRide,zone).equals("2026.10.17 (토) 16:00"),"native ride uses capture filename time");
   check(RideHistoryLabel.filenameTime("bad.track")==null,"missing timestamp");
   check(RideHistoryLabel.filenameTime("-id.track")==null,"missing numeric prefix");
   check(RideHistoryLabel.filenameTime("-123.jpg")==null,"not a track");
   check(RideHistoryLabel.filenameTime("-123.track")==null,"empty timestamp");
   check(RideHistoryLabel.filenameTime("-0.track")==null,"zero timestamp");
   check(RideHistoryLabel.filenameTime("not-a-number.track")==null,"non-numeric timestamp");
   check(RideHistoryLabel.title(new File(dir,"bad.track"),zone).equals("날짜 정보 없음"),"unknown title");
   File empty=new File(dir,"1-empty.track");write(empty,"");new File(empty.getPath()+".imported").createNewFile();
   check(RideHistoryLabel.title(empty,zone).equals("날짜 정보 없음"),"empty import must not use import time");
   write(empty,"37.1,127.2,4.0,"+recorded);check(RideHistoryLabel.firstRecordedTime(empty)==null,"uncommitted row ignored");
   write(empty,"37.1,127.2,4.0,"+recorded+"\n");check(RideHistoryLabel.firstRecordedTime(empty)==recorded,"committed point accepted");
   write(empty,"99.0,127.2,4.0,"+recorded+"\n");check(RideHistoryLabel.firstRecordedTime(empty)==null,"invalid latitude rejected");
   write(empty,"37.1,127.2,4.0,0\n");check(RideHistoryLabel.firstRecordedTime(empty)==null,"invalid time rejected");
   write(empty,"37.1,127.2,4.0,abc\n");check(RideHistoryLabel.firstRecordedTime(empty)==null,"invalid time text rejected");
   write(empty,"x".repeat(257)+"\n");check(RideHistoryLabel.firstRecordedTime(empty)==null,"oversized line rejected");
   check(RideHistoryLabel.firstRecordedTime(new File(dir,"missing.track"))==null,"missing file safe");
   System.out.println("PASS: "+assertions+" GPS history date/time assertions");
  }finally{File[] files=dir.listFiles();if(files!=null)for(File f:files)f.delete();dir.delete();}
 }
}
