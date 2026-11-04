package com.motohub.next;

import android.content.Context;
import android.content.ContextWrapper;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=35)
public final class GpsEstimateConsistencyTest {
 Context c;
 static final String A="11111111-2222-4333-8444-555555555555";
 static final String B="22222222-2222-4333-8444-555555555555";
 @Before public void setup(){c=RuntimeEnvironment.getApplication();RideMileageEstimate.resetAfterFullRestore(c);RideDistanceCache.clear();}
 File track(long start,String session,double latitude)throws Exception{
  File f=new File(TrackFile.directory(c.getFilesDir()),start+"-"+session+".track");
  Files.write(f.toPath(),(TrackFile.point(latitude,127,4,start)+TrackFile.point(latitude+.001,127,4,start+10000)).getBytes(StandardCharsets.UTF_8));
  TrackCompletion.mark(f,2);RideBikeBinding.write(f,A);return f;
 }
 @Test public void changingTheBikeMovesEverySegmentEstimateWithoutChangingGps()throws Exception{
  String session=UUID.randomUUID().toString();
  File a=track(1700000000000L,session,37),b=track(1700000030000L,session,37.002);
  byte[] original=Files.readAllBytes(a.toPath());
  RideMileageEstimate.rebuild(c);double total=RideMileageEstimate.total(c,A);assertTrue(total>0);
  RideSessionBikeBinding.write(c.getFilesDir(),a,B,Collections.emptySet());
  assertTrue("binding-only change must trigger a refresh",RideMileageEstimate.rebuild(c)>0);
  assertEquals(0,RideMileageEstimate.total(c,A),.000001);
  assertEquals(total,RideMileageEstimate.total(c,B),.000001);
  assertEquals(0,RideMileageEstimate.rebuild(c));
  assertArrayEquals(original,Files.readAllBytes(a.toPath()));assertTrue(b.isFile());
 }
 @Test public void mergedSourcesAreNotCountedAgainAndUndoRestoresTheirTotals()throws Exception{
  File a=track(1700000000000L,UUID.randomUUID().toString(),37);
  File b=track(1700000030000L,UUID.randomUUID().toString(),37.002);
  RideMileageEstimate.rebuild(c);double originals=RideMileageEstimate.total(c,A);
  RideTrackMerge.Result merge=RideTrackMerge.merge(c.getFilesDir(),a,b);
  RideMileageEstimate.rebuild(c);
  assertEquals(RideInsights.analyze(merge.combined).distanceKm,RideMileageEstimate.total(c,A),.000001);
  RideTrackMerge.undo(c.getFilesDir(),merge.combined);RideMileageEstimate.rebuild(c);
  assertEquals(originals,RideMileageEstimate.total(c,A),.000001);
  assertTrue(a.isFile());assertTrue(b.isFile());assertTrue(merge.combined.isFile());
 }
 @Test public void trashAndRestoreUpdateTheDerivedTotalWhileRetainingOriginalBytes()throws Exception{
  File f=track(1700000000000L,UUID.randomUUID().toString(),37);
  byte[] original=Files.readAllBytes(f.toPath());
  RideMileageEstimate.rebuild(c);double total=RideMileageEstimate.total(c,A);
  RideTrash.move(c.getFilesDir(),f);assertTrue(RideMileageEstimate.rebuild(c)>0);
  assertEquals(0,RideMileageEstimate.total(c,A),.000001);
  File restored=RideTrash.restore(c.getFilesDir(),RideTrash.entries(c.getFilesDir()).get(0));
  RideMileageEstimate.rebuild(c);assertEquals(total,RideMileageEstimate.total(c,A),.000001);
  assertArrayEquals(original,Files.readAllBytes(restored.toPath()));
 }
 @Test public void sameSizeAndTimestampReplacementDoesNotReuseStaleDistance()throws Exception{
  File f=track(1700000000000L,UUID.randomUUID().toString(),37);
  RideMileageEstimate.rebuild(c);double old=RideMileageEstimate.total(c,A);
  long modified=f.lastModified();long size=f.length();
  String raw=new String(Files.readAllBytes(f.toPath()),StandardCharsets.UTF_8).replace("37.001","37.002");
  Files.write(f.toPath(),raw.getBytes(StandardCharsets.UTF_8));assertEquals(size,f.length());assertTrue(f.setLastModified(modified));
  assertTrue(RideMileageEstimate.rebuild(c)>0);
  assertEquals(RideInsights.analyze(f).distanceKm,RideMileageEstimate.total(c,A),.000001);
  assertNotEquals(old,RideMileageEstimate.total(c,A),.000001);
 }
 @Test public void dashboardReadsThePreviousSnapshotDuringARealBackgroundRebuild()throws Exception{
  File f=track(1700000000000L,UUID.randomUUID().toString(),37);
  RideMileageEstimate.rebuild(c);double previous=RideMileageEstimate.total(c,A);
  RideBikeBinding.write(f,B);
  CountDownLatch scanning=new CountDownLatch(1),resume=new CountDownLatch(1);
  Context slow=new ContextWrapper(c){
   @Override public File getFilesDir(){
    scanning.countDown();
    try{if(!resume.await(5,TimeUnit.SECONDS))throw new IllegalStateException("test scan timed out");}
    catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException(e);}
    return super.getFilesDir();
   }
  };
  ExecutorService threads=Executors.newFixedThreadPool(2);
  try{
   Future<Integer> writer=threads.submit(()->RideMileageEstimate.rebuild(slow));
   assertTrue(scanning.await(2,TimeUnit.SECONDS));
   Future<Double> read=threads.submit(()->RideMileageEstimate.total(c,A));
   assertEquals(previous,read.get(2,TimeUnit.SECONDS),0);
   assertEquals(0,RideMileageEstimate.total(c,B),0);
   resume.countDown();assertTrue(writer.get(5,TimeUnit.SECONDS)>0);
   assertEquals(0,RideMileageEstimate.total(c,A),0);
   assertEquals(previous,RideMileageEstimate.total(c,B),0);
  }finally{resume.countDown();threads.shutdownNow();}
 }
 @Test public void failedSourceAnalysisDoesNotPublishAPartiallyReassignedLedger()throws Exception{
  File a=track(1700000000000L,UUID.randomUUID().toString(),37);
  File b=track(1700000030000L,UUID.randomUUID().toString(),37.002);
  RideMileageEstimate.rebuild(c);double before=RideMileageEstimate.total(c,A);
  RideBikeBinding.write(b,B);
  Files.write(a.toPath(),"broken GPS row\n".getBytes(StandardCharsets.UTF_8));
  byte[] damaged=Files.readAllBytes(a.toPath());
  try{RideMileageEstimate.rebuild(c);fail("source analysis must fail");}catch(java.io.IOException expected){}
  assertEquals(before,RideMileageEstimate.total(c,A),0);
  assertEquals(0,RideMileageEstimate.total(c,B),0);
  assertArrayEquals(damaged,Files.readAllBytes(a.toPath()));
 }
 @Test public void unfinishedAndUnboundTracksRemainExcluded()throws Exception{
  File completed=track(1700000000000L,UUID.randomUUID().toString(),37);
  File unbound=track(1700000030000L,UUID.randomUUID().toString(),37.002);
  Files.delete(new File(unbound+".bike").toPath());
  File unfinished=track(1700000060000L,UUID.randomUUID().toString(),37.004);
  Files.delete(new File(unfinished+".done").toPath());
  RideMileageEstimate.rebuild(c);
  assertEquals(RideInsights.analyze(completed).distanceKm,RideMileageEstimate.total(c,A),.000001);
  assertTrue(unbound.isFile());assertTrue(unfinished.isFile());
 }
 @Test public void aCompletionFlagWithoutAnyCommittedPointAddsNoEstimate()throws Exception{
  File f=new File(TrackFile.directory(c.getFilesDir()),"1700000000000-"+UUID.randomUUID()+".track");
  Files.write(f.toPath(),"37,127,4,1700000000000".getBytes(StandardCharsets.UTF_8));
  assertTrue(new File(f+".done").createNewFile());RideBikeBinding.write(f,A);
  assertEquals(0,RideMileageEstimate.rebuild(c));assertEquals(0,RideMileageEstimate.total(c,A),0);
  assertFalse(c.getSharedPreferences("gps_distance_estimate_v567",Context.MODE_PRIVATE).contains("ledger"));
 }
 @Test public void unsafeCollectionDoesNotReplacePreviouslyVerifiedLedger()throws Exception{
  File f=track(1700000000000L,UUID.randomUUID().toString(),37);
  RideMileageEstimate.rebuild(c);double before=RideMileageEstimate.total(c,A);
  File bad=new File(f.getParentFile(),"1700000050000-"+UUID.randomUUID()+".track");
  Files.createSymbolicLink(bad.toPath(),f.toPath());
  try{RideMileageEstimate.rebuild(c);fail("unsafe collection must fail");}catch(java.io.IOException expected){}
  assertEquals(before,RideMileageEstimate.total(c,A),0);
 }
}
