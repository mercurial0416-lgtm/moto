package com.motohub.next;
import android.content.Context;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=35)
public final class SplitBackupTest {
 @Test public void splitRestoreAddsOnlyItsOwnAssetsAndForbidsFullOverwrite()throws Exception{
  Context context=RuntimeEnvironment.getApplication();
  File base=context.getFilesDir(),source=new File(base,"backup-source-542"),
   destination=new File(base,"backup-target-542");
  source.mkdirs();destination.mkdirs();TrackFile.directory(source);
  File pic=new File(source,"garage-photo-123e4567-e89b-12d3-a456-426614174000.jpg");
  try{
   Files.write(pic.toPath(),new byte[]{(byte)255,(byte)216,(byte)255,(byte)217});
   ByteArrayOutputStream bytes=new ByteArrayOutputStream();
   BackupBundle.export(Store.empty().toString(),source,bytes,"MEDIA");
   BackupIntegrity.Result validated=BackupIntegrity.verify(new ByteArrayInputStream(bytes.toByteArray()));
   assertEquals(0,validated.tracks);
   try(RestoreArchive.Prepared imported=RestoreArchive.prepare(new ByteArrayInputStream(bytes.toByteArray()),base)){
    assertTrue(imported.isSplit());assertEquals("MEDIA",imported.part);
    try{imported.install(destination,json->{});fail("partition must not run full restore");}
    catch(IOException expected){}
    assertEquals(1,imported.installPart(destination));
    assertEquals(0,imported.installPart(destination));
    assertTrue(new File(destination,pic.getName()).isFile());
   }
  }finally{pic.delete();new File(destination,pic.getName()).delete();source.delete();destination.delete();}
 }
}
