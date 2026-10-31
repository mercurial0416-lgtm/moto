package com.motohub.next;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.Comparator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.junit.Test;
import static org.junit.Assert.*;

/** UX100: inspect the same GPS trash and selected model assets that go into ZIPs. */
public final class UX100BackupCompletionTest {
 private static void cleanup(Path root)throws Exception{
  if(!Files.exists(root,LinkOption.NOFOLLOW_LINKS))return;
  try(java.util.stream.Stream<Path> children=Files.walk(root)){
   for(Path child:children.sorted(Comparator.reverseOrder()).toArray(Path[]::new))
    Files.deleteIfExists(child);
  }
 }
 private static Set<String> entries(byte[] bytes)throws Exception{
  Set<String> names=new HashSet<>();
  try(ZipInputStream zip=new ZipInputStream(new ByteArrayInputStream(bytes))){
   ZipEntry entry;
   while((entry=zip.getNextEntry())!=null){
    assertTrue("no duplicate ZIP entries",names.add(entry.getName()));
    zip.closeEntry();
   }
  }
  return names;
 }
 @Test public void selectedModelsAndGpsTrashAppearInPreflightAndActualZip()throws Exception{
  Path root=Files.createTempDirectory("ux100-full-");
  try{
   Path models=Files.createDirectory(root.resolve("moto-year-photos"));
   String model="abcdef0123456789abcdef01.jpg";
   Files.write(models.resolve(model),new byte[]{(byte)0xff,(byte)0xd8,(byte)0xff,(byte)0xd9});
   byte[] meta="{\"userSelected\":true}".getBytes(StandardCharsets.UTF_8);
   Files.write(models.resolve(model+".json"),meta);
   String ride="1761899000000-"+UUID.randomUUID()+".track";
   Path trash=Files.createDirectories(root.resolve("rides-trash").resolve(ride));
   byte[] gps="37.0,127.0,5.0,1761899000000\n".getBytes(StandardCharsets.UTF_8);
   Files.write(trash.resolve(ride),gps);
   Files.createFile(trash.resolve(ride+".done"));
   BackupReadiness.Summary summary=BackupReadiness.inspect(root.toFile());
   assertEquals(1,summary.trashRides);
   assertEquals(1,summary.selectedModels);
   assertTrue(summary.withinBasicLimits());
   assertEquals(4+meta.length+gps.length,summary.rawBytes);
   ByteArrayOutputStream out=new ByteArrayOutputStream();
   int count=BackupBundle.export("{}",root.toFile(),out,"FULL");
   Set<String> names=entries(out.toByteArray());
   assertEquals("reported entries must include README and all sidecars",names.size(),count);
   assertEquals(6,names.size());
   assertTrue(names.contains("MOTO-HUB-data.json"));
   assertTrue(names.contains("README-KO.txt"));
   assertTrue(names.contains("model-photos/"+model));
   assertTrue(names.contains("model-photos/"+model+".json"));
   assertTrue(names.contains("rides-trash/"+ride+"/"+ride));
   assertTrue(names.contains("rides-trash/"+ride+"/"+ride+".done"));
  }finally{cleanup(root);}
 }
 @Test public void gpsOnlyArchiveDoesNotInspectDamagedMediaFolder()throws Exception{
  Path root=Files.createTempDirectory("ux100-gps-part-");
  try{
   Files.createSymbolicLink(root.resolve("moto-year-photos"),root.resolve("missing-models"));
   ByteArrayOutputStream gps=new ByteArrayOutputStream();
   int count=BackupBundle.export("{}",root.toFile(),gps,"GPS");
   assertEquals(2,count);
   assertEquals(2,entries(gps.toByteArray()).size());
   try{
    BackupBundle.export("{}",root.toFile(),new ByteArrayOutputStream(),"MEDIA");
    fail("MEDIA partition must inspect and reject a dangerous model folder");
   }catch(java.io.IOException expected){
    assertTrue(expected.getMessage().contains("백업 폴더"));
   }
  }finally{cleanup(root);}
 }
 @Test public void quarantineMetadataOverLimitBlocksPreflight()throws Exception{
  Path root=Files.createTempDirectory("ux100-trash-");
  try{
   String ride="1761899000000-"+UUID.randomUUID()+".track";
   Path folder=Files.createDirectories(root.resolve("rides-trash").resolve(ride));
   Files.write(folder.resolve(ride),new byte[]{1,2,3});
   Files.write(folder.resolve(ride+".favorite"),new byte[]{1});
   BackupReadiness.Summary s=BackupReadiness.inspect(root.toFile());
   assertEquals(1,s.trashRides);
   assertEquals(1,s.oversizedFiles);
   assertFalse(s.withinBasicLimits());
  }finally{cleanup(root);}
 }
}
