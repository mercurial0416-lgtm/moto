package com.motohub.next;
import static org.junit.Assert.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import org.junit.Test;

public final class Ux125ActiveSessionReportTest {
 @Test public void allSegmentsOfLiveRideExcluded(){
  String uuid=UUID.randomUUID().toString();
  String earlier="1762000000000-"+uuid+".track",active="1762000050000-"+uuid+".track";
  assertTrue(RideActiveSessionPolicy.exclude(earlier,true,active));
  assertTrue(RideActiveSessionPolicy.exclude(active,true,active));
  assertFalse(RideActiveSessionPolicy.exclude(earlier,false,active));
  assertFalse(RideActiveSessionPolicy.exclude("1762000040000-"+UUID.randomUUID()+".track",true,active));
 }
 @Test public void commuteAcrossThirtyDayBoundaryCountsOnceInEitherScanOrder()throws Exception{
  File dir=Files.createTempDirectory("moto-type-578").toFile();
  long now=Instant.parse("2026-11-04T10:00:00Z").toEpochMilli();
  long threshold=now-30L*86400000;
  String uuid=UUID.randomUUID().toString();
  File before=new File(dir,(threshold-2000)+"-"+uuid+".track");
  File after=new File(dir,(threshold+2000)+"-"+uuid+".track");
  File beforeCategory=new File(before+".category"),afterCategory=new File(after+".category");
  try{
   Files.write(beforeCategory.toPath(),RideCategory.COMMUTE.getBytes(StandardCharsets.UTF_8));
   Files.write(afterCategory.toPath(),RideCategory.COMMUTE.getBytes(StandardCharsets.UTF_8));
   RideTypeComparison latestFirst=new RideTypeComparison();
   latestFirst.add(after,now,2.5,40_000);latestFirst.add(before,now,4.5,50_000);
   RideTypeComparison oldFirst=new RideTypeComparison();
   oldFirst.add(before,now,4.5,50_000);oldFirst.add(after,now,2.5,40_000);
   assertEquals(0,latestFirst.commuteNow.count);assertEquals(1,latestFirst.commutePrev.count);
   assertEquals(0,oldFirst.commuteNow.count);assertEquals(1,oldFirst.commutePrev.count);
   assertEquals(2.5,oldFirst.commuteNow.km,0.001);
   assertEquals(4.5,latestFirst.commutePrev.km,0.001);
  }finally{
   Files.deleteIfExists(beforeCategory.toPath());Files.deleteIfExists(afterCategory.toPath());
   Files.deleteIfExists(dir.toPath());
  }
 }
}
