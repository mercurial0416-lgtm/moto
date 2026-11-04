package com.motohub.next;

import android.content.Context;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

/** Error bypasses ordinary exception rollback, simulating a lost editing process. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35)
public final class RideMetadataCrashRecoveryTest {
 Context c;File first,second;
 static final String OLD="11111111-2222-4333-8444-555555555555",NEXT="22222222-2222-4333-8444-555555555555";
 static final class ProcessStopped extends Error {}
 @Before public void setup()throws Exception{
  c=RuntimeEnvironment.getApplication();String session=UUID.randomUUID().toString();
  first=create(1700000000000L,session);second=create(1700000030000L,session);
 }
 File create(long time,String session)throws Exception{
  File f=new File(TrackFile.directory(c.getFilesDir()),time+"-"+session+".track");
  Files.write(f.toPath(),(TrackFile.point(37,127,4,time)+TrackFile.point(37.001,127,4,time+10000)).getBytes(StandardCharsets.UTF_8));
  TrackCompletion.mark(f,2);return f;
 }
 void launchAgain(){MainActivity a=Robolectric.buildActivity(MainActivity.class).setup().get();try{assertFalse(a.readOnly);}finally{a.finish();}}
 @Test public void interruptedCategoryChangeIsRolledBackBeforeScreensLoad()throws Exception{
  RideCategory.write(first,RideCategory.DAILY);byte[] gps=Files.readAllBytes(first.toPath());
  try{RideSessionCategory.write(c.getFilesDir(),first,RideCategory.TOUR,()->{throw new ProcessStopped();});fail();}
  catch(ProcessStopped expected){}
  launchAgain();
  assertEquals(RideCategory.DAILY,RideCategory.read(first));assertEquals(RideCategory.AUTO,RideCategory.read(second));
  assertArrayEquals(gps,Files.readAllBytes(first.toPath()));
  assertFalse(new File(second+".category").exists());
 }
 @Test public void interruptedBikeChangeRestoresPresentAndMissingAssociations()throws Exception{
  RideBikeBinding.write(first,OLD);byte[] gps=Files.readAllBytes(first.toPath());
  try{RideSessionBikeBinding.write(c.getFilesDir(),first,NEXT,Collections.emptySet(),()->{throw new ProcessStopped();});fail();}
  catch(ProcessStopped expected){}
  launchAgain();assertEquals(OLD,RideBikeBinding.readStrict(first));assertEquals("",RideBikeBinding.readStrict(second));
  assertArrayEquals(gps,Files.readAllBytes(first.toPath()));
 }
 @Test public void fullyCommittedSessionChangeSurvivesRestart()throws Exception{
  RideSessionCategory.write(c.getFilesDir(),first,RideCategory.TOUR);
  RideSessionBikeBinding.write(c.getFilesDir(),first,NEXT,Collections.emptySet());
  launchAgain();
  assertEquals(RideCategory.TOUR,RideCategory.read(first));assertEquals(RideCategory.TOUR,RideCategory.read(second));
  assertEquals(NEXT,RideBikeBinding.readStrict(first));assertEquals(NEXT,RideBikeBinding.readStrict(second));
  assertFalse(new File(c.getFilesDir(),"ride-metadata-journal").exists());
 }
 @Test public void ordinaryFailureStillRollsBackImmediatelyAndPermitsRetry()throws Exception{
  RideCategory.write(first,RideCategory.DAILY);
  try{RideSessionCategory.write(c.getFilesDir(),first,RideCategory.TOUR,()->{throw new IllegalStateException("failed write");});fail();}
  catch(java.io.IOException expected){}
  assertEquals(RideCategory.DAILY,RideCategory.read(first));assertEquals(RideCategory.AUTO,RideCategory.read(second));
  assertFalse(new File(c.getFilesDir(),"ride-metadata-journal").exists());
  RideSessionCategory.write(c.getFilesDir(),first,RideCategory.WINDING);
  assertEquals(RideCategory.WINDING,RideCategory.read(first));
 }
 void interruptCategory()throws Exception{
  RideCategory.write(first,RideCategory.DAILY);
  try{RideSessionCategory.write(c.getFilesDir(),first,RideCategory.TOUR,()->{throw new ProcessStopped();});fail();}
  catch(ProcessStopped expected){}
 }
 File journal(){return new File(c.getFilesDir(),"ride-metadata-journal");}
 @Test public void damagedSnapshotProtectsLiveFilesAndRetainsRecoveryEvidence()throws Exception{
  interruptCategory();File snapshot=new File(journal(),"snapshot");byte[] good=Files.readAllBytes(snapshot.toPath());
  byte[] broken=good.clone();broken[10]^=1;Files.write(snapshot.toPath(),broken);
  MainActivity a=Robolectric.buildActivity(MainActivity.class).setup().get();try{assertTrue(a.readOnly);}finally{a.finish();}
  assertEquals(RideCategory.TOUR,RideCategory.read(first));assertEquals(RideCategory.AUTO,RideCategory.read(second));
  assertTrue(snapshot.isFile());Files.write(snapshot.toPath(),good);
  launchAgain();assertEquals(RideCategory.DAILY,RideCategory.read(first));assertFalse(journal().exists());
 }
 @Test public void unsafeLaterTargetIsRejectedBeforeAnyEarlierTargetIsRolledBack()throws Exception{
  interruptCategory();File external=new File(c.getFilesDir(),"external-category");
  Files.write(external.toPath(),"private original".getBytes(StandardCharsets.UTF_8));
  File linked=new File(second+".category");Files.createSymbolicLink(linked.toPath(),external.toPath());
  try{RideMetadataJournal.recoverPending(c.getFilesDir());fail();}catch(java.io.IOException expected){}
  assertEquals(RideCategory.TOUR,RideCategory.read(first));
  assertEquals("private original",new String(Files.readAllBytes(external.toPath()),StandardCharsets.UTF_8));
  assertTrue(journal().exists());Files.delete(linked.toPath());
  assertEquals(1,RideMetadataJournal.recoverPending(c.getFilesDir()));
  assertEquals(0,RideMetadataJournal.recoverPending(c.getFilesDir()));
  assertEquals(RideCategory.DAILY,RideCategory.read(first));assertFalse(linked.exists());
 }
 @Test public void committedFlagSurvivingInterruptedCleanupDoesNotUndoSuccessfulEdits()throws Exception{
  interruptCategory();RideCategory.write(second,RideCategory.TOUR);
  Files.write(new File(journal(),"committed").toPath(),"MOTO-RIDE-EDIT-1".getBytes(StandardCharsets.US_ASCII));
  launchAgain();assertEquals(RideCategory.TOUR,RideCategory.read(first));assertEquals(RideCategory.TOUR,RideCategory.read(second));
  assertFalse(journal().exists());
 }
 @Test public void pendingEditCannotBeOverwrittenByAnotherKindOfEdit()throws Exception{
  interruptCategory();
  try{RideSessionBikeBinding.write(c.getFilesDir(),first,NEXT,Collections.emptySet());fail();}catch(java.io.IOException expected){}
  assertEquals("",RideBikeBinding.readStrict(first));assertTrue(journal().exists());
  launchAgain();RideSessionBikeBinding.write(c.getFilesDir(),first,NEXT,Collections.emptySet());
  assertEquals(NEXT,RideBikeBinding.readStrict(second));assertFalse(journal().exists());
 }
 @Test public void interruptedPreparationDoesNotAlterLiveMetadata()throws Exception{
  RideCategory.write(first,RideCategory.DAILY);assertTrue(journal().mkdir());
  Files.write(new File(journal(),"snapshot.writing").toPath(),new byte[]{1,2,3});
  launchAgain();assertEquals(RideCategory.DAILY,RideCategory.read(first));assertFalse(journal().exists());
 }
 @Test public void aChecksummedSnapshotCannotEscapeTheRideDirectory()throws Exception{
  File external=new File(c.getFilesDir(),"outside.bike");Files.write(external.toPath(),OLD.getBytes(StandardCharsets.US_ASCII));
  java.io.ByteArrayOutputStream payload=new java.io.ByteArrayOutputStream();
  try(java.io.DataOutputStream out=new java.io.DataOutputStream(payload)){
   out.writeInt(1);out.writeInt(1);out.writeUTF("../outside.bike");out.writeInt(-1);
  }
  byte[] body=payload.toByteArray();payload.write(java.security.MessageDigest.getInstance("SHA-256").digest(body));
  assertTrue(journal().mkdir());Files.write(new File(journal(),"snapshot").toPath(),payload.toByteArray());
  try{RideMetadataJournal.recoverPending(c.getFilesDir());fail();}catch(java.io.IOException expected){}
  assertEquals(OLD,new String(Files.readAllBytes(external.toPath()),StandardCharsets.US_ASCII));assertTrue(journal().exists());
 }

 @Test public void unresolvedEditBlocksFullAndSplitBackupsBeforeAnyOutputIsPublished()throws Exception{
  interruptCategory();String json=Store.empty().toString();
  for(String part:new String[]{"FULL","GPS","MEDIA"}){
   java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();
   try{BackupBundle.export(json,c.getFilesDir(),bytes,part);fail();}catch(java.io.IOException expected){}
   assertEquals(0,bytes.size());
  }
  try{BatchArchiveExport.plan(c.getFilesDir());fail();}catch(java.io.IOException expected){}
  launchAgain();java.io.ByteArrayOutputStream safe=new java.io.ByteArrayOutputStream();
  assertTrue(BackupBundle.export(json,c.getFilesDir(),safe)>0);assertTrue(safe.size()>0);
 }

}
