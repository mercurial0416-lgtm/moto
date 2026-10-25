package com.motohub.next;

import android.content.Context;
import android.content.ContextWrapper;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.ZoneId;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=35)
public class LocalSafetyBackupTest {
 private static void clean(Path path)throws Exception {
  if(Files.isDirectory(path,LinkOption.NOFOLLOW_LINKS)){
   try(java.util.stream.Stream<Path> list=Files.list(path)){
    for(Path child:list.toArray(Path[]::new))clean(child);
   }
  }
  Files.deleteIfExists(path);
 }
 @Test public void corruptBackupsAreNotOfferedAndCannotReplaceDailySnapshot()throws Exception {
  Path temp=Files.createTempDirectory("moto-android-ux12-");
  Context context=new ContextWrapper(RuntimeEnvironment.getApplication()){
   @Override public File getFilesDir(){return temp.toFile();}
  };
  try {
   File dailyDir=new File(temp.toFile(),"local-safety-backup");
   assertTrue(dailyDir.mkdirs());
   String today=LocalDate.now(ZoneId.of("Asia/Seoul")).toString();
   File todays=new File(dailyDir,"motohub-"+today+".json");
   JSONObject invalid=new JSONObject().put("version",3).put("vehicles","bad");
   LocalSafetyBackup.snapshot(context,invalid);
   assertFalse("Do not save malformed daily snapshot",todays.exists());
   JSONObject valid=Store.empty();
   LocalSafetyBackup.snapshot(context,valid);
   assertTrue(todays.isFile());
   byte[] before=Files.readAllBytes(todays.toPath());
   LocalSafetyBackup.snapshot(context,Store.empty().put("settings",new JSONObject().put("changed",true)));
   assertArrayEquals("Do not replace pre-edit daily snapshot",before,Files.readAllBytes(todays.toPath()));
   assertEquals(1,LocalSafetyBackup.available(context).length);
   File bad=new File(dailyDir,"motohub-2024-09-20.json");
   Files.write(bad.toPath(),"{\"vehicles\":{\"bad\":1}}".getBytes(StandardCharsets.UTF_8));
   assertEquals("Corrupted dated backup not offered for restore",1,LocalSafetyBackup.available(context).length);
   LocalSafetyBackup.checkpoint(context,valid);
   assertEquals(2,LocalSafetyBackup.recoveryPoints(context).length);
   assertNotNull(LocalSafetyBackup.read(todays));
  }finally{clean(temp);}
 }
}
