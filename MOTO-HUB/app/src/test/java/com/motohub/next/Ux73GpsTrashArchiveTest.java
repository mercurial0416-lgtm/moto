package com.motohub.next;
import java.io.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import org.junit.Test;
import static org.junit.Assert.*;
public final class Ux73GpsTrashArchiveTest {
 private static File source(File root)throws Exception{
  File f=new File(root,"1761600000000-123e4567-e89b-12d3-a456-0000000000cd.track");
  Files.write(f.toPath(),(TrackFile.point(37.0,127.0,5,1761600000000L)+
   TrackFile.point(37.001,127.001,5,1761600003000L)).getBytes(StandardCharsets.UTF_8));
  TrackCompletion.mark(f,2);
  return f;
 }
 @Test public void restoreCompletedTrashWithoutPromotingToActiveHistory()throws Exception{
  File root=Files.createTempDirectory("ux73-backup-source").toFile();
  File destination=Files.createTempDirectory("ux73-backup-restored").toFile();
  try{
   File track=source(TrackFile.directory(root));
   RideTrash.move(root,track);
   ByteArrayOutputStream buffer=new ByteArrayOutputStream();
   BackupBundle.export("{\"schema\":3}",root,buffer);
   BackupIntegrity.Result verified=BackupIntegrity.verify(new ByteArrayInputStream(buffer.toByteArray()));
   assertEquals(1,verified.tracks);
   String stored="rides-trash/"+track.getName()+"/"+track.getName();
   assertTrue(verified.entryNames.contains(stored));
   assertTrue(verified.entryNames.contains(stored+".done"));
   try(RestoreArchive.Prepared ready=RestoreArchive.prepare(new ByteArrayInputStream(buffer.toByteArray()),destination)){
    assertEquals(1,ready.rides);
    ready.install(destination,json->{});
    assertTrue(new File(destination,stored).isFile());
    assertTrue(new File(destination,stored+".done").isFile());
    assertFalse(new File(destination,"rides/"+track.getName()).exists());
    File restored=RideTrash.restore(destination,new File(destination,stored));
    assertTrue(restored.isFile());
    assertTrue(new File(restored+".done").isFile());
   }
  }finally{
   delete(root);delete(destination);
  }
 }
 @Test public void crashRepairCompletesInterruptedMoveWithoutLosingMarkers()throws Exception{
  File root=Files.createTempDirectory("ux73-crash-source").toFile();
  try{
   File track=source(TrackFile.directory(root));
   File trashed=new File(root,"rides-trash/"+track.getName());
   assertTrue(trashed.mkdirs());
   Files.move(new File(track+".done").toPath(),new File(trashed,track.getName()+".done").toPath());
   assertEquals(1,RideTrash.recoverInterrupted(root));
   assertTrue(new File(track+".done").exists());
   RideTrash.move(root,track);
   File tucked=new File(trashed,track.getName());
   assertTrue(tucked.exists());
   Files.move(new File(trashed,track.getName()+".done").toPath(),new File(track+".done").toPath());
   assertEquals(1,RideTrash.recoverInterrupted(root));
   assertTrue(new File(trashed,track.getName()+".done").exists());
   assertFalse(new File(track+".done").exists());
  }finally{delete(root);}
 }
 private static void delete(File file){
  if(file.isDirectory()){File[] children=file.listFiles();if(children!=null)for(File child:children)delete(child);}
  file.delete();
 }
}
