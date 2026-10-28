package com.motohub.next;
import android.content.Context;
import java.io.*;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=35)
public final class GpsBackupProofAndTrashTest {
 @Test public void verifiedGpsHashMustMatchBeforeTrashAndRoundTripRestores()throws Exception{
  Context c=RuntimeEnvironment.getApplication();
  File root=c.getFilesDir(),rides=TrackFile.directory(root);
  File track=new File(rides,"1761600000000-123e4567-e89b-12d3-a456-0000000000ad.track");
  byte[] original=(TrackFile.point(37.0,127.0,5.0,1761600000000L)+
     TrackFile.point(37.0001,127.0001,5.0,1761600003000L)).getBytes(StandardCharsets.UTF_8);
  try{
   Files.write(track.toPath(),original);TrackCompletion.mark(track,2);
   ByteArrayOutputStream bytes=new ByteArrayOutputStream();
   BackupBundle.export(Store.empty().toString(),root,bytes,"GPS");
   BackupIntegrity.Result verified=BackupIntegrity.verify(new ByteArrayInputStream(bytes.toByteArray()));
   assertTrue(verified.trackSha256.containsKey(track.getName()));
   BackupGpsProof.record(c,verified.trackSha256);
   BackupGpsProof.requireExact(c,track);
   byte[] tampered=Arrays.copyOf(original,original.length);
   tampered[3]=tampered[3]==(byte)'0'?(byte)'1':(byte)'0';
   Files.write(track.toPath(),tampered);
   try{BackupGpsProof.requireExact(c,track);fail("same-length mutation must reject");}
   catch(IOException expected){}
   Files.write(track.toPath(),original);
   RideTrash.move(root,track);
   assertFalse(track.exists());
   File trashed=RideTrash.entries(root).stream().filter(f->f.getName().equals(track.getName())).findFirst().orElse(null);
   assertNotNull(trashed);
   assertArrayEquals(original,Files.readAllBytes(trashed.toPath()));
   File restored=RideTrash.restore(root,trashed);
   assertArrayEquals(original,Files.readAllBytes(restored.toPath()));
   assertTrue(new File(restored.getPath()+".done").isFile());
  }finally{
   Files.deleteIfExists(track.toPath());Files.deleteIfExists(new File(track.getPath()+".done").toPath());
   for(File folder:new File(root,"rides-trash").listFiles()==null?new File[0]:new File(root,"rides-trash").listFiles()){
    if(folder.getName().equals(track.getName())){
     File[] children=folder.listFiles();if(children!=null)for(File f:children)f.delete();folder.delete();
    }
   }
   c.getSharedPreferences("verified_full_export",0).edit().clear().commit();
  }
 }
}
