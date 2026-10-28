package com.motohub.next;

import android.content.Context;
import org.junit.Test;
import org.json.JSONObject;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=35)
public final class UpdateDataFootprintTest {
 @Test public void sameSizeGarageSettingsChangesAreCaught()throws Exception{
  Context context=RuntimeEnvironment.getApplication();
  JSONObject before=Store.empty(),after=Store.empty();
  before.getJSONObject("settings").put("theme","light");
  after.getJSONObject("settings").put("theme","dark");
  assertNotEquals(UpdateDataFootprint.capture(context,before),
    UpdateDataFootprint.capture(context,after));
 }
 @Test public void sameByteCountDifferentGpsContentChangesDigest()throws Exception{
  Context context=RuntimeEnvironment.getApplication();
  File rides=TrackFile.directory(context.getFilesDir());
  File file=new File(rides,"1761600000000-123e4567-e89b-12d3-a456-0000000000fe.track");
  try{
   Files.write(file.toPath(),"37.1,127.1,5,1761600000000\n".getBytes(StandardCharsets.UTF_8));
   String first=UpdateDataFootprint.capture(context,Store.empty());
   assertTrue(first.contains(";contentSha256="));
   Files.write(file.toPath(),"37.2,127.1,5,1761600000000\n".getBytes(StandardCharsets.UTF_8));
   String changed=UpdateDataFootprint.capture(context,Store.empty());
   assertNotEquals(first,changed);
  }finally{file.delete();}
 }
}
