package com.motohub.next;

import android.content.Context;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.zip.*;
import org.json.JSONObject;

/** Real ZIP readback proofs and unsent draft import, no internet or production data. */
@RunWith(RobolectricTestRunner.class)
@Config(manifest=Config.NONE,sdk=28)
public class UX88BackupProofAndDraftTest {
 static void write(File f,String value)throws Exception{
  if(!f.getParentFile().isDirectory())assertTrue(f.getParentFile().mkdirs());
  Files.write(f.toPath(),value.getBytes(StandardCharsets.UTF_8));
 }
 static void put(ZipOutputStream out,String name,String data)throws Exception{
  out.putNextEntry(new ZipEntry(name));
  out.write(data.getBytes(StandardCharsets.UTF_8));out.closeEntry();
 }
 @Test public void zipProofRejectsChangedOrMissingGpsSidecars()throws Exception{
  Context c=RuntimeEnvironment.getApplication();
  String name="1690000000000-"+UUID.randomUUID()+".track";
  File track=new File(new File(c.getFilesDir(),"rides"),name);
  File done=new File(track+".done"),category=new File(track+".category"),favorite=new File(track+".favorite");
  try{
   write(track,"1690000000000,37.0,127.0,0\n");
   write(done,"");write(category,"일상");write(favorite,"");
   ByteArrayOutputStream archive=new ByteArrayOutputStream();
   try(ZipOutputStream zip=new ZipOutputStream(archive,StandardCharsets.UTF_8)){
    put(zip,"MOTO-HUB-data.json",Store.empty().toString());
    put(zip,"rides/"+name,"1690000000000,37.0,127.0,0\n");
    put(zip,"rides/"+name+".done","");
    put(zip,"rides/"+name+".category","일상");
    put(zip,"rides/"+name+".favorite","");
   }
   BackupIntegrity.Result verified=BackupIntegrity.verify(new ByteArrayInputStream(archive.toByteArray()));
   assertEquals(3,verified.sidecarSha256.size());
   BackupGpsProof.record(c,verified.trackSha256,verified.sidecarSha256,Store.empty().toString(),true);
   BackupGpsProof.requireExact(c,track);
   write(category,"와인딩");
   try{BackupGpsProof.requireExact(c,track);fail("category changed since ZIP");}
   catch(IOException expected){assertTrue(expected.getMessage().contains("변경"));}
   write(category,"일상");Files.delete(favorite.toPath());
   try{BackupGpsProof.requireExact(c,track);fail("favorite missing since ZIP");}
   catch(IOException expected){assertTrue(expected.getMessage().contains("사라졌"));}
  }finally{
   Files.deleteIfExists(favorite.toPath());Files.deleteIfExists(category.toPath());
   Files.deleteIfExists(done.toPath());Files.deleteIfExists(track.toPath());
  }
 }
 @Test public void fullZipDraftSnapshotOnlyRestoresKnownBike()throws Exception{
  Context c=RuntimeEnvironment.getApplication();
  String known=UUID.randomUUID().toString(),orphan=UUID.randomUUID().toString();
  FuelDraft.clear(c,known);FuelDraft.clear(c,orphan);
  try{
   assertTrue(FuelDraft.saveDurable(c,known,"20000","8","2500","900","2026-10-30",false));
   assertTrue(FuelDraft.saveDurable(c,orphan,"5000","2","2500","100","2026-10-30",true));
   JSONObject bundle=FuelDraftBackup.snapshot(c);
   FuelDraftBackup.validate(bundle);
   FuelDraft.clear(c,known);FuelDraft.clear(c,orphan);
   JSONObject db=Store.empty();
   db.getJSONArray("vehicles").put(new JSONObject().put("id",known).put("name","known bike"));
   assertEquals(1,FuelDraftBackup.restore(c,bundle,db));
   assertNotNull(FuelDraft.read(c,known));
   assertEquals("20000",FuelDraft.read(c,known).optString("amount"));
   assertNull(FuelDraft.read(c,orphan));
  }finally{FuelDraft.clear(c,known);FuelDraft.clear(c,orphan);}
 }
 @Test public void globalSearchReallySortsGpsBetweenLedgerDates(){
  List<HubScreens.UnifiedSearchHit> hits=new ArrayList<>();
  hits.add(new HubScreens.UnifiedSearchHit("2026-10-28","정비","service","",()->{}));
  hits.add(new HubScreens.UnifiedSearchHit("2026-10-30","GPS 라이딩","ride","",()->{}));
  hits.add(new HubScreens.UnifiedSearchHit("2026-10-29","주유","fuel","",()->{}));
  HubScreens.newestFirst(hits);
  assertEquals("GPS 라이딩",hits.get(0).type);
  assertEquals("주유",hits.get(1).type);
  assertEquals("정비",hits.get(2).type);
 }
}
