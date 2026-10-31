package com.motohub.next;

import android.content.Context;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.*;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

/** UX97: verified full-ZIP database formatting and all GPS sidecars remain exact. */
@RunWith(RobolectricTestRunner.class)
@Config(manifest=Config.NONE,sdk=28)
public final class UX97BackupIntegrityTest {
 private static String sha(byte[] bytes)throws Exception{
  StringBuilder hex=new StringBuilder();
  for(byte b:MessageDigest.getInstance("SHA-256").digest(bytes))
   hex.append(String.format(Locale.ROOT,"%02x",b&255));
  return hex.toString();
 }
 @Test public void fullZipPrettyJsonAndSidecarMutationAreDetected()throws Exception{
  Context c=RuntimeEnvironment.getApplication();
  File rides=TrackFile.directory(c.getFilesDir());
  String name="1761888000000-"+UUID.randomUUID()+".track";
  File track=new File(rides,name),bike=new File(track.getPath()+".bike");
  byte[] gps="37.0,127.0,5.0,1761888000000\n".getBytes(StandardCharsets.UTF_8);
  byte[] original="123e4567-e89b-12d3-a456-0000000000ab".getBytes(StandardCharsets.US_ASCII);
  byte[] changed="123e4567-e89b-12d3-a456-0000000000ac".getBytes(StandardCharsets.US_ASCII);
  JSONObject db=new JSONObject().put("vehicles",new org.json.JSONArray())
     .put("settings",new JSONObject().put("theme","dark"));
  String pretty=db.toString(2),compact=db.toString();
  Map<String,String> hashes=new HashMap<>(),sidecars=new HashMap<>();
  hashes.put(name,sha(gps));
  sidecars.put("rides/"+name+".bike",sha(original));
  c.getSharedPreferences("verified_full_export",0).edit().clear().commit();
  try{
   Files.write(track.toPath(),gps);
   Files.write(bike.toPath(),original);
   BackupGpsProof.record(c,hashes,sidecars,pretty,true);
   assertTrue(BackupGpsProof.fullCurrentStatus(c,pretty).contains("일치"));
   assertTrue(BackupGpsProof.fullGpsDeepStatus(c,pretty).contains("SHA-256이 일치"));
   assertTrue("test fixture must exercise pretty-vs-compact",!pretty.equals(compact));
   // A later GPS-only export must not replace the full-ZIP's sidecar proof.
   BackupGpsProof.record(c,hashes,Collections.emptyMap(),null,false);
   assertTrue(BackupGpsProof.fullGpsDeepStatus(c,pretty).contains("SHA-256이 일치"));
   long modified=bike.lastModified();
   Files.write(bike.toPath(),changed);assertEquals(original.length,bike.length());
   assertTrue(bike.setLastModified(modified));
   assertTrue(BackupGpsProof.fullGpsDeepStatus(c,pretty).contains("부속 파일 내용이 변경"));
   Files.delete(bike.toPath());
   assertTrue(BackupGpsProof.fullGpsDeepStatus(c,pretty).contains("부속 파일이 삭제"));
  }finally{
   Files.deleteIfExists(bike.toPath());
   Files.deleteIfExists(track.toPath());
   c.getSharedPreferences("verified_full_export",0).edit().clear().commit();
  }
 }
 @Test public void fullArchiveWithMissingSidecarProofMustNotClaimSuccess()throws Exception{
  Context c=RuntimeEnvironment.getApplication();
  c.getSharedPreferences("verified_full_export",0).edit().clear()
    .putString("full_track_names","{}")
    .putString("full_db_sha256",sha("{}".getBytes(StandardCharsets.UTF_8)))
    .commit();
  try{assertTrue(BackupGpsProof.fullGpsDeepStatus(c,"{}").contains("부속 파일 검증 기록"));}
  finally{c.getSharedPreferences("verified_full_export",0).edit().clear().commit();}
 }
}
