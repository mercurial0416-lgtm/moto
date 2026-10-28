package com.motohub.next;

import android.content.Context;
import org.json.JSONObject;
import org.json.JSONArray;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=35)
public final class RideCompletionInboxTest {
 @Test public void retainsTenVerifiedSummariesAndDeduplicatesCompletedTrack()throws Exception{
  Context context=RuntimeEnvironment.getApplication();
  context.getSharedPreferences("ride_completion_inbox",0).edit().clear().commit();
  File rides=TrackFile.directory(context.getFilesDir());
  java.util.List<File> created=new java.util.ArrayList<>();
  try{
   for(int i=0;i<12;i++){
    File track=new File(rides,"1761600000000-123e4567-e89b-12d3-a456-"+
     String.format(java.util.Locale.ROOT,"%012x",i)+".track");
    Files.write(track.toPath(),(TrackFile.point(37.0,127.0,5.0,1761600000000L)+
     TrackFile.point(37.0001,127.0001,5.0,1761600003000L)).getBytes(StandardCharsets.UTF_8));
    TrackCompletion.mark(track,2);
    created.add(track);
    RideCompletionInbox.record(context,track);
    RideCompletionInbox.record(context,track);
   }
   assertEquals(10,RideCompletionInbox.entries(context).size());
   JSONObject latest=RideCompletionInbox.unread(context);
   assertNotNull(latest);
   assertTrue(latest.optString("summary").contains("GPS"));
   String id=latest.optString("id");
   assertTrue(RideCompletionInbox.seen(context,id));
   assertTrue(RideCompletionInbox.entries(context).get(0).optBoolean("seen"));
   assertEquals(10,RideCompletionInbox.entries(context).size());
  }finally{
   for(File file:created){
    new File(file.getPath()+".done").delete();
    file.delete();
   }
   context.getSharedPreferences("ride_completion_inbox",0).edit().clear().commit();
  }
 }
}
