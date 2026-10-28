package com.motohub.next;
import android.content.Context;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=35)
public final class BackupVolumePlanTest {
 @Test public void syntheticSmallVolumeLimitPartitionsWithoutDroppingImages()throws Exception{
  Context context=RuntimeEnvironment.getApplication();
  File root=new File(context.getFilesDir(),"ux73-volume-plan");
  root.mkdirs();
  List<File> made=new ArrayList<>();
  try{
   for(int i=0;i<3;i++){
    File file=new File(root,"garage-photo-123e4567-e89b-12d3-a456-"+String.format(Locale.ROOT,"%012x",i)+".jpg");
    byte[] bytes=new byte[14000];bytes[0]=(byte)255;bytes[1]=(byte)216;bytes[bytes.length-2]=(byte)255;bytes[bytes.length-1]=(byte)217;
    Files.write(file.toPath(),bytes);made.add(file);
   }
   String json=Store.empty().toString(),prefs=UiSettingsArchive.capture(context);
   List<BackupVolumeExporter.Volume> volumes=BackupVolumeExporter.plan(root,"MEDIA",
      json.length(),prefs.length(),60000);
   assertTrue(volumes.size()>1);
   Set<String> names=new HashSet<>();
   for(BackupVolumeExporter.Volume volume:volumes){
    names.addAll(volume.entries);
    ByteArrayOutputStream out=new ByteArrayOutputStream();
    BackupBundle.export(json,root,out,"MEDIA",prefs,volume.entries);
    BackupIntegrity.Result verified=BackupIntegrity.verify(new ByteArrayInputStream(out.toByteArray()));
    assertTrue(verified.entryNames.containsAll(volume.entries));
    assertEquals(volume.entries.size()+3,verified.entryNames.size());
   }
   assertEquals(3,names.size());
  }finally{
   for(File file:made)file.delete();root.delete();
  }
 }
}
