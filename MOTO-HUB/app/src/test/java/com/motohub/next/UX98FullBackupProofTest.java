package com.motohub.next;

import android.content.Context;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

/** UX98: no verification stamp for ZIP/source drift; cascade delete requires exact GPS bytes. */
@RunWith(RobolectricTestRunner.class)
@Config(manifest=Config.NONE,sdk=28)
public final class UX98FullBackupProofTest {
 static String sha(byte[] bytes)throws Exception{
  StringBuilder s=new StringBuilder();
  for(byte b:MessageDigest.getInstance("SHA-256").digest(bytes))s.append(String.format(Locale.ROOT,"%02x",b&255));
  return s.toString();
 }
 static void mustFail(RunnableIO work)throws Exception{
  try{work.run();fail("the backup safety proof must fail closed");}
  catch(IOException expected){assertNotNull(expected.getMessage());}
 }
 interface RunnableIO{void run()throws Exception;}
 @Test public void backupReadbackCannotCertifyAlteredSourceAndFullDeleteProofChecksTrack()throws Exception{
  Context c=RuntimeEnvironment.getApplication();
  File rides=TrackFile.directory(c.getFilesDir());
  String name="1761891000000-"+UUID.randomUUID()+".track";
  File track=new File(rides,name);
  byte[] clean="37.0,127.0,5.0,1761891000000\n".getBytes(StandardCharsets.UTF_8);
  byte[] damaged="37.1,127.0,5.0,1761891000000\n".getBytes(StandardCharsets.UTF_8);
  String db="{\"verified\":true}";
  Map<String,String> hashes=new HashMap<>();hashes.put(name,sha(clean));
  c.getSharedPreferences("verified_full_export",0).edit().clear().commit();
  try{
   Files.write(track.toPath(),damaged);
   mustFail(()->BackupGpsProof.recordVerifiedZip(c,hashes,Collections.emptyMap(),db,true));
   assertFalse(c.getSharedPreferences("verified_full_export",0).contains("full_db_sha256"));
   Files.write(track.toPath(),clean);
   BackupGpsProof.recordVerifiedZip(c,hashes,Collections.emptyMap(),db,true);
   BackupGpsProof.requireFullDeepCurrent(c,db);
   long time=track.lastModified();
   Files.write(track.toPath(),damaged);
   assertEquals(clean.length,track.length());
   assertTrue(track.setLastModified(time)); // size and mtime can remain identical
   mustFail(()->BackupGpsProof.requireFullDeepCurrent(c,db));
   mustFail(()->BackupGpsProof.recordVerifiedZip(c,hashes,Collections.emptyMap(),db,true));
  }finally{
   Files.deleteIfExists(track.toPath());
   c.getSharedPreferences("verified_full_export",0).edit().clear().commit();
  }
 }
 @Test public void changedOrNewSidecarCannotPassBackupCompletion()throws Exception{
  Context c=RuntimeEnvironment.getApplication();
  File rides=TrackFile.directory(c.getFilesDir());
  String name="1761892000000-"+UUID.randomUUID()+".track";
  File track=new File(rides,name),category=new File(track.getPath()+".category");
  byte[] gps="37.0,127.0,5.0,1761892000000\n".getBytes(StandardCharsets.UTF_8);
  byte[] old="daily".getBytes(StandardCharsets.UTF_8),changed="sport".getBytes(StandardCharsets.UTF_8);
  Map<String,String> hashes=Collections.singletonMap(name,sha(gps));
  Map<String,String> sidecars=Collections.singletonMap("rides/"+name+".category",sha(old));
  try{
   Files.write(track.toPath(),gps);
   Files.write(category.toPath(),changed);
   mustFail(()->BackupGpsProof.recordVerifiedZip(c,hashes,sidecars,"{}",true));
   Files.write(category.toPath(),old);
   BackupGpsProof.recordVerifiedZip(c,hashes,sidecars,"{}",true);
   BackupGpsProof.requireFullDeepCurrent(c,"{}");
   Files.delete(category.toPath());
   mustFail(()->BackupGpsProof.requireFullDeepCurrent(c,"{}"));
  }finally{
   Files.deleteIfExists(category.toPath());
   Files.deleteIfExists(track.toPath());
   c.getSharedPreferences("verified_full_export",0).edit().clear().commit();
  }
 }
}
