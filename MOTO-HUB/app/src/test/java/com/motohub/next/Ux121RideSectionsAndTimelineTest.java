package com.motohub.next;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.Test;
import static org.junit.Assert.*;

/** Logical GPS rollover never rewrites a legacy track or double-counts a ride. */
public final class Ux121RideSectionsAndTimelineTest {
 @Test public void cappedSectionsRemainSeparateCompletedBackuppableTracks()throws Exception{
  File base=Files.createTempDirectory("moto-v575-segments").toFile();
  File rides=TrackFile.directory(base);
  String uuid=UUID.randomUUID().toString();
  long now=System.currentTimeMillis()-100_000L;
  File first=new File(rides,now+"-"+uuid+".track");
  File second=new File(rides,(now+10_000L)+"-"+uuid+".track");
  try{
   Files.write(first.toPath(),(TrackFile.point(37,127,5,now)+TrackFile.point(37.0001,127,5,now+10_000L)).getBytes(StandardCharsets.UTF_8));
   Files.write(second.toPath(),(TrackFile.point(37.0001,127,5,now+10_000L)+TrackFile.point(37.0002,127,5,now+20_000L)).getBytes(StandardCharsets.UTF_8));
   TrackCompletion.mark(first,2);TrackCompletion.mark(second,2);
   assertTrue(RideMileageIntegrity.validCompleted(first));
   assertTrue(RideMileageIntegrity.validCompleted(second));
   assertEquals(RideSegmentGroup.id(first),RideSegmentGroup.id(second));
   assertTrue(BackupFormat.allowed("rides/"+first.getName(),false));
   assertTrue(BackupFormat.allowed("rides/"+second.getName(),false));
   assertEquals(2,RideSegmentGroup.members(base,first.getName()).size());
   Map<String,Integer> groups=RideSegmentGroup.counts(TrackFile.tracks(base));
   assertEquals(Integer.valueOf(2),groups.get(uuid));
   RideMonthlyReport report=new RideMonthlyReport();
   report.add(second,System.currentTimeMillis(),2,120000L);
   report.add(first,System.currentTimeMillis(),3,60000L);
   assertEquals(1,report.current.count);
   assertEquals(5,report.current.km,.001);
   RideStatsSeries series=new RideStatsSeries(YearMonth.now(),ZoneId.systemDefault());
   assertTrue(series.add(uuid,now,2,120000L));
   assertTrue(series.add(uuid,now+10000L,3,60000L));
   assertEquals(1,series.rides());
   assertEquals(5,series.totalKm,.001);
  }finally{
   File[] parts=rides.listFiles();if(parts!=null)for(File f:parts)Files.deleteIfExists(f.toPath());
   Files.deleteIfExists(rides.toPath());Files.deleteIfExists(base.toPath());
  }
 }
 @Test public void thousandsOfTimelineEventsNeedOnlyTopWindowAndOneSessionCard(){
  String uuid="12345678-1234-4234-9234-123456789abc";
  List<MotoTimeline.Event> input=new ArrayList<>();
  for(int i=0;i<3000;i++)input.add(new MotoTimeline.Event(MotoTimeline.SERVICE,
   "2026-11-03","S"+i,"", "services",""+i,"",i));
  input.add(new MotoTimeline.Event(MotoTimeline.RIDE,"2026-11-03","GPS","",
   "","","1762000100000-"+uuid+".track",4001));
  input.add(new MotoTimeline.Event(MotoTimeline.RIDE,"2026-11-03","GPS","",
   "","","1762000200000-"+uuid+".track",4002));
  List<MotoTimeline.Event> newest=MotoTimeline.newest(input,80);
  assertEquals(80,newest.size());
  assertEquals(4002,newest.get(0).time);
  List<MotoTimeline.Event> grouped=RideSegmentGroup.collapse(newest);
  assertEquals(79,grouped.size());
  assertTrue(grouped.get(0).detail.contains("2구간"));
  assertEquals(4001,grouped.get(0).time);
 }
}
