package com.motohub.next;

import android.content.Context;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

/** UX96: real-file verification of missing tracks, changed bytes and fresh full ZIP proof. */
@RunWith(RobolectricTestRunner.class)
@Config(manifest=Config.NONE,sdk=28)
public final class UX96IntegrityTest {
 private static String hash(byte[] data)throws Exception{
  StringBuilder out=new StringBuilder();
  for(byte v:MessageDigest.getInstance("SHA-256").digest(data))
   out.append(String.format(java.util.Locale.ROOT,"%02x",v&255));
  return out.toString();
 }
 @Test public void detectsDeletedAndModifiedTrackEvenWithMatchingDatabase()throws Exception{
  Context c=RuntimeEnvironment.getApplication();
  File rides=TrackFile.directory(c.getFilesDir());
  String name="1761888000000-"+java.util.UUID.randomUUID()+".track";
  File file=new File(rides,name);
  byte[] initial="37.0,127.0,5.0,1761888000000\n".getBytes(StandardCharsets.UTF_8);
  byte[] changed="37.1,127.0,5.0,1761888000000\n".getBytes(StandardCharsets.UTF_8);
  String json="{\"ux96\":true}";
  Map<String,String> hashes=new HashMap<>();
  hashes.put(name,hash(initial));
  c.getSharedPreferences("verified_full_export",0).edit().clear().commit();
  try{
   Files.write(file.toPath(),initial);
   BackupGpsProof.record(c,hashes,new HashMap<>(),json,true);
   assertTrue(BackupGpsProof.fullCurrentStatus(c,json).contains("일치"));
   assertTrue(BackupGpsProof.fullGpsDeepStatus(c,json).contains("SHA-256이 일치"));
   long time=file.lastModified();
   Files.write(file.toPath(),changed);
   assertEquals(initial.length,file.length());
   assertTrue(file.setLastModified(time)); // Metadata-only status cannot prove unchanged bytes.
   assertTrue(BackupGpsProof.fullGpsDeepStatus(c,json).contains("내용이 변경"));
   Files.delete(file.toPath());
   assertTrue(BackupGpsProof.fullCurrentStatus(c,json).contains("삭제"));
   assertTrue(BackupGpsProof.fullGpsDeepStatus(c,json).contains("삭제"));
  }finally{
   Files.deleteIfExists(file.toPath());
   c.getSharedPreferences("verified_full_export",0).edit().clear().commit();
  }
 }
}
