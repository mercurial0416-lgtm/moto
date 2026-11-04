package com.motohub.next;

import android.os.Looper;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=35)
public final class RideRecoveryProtectionTest {
 MainActivity a;File track;byte[] original;
 @Before public void setup()throws Exception{
  a=Robolectric.buildActivity(MainActivity.class).setup().get();
  track=new File(TrackFile.directory(a.getFilesDir()),"1700000000000-"+UUID.randomUUID()+".track");
  original=(TrackFile.point(37,127,4,1700000000000L)+TrackFile.point(37.001,127,4,1700000010000L)+"partial").getBytes(StandardCharsets.UTF_8);
  Files.write(track.toPath(),original);
 }
 @After public void cleanup(){RestoreRideGate.end();RestoreRideGate.endExport();RestoreRideGate.endFormWrite();RideService.running=false;if(a!=null){a.close();a.finish();}}
 void awaitError()throws Exception{
  for(int i=0;i<200;i++){
   Shadows.shadowOf(Looper.getMainLooper()).idle();
   if(ShadowAlertDialog.getLatestAlertDialog()!=null)return;
   Thread.sleep(10);
  }fail("blocked recovery must report a real UI error");
 }
 void unchanged()throws Exception{assertArrayEquals(original,Files.readAllBytes(track.toPath()));assertFalse(new File(track+".recovered").exists());}
 @Test public void readOnlyProtectionPreventsOriginalGpsMutation()throws Exception{
  a.readOnly=true;new RideUi(a).recoverInterrupted(track);awaitError();unchanged();assertFalse(RestoreRideGate.isRestoring());
 }
 @Test public void exportingBlocksRecoveryWithoutReleasingTheExportReservation()throws Exception{
  assertTrue(RestoreRideGate.beginExport(false,false));new RideUi(a).recoverInterrupted(track);awaitError();unchanged();assertTrue(RestoreRideGate.isExporting());
 }
 @Test public void anotherFormWriteBlocksRecoveryAndRetainsItsReservation()throws Exception{
  assertTrue(RestoreRideGate.beginFormWrite());new RideUi(a).recoverInterrupted(track);awaitError();unchanged();assertTrue(RestoreRideGate.isFormWriting());
 }
 @Test public void runningRideBlocksRecoveryAndPreservesTheOriginal()throws Exception{
  RideService.running=true;new RideUi(a).recoverInterrupted(track);awaitError();unchanged();assertFalse(RestoreRideGate.isRestoring());
 }
 @Test public void successfulRecoveryReleasesItsReservationAndKeepsCommittedPoints()throws Exception{
  new RideUi(a).recoverInterrupted(track);
  for(int i=0;i<200;i++){
   Shadows.shadowOf(Looper.getMainLooper()).idle();
   if(new File(track+".recovered").isFile()&&!RestoreRideGate.isRestoring())break;
   Thread.sleep(10);
  }
  assertTrue(new File(track+".recovered").isFile());assertFalse(RestoreRideGate.isRestoring());
  assertEquals(2,TrackFile.read(track,(lat,lon,accuracy,time)->{}));
  assertTrue(new String(Files.readAllBytes(track.toPath()),StandardCharsets.UTF_8).endsWith("\n"));
 }
}
