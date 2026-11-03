package com.motohub.next;

import org.junit.Test;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;
import static org.junit.Assert.*;

/** v573: no arbitrary history truncation, bounded UI-only GPS memoization, local cost dates. */
public final class Ux118LongHistoryDistanceCacheTest {
 @Test public void timelineCanPagePastTwoHundredWithoutBreakingFilter(){
  List<MotoTimeline.Event> records=new ArrayList<>();
  for(int i=0;i<225;i++)
   records.add(new MotoTimeline.Event(MotoTimeline.RIDE,"2026-11-03","ride "+i,
    "GPS","", "", "1700000000000-12345678-1234-4234-9234-123456789abc.track",225-i));
  for(int i=0;i<5;i++)records.add(new MotoTimeline.Event(MotoTimeline.SERVICE,"2026-11-03",
   "service "+i,"service","services",""+i,"",350-i));
  MotoTimeline.sort(records);
  assertEquals(40,MotoTimeline.filtered(records,MotoTimeline.ALL,40).size());
  assertEquals(80,MotoTimeline.filtered(records,MotoTimeline.ALL,80).size());
  assertEquals(230,MotoTimeline.filtered(records,MotoTimeline.ALL,240).size());
  assertEquals(225,MotoTimeline.filtered(records,MotoTimeline.RIDE,240).size());
  assertEquals(5,MotoTimeline.filtered(records,MotoTimeline.SERVICE,40).size());
 }
 @Test public void gpsDistanceCacheReusesOnlyUnchangedCompletedTrack()throws Exception{
  File folder=Files.createTempDirectory("moto-cache-gps-").toFile();
  File ride=new File(folder,"1700000000000-12345678-1234-4234-9234-123456789abc.track");
  File done=new File(ride.getPath()+".done");
  try{
   String points=TrackFile.point(37.0,127.0,5,1700000000000L)+
    TrackFile.point(37.001,127.0,5,1700000005000L);
   Files.write(ride.toPath(),points.getBytes(StandardCharsets.UTF_8));
   Files.write(done.toPath(),new byte[0]);
   RideDistanceCache.clear();
   long analyzed=RideDistanceCache.analysesForTest();
   long cached=RideDistanceCache.hitsForTest();
   RideDistanceCache.Summary first=RideDistanceCache.read(ride);
   assertEquals(analyzed+1,RideDistanceCache.analysesForTest());
   assertTrue(first.distanceKm>0);
   assertEquals(first.distanceKm,RideDistanceCache.read(ride).distanceKm,0.00001);
   assertEquals(cached+1,RideDistanceCache.hitsForTest());
   Files.write(ride.toPath(),TrackFile.point(37.002,127.0,5,1700000010000L)
     .getBytes(StandardCharsets.UTF_8),StandardOpenOption.APPEND);
   RideDistanceCache.Summary updated=RideDistanceCache.read(ride);
   assertEquals(analyzed+2,RideDistanceCache.analysesForTest());
   assertTrue(updated.distanceKm>=first.distanceKm);
   Files.delete(done.toPath());
   try{RideDistanceCache.read(ride);fail("An incomplete track must never use cache");}
   catch(java.io.IOException expected){}
   RideDistanceCache.clear();
  }finally{
   Files.deleteIfExists(done.toPath());
   Files.deleteIfExists(ride.toPath());
   Files.deleteIfExists(folder.toPath());
  }
 }
 @Test public void localCostMonthBoundaryMatchesTravelTimezoneWithoutAndroidJson(){
  // The cost screen passes one local day to forBikeAsOf; the same absolute
  // instant must be October in LA and November in Tokyo.
  Instant sameInstant=Instant.parse("2026-10-31T16:30:00Z");
  LocalDate tokyo=sameInstant.atZone(ZoneId.of("Asia/Tokyo")).toLocalDate();
  LocalDate losAngeles=sameInstant.atZone(ZoneId.of("America/Los_Angeles")).toLocalDate();
  assertEquals(LocalDate.of(2026,11,1),tokyo);
  assertEquals(LocalDate.of(2026,10,31),losAngeles);
  assertEquals(YearMonth.of(2026,11),YearMonth.from(tokyo));
  assertEquals(YearMonth.of(2026,10),YearMonth.from(losAngeles));
 }
}
