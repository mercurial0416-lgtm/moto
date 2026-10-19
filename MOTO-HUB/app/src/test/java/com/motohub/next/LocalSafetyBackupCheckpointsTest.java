package com.motohub.next;

import android.content.Context;
import android.content.ContextWrapper;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=35)
public class LocalSafetyBackupCheckpointsTest {
 private Context isolated() throws Exception {
  final File root=Files.createTempDirectory("moto-checkpoint-test").toFile();
  return new ContextWrapper(RuntimeEnvironment.getApplication()){
   @Override public File getFilesDir(){return root;}
  };
 }
 @Test public void keepsDailyPreEditAndLatestPostEditRestores() throws Exception {
  Context ctx=isolated();
  JSONObject before=Store.empty();before.getJSONObject("settings").put("marker","before");
  LocalSafetyBackup.snapshot(ctx,before);
  JSONObject after=Store.empty();after.getJSONObject("settings").put("marker","after");
  LocalSafetyBackup.checkpoint(ctx,after);
  File[] daily=LocalSafetyBackup.available(ctx);
  assertEquals(1,daily.length);
  assertEquals("before",LocalSafetyBackup.read(daily[0]).getJSONObject("settings").getString("marker"));
  File[] points=LocalSafetyBackup.recoveryPoints(ctx);
  assertTrue(points.length>=2);
  boolean foundAfter=false;
  for(File file:points){
   if(LocalSafetyBackup.read(file).getJSONObject("settings").optString("marker").equals("after"))foundAfter=true;
  }
  assertTrue(foundAfter);
  for(int i=0;i<20;i++){
   JSONObject updated=Store.empty();updated.getJSONObject("settings").put("change",i);
   LocalSafetyBackup.checkpoint(ctx,updated);
  }
  assertEquals(12,new File(new File(ctx.getFilesDir(),"local-safety-backup"),"updates").listFiles((dir,name)->name.endsWith(".json")).length);
  assertEquals("before",LocalSafetyBackup.read(daily[0]).getJSONObject("settings").getString("marker"));
 }
 @Test public void corruptedPointCannotBeLoaded() throws Exception {
  Context ctx=isolated();
  File file=new File(ctx.getCacheDir(),"invalid-checkpoint.json");
  Files.write(file.toPath(),"{bad json".getBytes(StandardCharsets.UTF_8));
  assertThrows(Exception.class,()->LocalSafetyBackup.read(file));
 }
}
