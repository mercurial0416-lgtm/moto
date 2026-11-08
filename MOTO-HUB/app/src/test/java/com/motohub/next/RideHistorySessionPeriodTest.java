package com.motohub.next;

import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.*;

public class RideHistorySessionPeriodTest {
 @Rule public TemporaryFolder folder=new TemporaryFolder();
 private static final String SESSION="11111111-1111-4111-8111-111111111111";
 private static final String OTHER="22222222-2222-4222-8222-222222222222";
 private static final String BIKE_A="aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa";
 private static final String BIKE_B="bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb";
 private File track(long t,String session,String bike)throws Exception{
  File file=folder.newFile(t+"-"+session+".track");
  if(bike!=null)Files.writeString(new File(file.getPath()+".bike").toPath(),bike,StandardCharsets.US_ASCII);
  return file;
 }
 @Test public void startTimeAcrossMidnightKeepsEverySectionInSamePeriod()throws Exception{
  long now=1_700_000_000_000L,old=now-36*3600_000L,recent=now-3600_000L;
  File first=track(old,SESSION,BIKE_A);
  File second=track(recent,SESSION,BIKE_A);
  File independent=track(recent+1,OTHER,BIKE_A);
  List<File> result=RideHistorySessionPeriod.select(Arrays.asList(first,second,independent),now,RideHistoryPeriod.DAY);
  assertFalse(result.contains(first));
  assertFalse(result.contains(second));
  assertTrue(result.contains(independent));
  assertEquals(1,result.size());
  assertEquals(3,RideHistorySessionPeriod.select(Arrays.asList(first,second,independent),now,RideHistoryPeriod.ALL).size());
 }
 @Test public void sameSessionNameDoesNotBorrowStartFromOtherBike()throws Exception{
  long now=1_700_000_000_000L;
  File bikeA=track(now-36*3600_000L,SESSION,BIKE_A);
  File bikeB=track(now-3600_000L,SESSION,BIKE_B);
  List<File> result=RideHistorySessionPeriod.select(Arrays.asList(bikeA,bikeB),now,RideHistoryPeriod.DAY);
  assertEquals(1,result.size());
  assertSame(bikeB,result.get(0));
 }
 @Test public void importedGpxUsesRecordedPointTimeInsteadOfFileName()throws Exception{
  long now=1_700_000_000_000L;
  File imported=track(now-36*3600_000L,OTHER,null);
  Files.writeString(imported.toPath(),TrackFile.point(37.0,127.0,5,now-3600_000L),StandardCharsets.UTF_8);
  Files.writeString(new File(imported.getPath()+".imported").toPath(),"imported",StandardCharsets.UTF_8);
  assertEquals(1,RideHistorySessionPeriod.select(Arrays.asList(imported),now,RideHistoryPeriod.DAY).size());
 }
}
