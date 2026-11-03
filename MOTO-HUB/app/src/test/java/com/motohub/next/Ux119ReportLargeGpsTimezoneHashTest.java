package com.motohub.next;

import org.junit.Test;
import java.io.BufferedWriter;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import static org.junit.Assert.*;

/** Read-only GPS correctness: large tracks, >150 ride reports, travel dates and SHA cache. */
public final class Ux119ReportLargeGpsTimezoneHashTest {
 private static File track(File dir,long ms,int index){
  return new File(dir,ms+"-"+String.format(Locale.ROOT,"%08x",index)+
   "-1234-4234-9234-123456789abc.track");
 }
 private static void completed(File track,String content)throws Exception{
  Files.write(track.toPath(),content.getBytes(StandardCharsets.UTF_8));
  Files.write(new File(track.getPath()+".done").toPath(),new byte[0]);
 }
 private static void cleanup(File directory)throws Exception{
  File[] children=directory.listFiles();
  if(children!=null)for(File child:children)Files.deleteIfExists(child.toPath());
  Files.deleteIfExists(directory.toPath());
 }

 @Test public void sixtyDayReportProcessesMoreThanOneHundredFiftyTracks()throws Exception{
  File dir=Files.createTempDirectory("moto-report-160").toFile();
  RideDistanceCache.clear();
  try{
   long now=System.currentTimeMillis();
   long first=now-86_400_000L;
   List<File> all=new ArrayList<>();
   for(int i=0;i<160;i++){
    long time=first+i*5_000L;
    File track=track(dir,time,i);
    completed(track,TrackFile.point(37.0,127.0,5,time)+
     TrackFile.point(37.0004,127.0,5,time+4_000L));
    all.add(track);
   }
   RideMonthlyReport report=new RideMonthlyReport();
   RideTypeComparison categories=new RideTypeComparison();
   for(File file:RideHistorySort.order(all,RideHistorySort.NEWEST)){
    if(!RideMonthlyReport.recent(file,now,60))continue;
    RideDistanceCache.Summary entry=RideDistanceCache.read(file);
    report.add(file,now,entry.distanceKm,entry.durationMillis);
    categories.add(file,now,entry);
   }
   assertEquals(160,report.current.count);
   assertTrue(report.current.km>1);
   assertEquals(0,report.skipped);
  }finally{RideDistanceCache.clear();cleanup(dir);}
 }

 @Test public void gpsAboveFiveMegabytesButBelowTwentyFiveIsAnalyzed()throws Exception{
  File dir=Files.createTempDirectory("moto-gps-6mb").toFile();
  File file=track(dir,1700000000000L,1);
  RideDistanceCache.clear();
  try{
   try(BufferedWriter out=Files.newBufferedWriter(file.toPath(),StandardCharsets.UTF_8)){
    for(int i=0;i<145000;i++)
     out.write("37.000000,127.000000,5.0,"+(1700000000000L+i*1000L)+"\n");
   }
   Files.write(new File(file.getPath()+".done").toPath(),new byte[0]);
   assertTrue(file.length()>5_000_000L);
   assertTrue(file.length()<=TrackFile.MAX_BYTES);
   assertEquals(TrackFile.MAX_BYTES,RideDistanceCache.MAX_ANALYSIS_BYTES);
   RideDistanceCache.Summary value=RideDistanceCache.read(file);
   assertEquals(0,value.distanceKm,0.01);
   assertTrue(value.durationMillis>86_400_000L);
  }finally{RideDistanceCache.clear();cleanup(dir);}
 }

 @Test public void cacheRejectsSameSizeSameMtimeRewrites()throws Exception{
  File dir=Files.createTempDirectory("moto-same-stat").toFile();
  File file=track(dir,1700000000000L,1);
  RideDistanceCache.clear();
  try{
   String original=TrackFile.point(37.000,127.000,5,1700000000000L)+
    TrackFile.point(37.001,127.000,5,1700000010000L);
   String replacement=TrackFile.point(37.000,127.000,5,1700000000000L)+
    TrackFile.point(37.002,127.000,5,1700000010000L);
   assertEquals(original.length(),replacement.length());
   completed(file,original);
   long time=file.lastModified();
   long before=RideDistanceCache.analysesForTest(),hits=RideDistanceCache.hitsForTest();
   double first=RideDistanceCache.read(file).distanceKm;
   assertEquals(before+1,RideDistanceCache.analysesForTest());
   assertEquals(first,RideDistanceCache.read(file).distanceKm,0.000001);
   assertEquals(hits+1,RideDistanceCache.hitsForTest());
   Files.write(file.toPath(),replacement.getBytes(StandardCharsets.UTF_8));
   assertTrue(file.setLastModified(time));
   assertEquals(time,file.lastModified());
   double second=RideDistanceCache.read(file).distanceKm;
   assertEquals(before+2,RideDistanceCache.analysesForTest());
   assertTrue("GPS contents changed despite identical stat",second>first);
  }finally{RideDistanceCache.clear();cleanup(dir);}
 }

 @Test public void activityDateUsesProvidedLocalZoneNotFixedSeoul(){
  ZoneId tokyo=ZoneId.of("Asia/Tokyo"),california=ZoneId.of("America/Los_Angeles");
  long tokyoStart=MotoTimeline.startOfDay("2026-11-01",tokyo);
  long californiaStart=MotoTimeline.startOfDay("2026-11-01",california);
  assertNotEquals(tokyoStart,californiaStart);
  Instant instant=Instant.parse("2026-10-31T16:30:00Z");
  assertEquals(LocalDate.of(2026,11,1),instant.atZone(tokyo).toLocalDate());
  assertEquals(LocalDate.of(2026,10,31),instant.atZone(california).toLocalDate());
 }
}
