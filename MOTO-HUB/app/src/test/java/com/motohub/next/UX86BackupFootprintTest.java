package com.motohub.next;

import android.content.Context;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.UUID;
import static org.junit.Assert.*;

/** Real temporary files; changing only metadata must change the audit digest. */
@RunWith(RobolectricTestRunner.class)
@Config(manifest=Config.NONE,sdk=28)
public class UX86BackupFootprintTest {
 private static String name(){return "1690000000000-"+UUID.randomUUID().toString()+".track";}
 private static void write(File file,String data)throws Exception{
  File parent=file.getParentFile();
  assertTrue(parent.isDirectory()||parent.mkdirs());
  Files.write(file.toPath(),data.getBytes(StandardCharsets.UTF_8));
 }
 @Test public void favoriteAndCategoryAreIncludedInFingerprint()throws Exception{
  Context app=RuntimeEnvironment.getApplication();
  File track=new File(new File(app.getFilesDir(),"rides"),name());
  File favorite=new File(track.getPath()+".favorite");
  File category=new File(track.getPath()+".category");
  try{
   write(track,"committed gps sample");
   String original=UpdateDataFootprint.capture(app,Store.empty());
   write(favorite,"");
   String marked=UpdateDataFootprint.capture(app,Store.empty());
   assertNotEquals(original,marked);
   write(category,"일상");
   String categorized=UpdateDataFootprint.capture(app,Store.empty());
   assertNotEquals(marked,categorized);
   assertTrue(categorized.contains("gpsSidecars=2"));
   write(category,"와인딩");
   assertNotEquals(categorized,UpdateDataFootprint.capture(app,Store.empty()));
  }finally{
   Files.deleteIfExists(category.toPath());
   Files.deleteIfExists(favorite.toPath());
   Files.deleteIfExists(track.toPath());
  }
 }
 @Test public void quarantineBytesAndBindingAreIncludedInFingerprint()throws Exception{
  Context app=RuntimeEnvironment.getApplication();
  String id=name();
  File folder=new File(new File(app.getFilesDir(),"rides-trash"),id);
  File source=new File(folder,id);
  File binding=new File(folder,id+".bike");
  try{
   String before=UpdateDataFootprint.capture(app,Store.empty());
   write(source,"old gps");
   String after=UpdateDataFootprint.capture(app,Store.empty());
   assertNotEquals(before,after);
   assertTrue(after.contains("trashTracks=1"));
   write(binding,"owner-one");
   String withBinding=UpdateDataFootprint.capture(app,Store.empty());
   assertNotEquals(after,withBinding);
   write(binding,"owner-two");
   assertNotEquals(withBinding,UpdateDataFootprint.capture(app,Store.empty()));
  }finally{
   Files.deleteIfExists(binding.toPath());Files.deleteIfExists(source.toPath());
   Files.deleteIfExists(folder.toPath());
  }
 }
 @Test public void oldInstallBaselineHasIdenticalScopeAcrossFootprintUpgrade()throws Exception{
  Context app=RuntimeEnvironment.getApplication();
  File track=new File(new File(app.getFilesDir(),"rides"),name());
  File binding=new File(track.getPath()+".bike");
  File favorite=new File(track.getPath()+".favorite");
  File category=new File(track.getPath()+".category");
  try{
   write(track,"committed gps");
   write(binding,"original bike ID");
   String legacy=UpdateDataFootprint.captureLegacy(app,Store.empty());
   assertTrue(legacy.startsWith("schema4;"));
   assertFalse(legacy.contains("trashTracks="));
   write(favorite,"");
   write(category,"와인딩");
   assertEquals("old baseline must not count NEW sidecars",
      legacy,UpdateDataFootprint.captureLegacy(app,Store.empty()));
   String expanded=UpdateDataFootprint.capture(app,Store.empty());
   assertTrue(expanded.startsWith("schema6;"));
   assertTrue(UpdateDataFootprint.captureLegacy5(app,Store.empty()).startsWith("schema5;"));
   assertNotEquals(legacy,expanded);
   write(binding,"different bike ID");
   assertNotEquals("previously audited sidecars MUST stay protected",
       legacy,UpdateDataFootprint.captureLegacy(app,Store.empty()));
  }finally{
   Files.deleteIfExists(category.toPath());Files.deleteIfExists(favorite.toPath());
   Files.deleteIfExists(binding.toPath());Files.deleteIfExists(track.toPath());
  }
 }
 @Test public void orphanLiveSidecarIsDetectedWithoutDeletingIt()throws Exception{
  Context app=RuntimeEnvironment.getApplication();
  File orphan=new File(new File(app.getFilesDir(),"rides"),name()+".favorite");
  try{
   write(orphan,"");
   String legacy=UpdateDataFootprint.captureLegacy(app,Store.empty());
   assertTrue(legacy.startsWith("schema4;"));
   try{
    UpdateDataFootprint.capture(app,Store.empty());
    fail("stray favorite without GPS original must be reported");
   }catch(java.io.IOException expected){assertTrue(expected.getMessage().contains("GPS 원본 없이"));}
   assertTrue("audit must not delete recovery evidence",Files.exists(orphan.toPath()));
  }finally{Files.deleteIfExists(orphan.toPath());}
 }
 @Test public void orphanTrashSidecarIsDetectedAndPreserved()throws Exception{
  Context app=RuntimeEnvironment.getApplication();
  String main=name(),stray=name();
  File folder=new File(new File(app.getFilesDir(),"rides-trash"),main);
  File source=new File(folder,main), orphan=new File(folder,stray+".bike");
  try{
   write(source,"quarantined GPS");
   write(orphan,UUID.randomUUID().toString());
   try{
    UpdateDataFootprint.capture(app,Store.empty());
    fail("stray binding without source GPS must be reported");
   }catch(java.io.IOException expected){assertTrue(expected.getMessage().contains("GPS 원본 없이"));}
   assertTrue(Files.exists(source.toPath()));
   assertTrue(Files.exists(orphan.toPath()));
  }finally{
   Files.deleteIfExists(orphan.toPath());
   Files.deleteIfExists(source.toPath());
   Files.deleteIfExists(folder.toPath());
  }
 }
 @Test public void gpsOnlyProofIsClearlyLabeledAndDoesNotGrantBikeCascadeDeletion()throws Exception{
  Context app=RuntimeEnvironment.getApplication();
  app.getSharedPreferences("verified_full_export",0).edit().clear().commit();
  File track=new File(app.getFilesDir(),name());
  Map<String,String> hashes=Map.of(track.getName(),"a".repeat(64));
  BackupGpsProof.record(app,hashes,null,false);
  assertTrue("legacy-only proof must request fresh ZIP",
    BackupGpsProof.status(app,track).contains("새 ZIP"));
  BackupGpsProof.record(app,hashes,Map.of(),null,false);
  assertTrue(BackupGpsProof.status(app,track).contains("GPS 분할 ZIP"));
  try{
   BackupGpsProof.requireFullCurrent(app,Store.empty().toString());
   fail("GPS partition may not authorize vehicle cascade delete");
  }catch(java.io.IOException expected){assertTrue(expected.getMessage().contains("전체 ZIP"));}
  String current=Store.empty().toString();
  // A verified full ZIP has a real local GPS origin; the legacy test used only a synthetic hash.
  File source=new File(TrackFile.directory(app.getFilesDir()),track.getName());
  java.nio.file.Files.write(source.toPath(),TrackFile.point(37.0,127.0,5.0,1761888000000L).getBytes(java.nio.charset.StandardCharsets.UTF_8));
  try{
   BackupGpsProof.record(app,hashes,Map.of(),current,true);
   assertTrue(BackupGpsProof.status(app,track).contains("전체 ZIP"));
   BackupGpsProof.requireFullCurrent(app,current);
  }finally{java.nio.file.Files.deleteIfExists(source.toPath());}
  try{
   BackupGpsProof.requireFullCurrent(app,current+"modified");
   fail("stale full-backup snapshot must be refused");
  }catch(java.io.IOException expected){assertTrue(expected.getMessage().contains("변경"));}
 }
}
