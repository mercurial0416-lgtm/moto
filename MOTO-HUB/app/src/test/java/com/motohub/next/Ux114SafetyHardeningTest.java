package com.motohub.next;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.zip.*;
import static org.junit.Assert.*;

/** v573 final safety: paid-vs-planned costs, read-only ECU values, verified restore, GPS gaps. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35)
public final class Ux114SafetyHardeningTest {
 @Test public void futureAndPlannedItemsAreNeverChargedAsAlreadySpent()throws Exception{
  String bike="11111111-2222-4333-8444-555555555555";
  List<JSONObject> expenses=Arrays.asList(
   new JSONObject().put("vehicleId",bike).put("date","2026-11-03").put("type","주유").put("amount",20000),
   new JSONObject().put("vehicleId",bike).put("date","2026-11-04").put("type","주유").put("amount",30000),
   new JSONObject().put("vehicleId",bike).put("date","2026-11-02").put("type","기타").put("amount",40000).put("scheduleOnly",true),
   new JSONObject().put("vehicleId",bike).put("date","2026-10-01").put("type","주유").put("amount",10000));
  List<JSONObject> services=Arrays.asList(
   new JSONObject().put("vehicleId",bike).put("date","2026-11-03").put("cost",15000),
   new JSONObject().put("vehicleId",bike).put("date","2026-11-09").put("cost",35000),
   new JSONObject().put("vehicleId",bike).put("date","2026-11-01").put("cost",25000).put("scheduleOnly",true));
  RecordMonthlySummary sum=RecordMonthlySummary.forBikeAsOf(
   expenses,services,bike,YearMonth.of(2026,11),LocalDate.of(2026,11,3));
  assertEquals(30000,sum.sumFuelAndOther(),0.01);
  assertEquals(15000,sum.sumService(),0.01);
  assertEquals(4,sum.scheduledOrFuture);
  assertEquals(35000,sum.monthTotal(0),0.01);
 }
 @Test public void obdOnlyLabelsActualRepliesAfterDecodeAndSkipsUnknownPids(){
  int rpm=ObdLocalDiagnosticLog.bitForPid("010C");
  int speed=ObdLocalDiagnosticLog.bitForPid("010D");
  assertTrue(rpm>0);
  assertTrue(speed>0);
  assertTrue(rpm!=speed);
  assertEquals(0,ObdLocalDiagnosticLog.bitForPid("0101"));
  assertEquals(0,ObdLocalDiagnosticLog.bitForPid(null));
  assertFalse(RideReplayTimeline.gapAt(Arrays.asList(0,0,0,1,1),2));
  assertTrue(RideReplayTimeline.gapAt(Arrays.asList(0,0,0,1,1),3));
  assertFalse(RideReplayTimeline.gapAt(Arrays.asList(0,0,0,1,1),0));
 }
 @Test public void committedRestoreReadbackDetectsPostCopyTrackCorruption()throws Exception{
  File root=Files.createTempDirectory("moto-verified-restore-").toFile();
  String name="1700000000000-12345678-1234-1234-1234-123456789abc.track";
  byte[] bytes=TrackFile.point(37.0,127.0,6.0,1700000000000L).getBytes(StandardCharsets.UTF_8);
  ByteArrayOutputStream buffer=new ByteArrayOutputStream();
  try(ZipOutputStream zip=new ZipOutputStream(buffer,StandardCharsets.UTF_8)){
   add(zip,"MOTO-HUB-data.json",Store.empty().toString().getBytes(StandardCharsets.UTF_8));
   add(zip,"README-KO.txt","MOTO HUB 전체 백업\n".getBytes(StandardCharsets.UTF_8));
   add(zip,"rides/"+name,bytes);
   add(zip,"rides/"+name+".done",new byte[0]);
  }
  try(RestoreArchive.Prepared ready=RestoreArchive.prepare(new ByteArrayInputStream(buffer.toByteArray()),root)){
   ready.install(root,json->Files.write(new File(root,"motohub-v3.json").toPath(),json.getBytes(StandardCharsets.UTF_8)));
   String confirmed=ready.verifyInstalled(root);
   assertTrue(confirmed.contains("재검증 통과"));
   assertTrue(confirmed.contains("GPS 1개"));
   Files.write(new File(root,"rides/"+name).toPath(),"tampered".getBytes(StandardCharsets.UTF_8));
   try{ready.verifyInstalled(root);fail("Corrupted GPS must not be shown as verified");}
   catch(IOException expected){assertTrue(expected.getMessage().contains("불일치"));}
  }finally{deleteTree(root.toPath());}
 }
 private static void add(ZipOutputStream zip,String name,byte[] bytes)throws IOException{
  zip.putNextEntry(new ZipEntry(name));zip.write(bytes);zip.closeEntry();
 }
 private static void deleteTree(Path root)throws IOException{
  if(!Files.exists(root))return;
  try(java.util.stream.Stream<Path> paths=Files.walk(root)){
   paths.sorted(Comparator.reverseOrder()).forEach(p->{try{Files.deleteIfExists(p);}catch(IOException ignored){}});
  }
 }
}
