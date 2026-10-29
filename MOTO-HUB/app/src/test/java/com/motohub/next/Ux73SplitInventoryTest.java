package com.motohub.next;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
public final class Ux73SplitInventoryTest {
 @Test public void everyGpsOrMediaItemCanBeReadBackAndReimported()throws Exception{
  File source=Files.createTempDirectory("ux73-split-input").toFile();
  File dest=Files.createTempDirectory("ux73-split-target").toFile();
  try{
   File rides=TrackFile.directory(source);
   File track=new File(rides,"1761600000000-123e4567-e89b-12d3-a456-0000000000bb.track");
   Files.write(track.toPath(),(TrackFile.point(37,127,5,1761600000000L)+
    TrackFile.point(37.001,127.001,5,1761600003000L)).getBytes(StandardCharsets.UTF_8));
   TrackCompletion.mark(track,2);
   File photo=new File(source,"garage-photo-123e4567-e89b-12d3-a456-426614174000.jpg");
   Files.write(photo.toPath(),new byte[]{(byte)255,(byte)216,(byte)255,(byte)217});
   BatchArchiveExport.Plan plan=BatchArchiveExport.plan(source);
   assertEquals(2,plan.parts.size());
   Set<String> seen=new HashSet<>();
   String id="123e4567-e89b-12d3-a456-426614174001";
   for(BatchArchiveExport.Part part:plan.parts){
    ByteArrayOutputStream raw=new ByteArrayOutputStream();
    BatchArchiveExport.write(part,"{\"schema\":3}",id,raw);
    byte[] archive=raw.toByteArray();
    BackupIntegrity.Result checked=BackupIntegrity.verify(new ByteArrayInputStream(archive));
    assertEquals(part.expectedEntries(),checked.entryNames);
    try(RestoreArchive.Prepared prepared=RestoreArchive.prepare(new ByteArrayInputStream(archive),dest)){
     assertTrue(prepared.isSplit());
     assertTrue(prepared.batchRecord.contains(id));
     prepared.installPart(dest);
    }
    for(BatchArchiveExport.Asset asset:part.assets)assertTrue(seen.add(asset.name));
   }
   assertEquals(plan.assets,seen.size());
   assertTrue(new File(dest,"rides/"+track.getName()).exists());
   assertTrue(new File(dest,photo.getName()).exists());
  }finally{delete(source);delete(dest);}
 }
 private static void delete(File file){
  if(file.isDirectory()){File[] children=file.listFiles();if(children!=null)for(File child:children)delete(child);}
  file.delete();
 }
}
