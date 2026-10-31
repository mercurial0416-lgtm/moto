package com.motohub.next;

import java.io.File;
import java.util.*;
import android.content.Context;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

/** UX95: full ZIP freshness includes GPS inventory; low-space protection has exact boundaries. */
@RunWith(RobolectricTestRunner.class)
@Config(manifest=Config.NONE,sdk=28)
public final class UX95ReliabilityTest {
 @Test public void detectsGpsCreatedAfterVerifiedFullZip()throws Exception{
  Context c=RuntimeEnvironment.getApplication();
  File rides=TrackFile.directory(c.getFilesDir());
  String json=new JSONObject().put("vehicles",new org.json.JSONArray()).toString();
  Map<String,String> old=new HashMap<>();
  File[] existing=rides.listFiles((directory,name)->name.matches(BackupFormat.TRACK));
  if(existing!=null)for(File f:existing)old.put(f.getName(),repeat("a",64));
  String name="1761880000000-"+java.util.UUID.randomUUID().toString()+".track";
  File added=new File(rides,name);
  c.getSharedPreferences("verified_full_export",0).edit().clear().commit();
  try{
   BackupGpsProof.record(c,old,Collections.emptyMap(),json,true);
   assertTrue(BackupGpsProof.fullCurrentStatus(c,json).contains("일치"));
   assertTrue(added.createNewFile());
   // The full ZIP omits zero-byte unfinished tracks; such placeholders do not make it stale.
   assertTrue(BackupGpsProof.fullCurrentStatus(c,json).contains("일치"));
   java.nio.file.Files.write(added.toPath(),TrackFile.point(37.0,127.0,5.0,1761880000000L).getBytes(java.nio.charset.StandardCharsets.UTF_8));
   assertTrue(BackupGpsProof.fullCurrentStatus(c,json).contains("새 GPS"));
   assertTrue(BackupGpsProof.fullCurrentStatus(c,json+" ").contains("데이터 변경"));
  }finally{
   added.delete();
   c.getSharedPreferences("verified_full_export",0).edit().clear().commit();
  }
 }
 @Test public void storageThresholdsWarnAndStopOnlyWhenMeasured(){
  long m=1024L*1024L;
  assertFalse(GpsStorageSpacePolicy.warn(-1));
  assertFalse(GpsStorageSpacePolicy.critical(-1));
  assertFalse(GpsStorageSpacePolicy.warn(32*m));
  assertTrue(GpsStorageSpacePolicy.warn(32*m-1));
  assertFalse(GpsStorageSpacePolicy.critical(8*m));
  assertTrue(GpsStorageSpacePolicy.critical(8*m-1));
  assertTrue(GpsStorageSpacePolicy.sufficient(48*m));
 }
 private static String repeat(String s,int count){StringBuilder b=new StringBuilder();for(int i=0;i<count;i++)b.append(s);return b.toString();}
}
