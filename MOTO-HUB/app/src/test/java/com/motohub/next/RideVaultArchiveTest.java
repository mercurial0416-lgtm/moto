package com.motohub.next;
import android.content.Context;
import java.io.*;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=35)
public final class RideVaultArchiveTest {
 @Test public void vaultedGpsIsIncludedAndRestoredWithoutAppearingInActiveHistory()throws Exception{
  Context c=RuntimeEnvironment.getApplication();
  File root=c.getFilesDir(),src=new File(root,"ux73-vault-source"),target=new File(root,"ux73-vault-target");
  src.mkdirs();target.mkdirs();
  File track=new File(TrackFile.directory(src),"1761600000000-123e4567-e89b-12d3-a456-000000000543.track");
  try{
   Files.write(track.toPath(),(TrackFile.point(37.0,127.0,5.0,1761600000000L)+
    TrackFile.point(37.0001,127.0001,5.0,1761600003000L)).getBytes(StandardCharsets.UTF_8));
   TrackCompletion.mark(track,2);
   RideTrash.move(src,track);
   assertFalse(track.exists());
   ByteArrayOutputStream output=new ByteArrayOutputStream();
   BackupBundle.export(Store.empty().toString(),src,output,"FULL",UiSettingsArchive.capture(c));
   BackupIntegrity.Result verified=BackupIntegrity.verify(new ByteArrayInputStream(output.toByteArray()));
   String entry="rides-trash/"+track.getName()+"/"+track.getName();
   assertTrue(verified.entryNames.contains(entry));
   try(RestoreArchive.Prepared prepared=RestoreArchive.prepare(new ByteArrayInputStream(output.toByteArray()),root)){
    assertFalse(prepared.isSplit());
    prepared.install(target,json->{});
   }
   assertTrue(new File(target,entry).isFile());
   assertTrue(new File(target,entry+".done").isFile());
   assertEquals(0,TrackFile.tracks(target).size());
   assertEquals(1,RideTrash.entries(target).size());
   assertEquals(0,RideTrash.recoverInterrupted(target));
  }finally{
   // Test folders are isolated; remove only known synthetic files.
   for(File base:new File[]{src,target}){
    File active=new File(base,"rides"),vault=new File(base,"rides-trash");
    File folder=new File(vault,track.getName());
    File[] entries=folder.listFiles();if(entries!=null)for(File x:entries)x.delete();
    folder.delete();vault.delete();File[] r=active.listFiles();if(r!=null)for(File x:r)x.delete();active.delete();
    base.delete();
   }
  }
 }
 @Test public void interruptedSidecarMoveFinishesAccordingToTrackLocation()throws Exception{
  Context c=RuntimeEnvironment.getApplication();
  File root=new File(c.getFilesDir(),"ux73-vault-interruption");
  root.mkdirs();File rides=TrackFile.directory(root);
  String id="1761600000000-123e4567-e89b-12d3-a456-000000000544.track";
  File source=new File(rides,id),folder=new File(new File(root,"rides-trash"),id);
  folder.mkdirs();
  try{
   Files.write(source.toPath(),(TrackFile.point(37.0,127.0,5.0,1761600000000L)+"").getBytes(StandardCharsets.UTF_8));
   Files.createFile(new File(folder,id+".done").toPath());
   assertEquals(1,RideTrash.recoverInterrupted(root));
   assertTrue(new File(rides,id+".done").exists());
   assertFalse(folder.exists());
  }finally{
   new File(rides,id+".done").delete();source.delete();folder.delete();
   new File(root,"rides-trash").delete();rides.delete();root.delete();
  }
 }
}
