package com.motohub.next;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.UUID;
import java.util.concurrent.*;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=35)
public final class RideCompletionResponsivenessTest {
 Context c;
 static final String BIKE="11111111-2222-4333-8444-555555555555";
 @Before public void setup(){
  c=RuntimeEnvironment.getApplication();c.getSharedPreferences("ride_completion_inbox",0).edit().clear().commit();
  RideMileageEstimate.resetAfterFullRestore(c);
 }
 File track(long t)throws Exception{
  File f=new File(TrackFile.directory(c.getFilesDir()),t+"-"+UUID.randomUUID()+".track");
  Files.write(f.toPath(),(TrackFile.point(37,127,4,t)+TrackFile.point(37.001,127,4,t+10000)).getBytes(StandardCharsets.UTF_8));
  TrackCompletion.mark(f,2);RideBikeBinding.write(f,BIKE);return f;
 }
 Context pauseMileage(CountDownLatch blocked,CountDownLatch resume){
  return new ContextWrapper(c){
   @Override public SharedPreferences getSharedPreferences(String name,int mode){
    if("gps_distance_estimate_v567".equals(name)){
     blocked.countDown();
     try{if(!resume.await(6,TimeUnit.SECONDS))throw new IllegalStateException("test mileage timed out");}
     catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException(e);}
    }
    return super.getSharedPreferences(name,mode);
   }
  };
 }
 @Test public void viewingAndAcknowledgingAnOldSummaryDoesNotWaitForNewMileageWork()throws Exception{
  File old=track(1700000000000L),fresh=track(1700000030000L);
  RideCompletionInbox.record(c,old);
  CountDownLatch blocked=new CountDownLatch(1),resume=new CountDownLatch(1);
  ExecutorService threads=Executors.newFixedThreadPool(2);
  try{
   Future<?> worker=threads.submit(()->RideCompletionInbox.record(pauseMileage(blocked,resume),fresh));
   assertTrue(blocked.await(2,TimeUnit.SECONDS));
   Future<Boolean> reader=threads.submit(()->{
    assertEquals("the new recap is available before optional mileage work ends",fresh.getName(),RideCompletionInbox.unread(c).getString("id"));
    assertEquals(2,RideCompletionInbox.entries(c).size());
    return RideCompletionInbox.seen(c,old.getName());
   });
   assertTrue(reader.get(2,TimeUnit.SECONDS));
   resume.countDown();worker.get(5,TimeUnit.SECONDS);
   assertEquals(2,RideCompletionInbox.entries(c).size());
   for(JSONObject row:RideCompletionInbox.entries(c))
    if(old.getName().equals(row.getString("id")))assertTrue("late publication must preserve a UI acknowledgement",row.getBoolean("seen"));
  }finally{resume.countDown();threads.shutdownNow();}
 }
 @Test public void movingATrackToTrashDuringAnalysisCannotRepublishAGhostSummary()throws Exception{
  File f=track(1700000000000L);byte[] original=Files.readAllBytes(f.toPath());
  CountDownLatch blocked=new CountDownLatch(1),resume=new CountDownLatch(1);
  ExecutorService threads=Executors.newFixedThreadPool(2);
  try{
   Future<?> worker=threads.submit(()->RideCompletionInbox.record(pauseMileage(blocked,resume),f));
   assertTrue(blocked.await(2,TimeUnit.SECONDS));
   Future<?> removal=threads.submit(()->{
    try{RideTrash.move(c.getFilesDir(),f);RideCompletionInbox.remove(c,f.getName());}
    catch(Exception e){throw new IllegalStateException(e);}
   });
   removal.get(2,TimeUnit.SECONDS);
   resume.countDown();worker.get(5,TimeUnit.SECONDS);
   assertTrue(RideCompletionInbox.entries(c).isEmpty());
   assertEquals(0,RideMileageEstimate.total(c,BIKE),0);
   File restored=RideTrash.restore(c.getFilesDir(),RideTrash.entries(c.getFilesDir()).get(0));
   assertArrayEquals(original,Files.readAllBytes(restored.toPath()));
  }finally{resume.countDown();threads.shutdownNow();}
 }
 @Test public void simultaneousCompletionAttemptsStillPublishOnlyOneSummary()throws Exception{
  File f=track(1700000000000L);CountDownLatch start=new CountDownLatch(1);
  ExecutorService threads=Executors.newFixedThreadPool(2);
  try{
   Callable<Void> record=()->{assertTrue(start.await(2,TimeUnit.SECONDS));RideCompletionInbox.record(c,f);return null;};
   Future<Void> first=threads.submit(record),second=threads.submit(record);start.countDown();
   first.get(5,TimeUnit.SECONDS);second.get(5,TimeUnit.SECONDS);
   assertEquals(1,RideCompletionInbox.entries(c).size());
   assertEquals(f.getName(),RideCompletionInbox.unread(c).getString("id"));
   assertEquals(RideInsights.analyze(f).distanceKm,RideMileageEstimate.total(c,BIKE),.000001);
  }finally{start.countDown();threads.shutdownNow();}
 }
}
