package com.motohub.next;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.junit.Test;
import static org.junit.Assert.*;

/** UX99: backup preflight must never declare an incomplete or corrupted source safe. */
public final class UX99BackupEnumerationTest {
 @Test public void everyStatusSidecarIsCheckedBeforeExport()throws Exception{
  Path root=Files.createTempDirectory("ux99-garage-"),rides=Files.createDirectory(root.resolve("rides"));
  String name="1761899000000-"+UUID.randomUUID()+".track";
  Path track=rides.resolve(name),done=rides.resolve(name+".done"),
       favorite=rides.resolve(name+".favorite"),unmerged=rides.resolve(name+".unmerged");
  byte[] gps="37.0,127.0,5.0,1761899000000\n".getBytes(StandardCharsets.UTF_8);
  try{
   Files.write(track,gps);
   Files.createFile(done);
   BackupReadiness.Summary valid=BackupReadiness.inspect(root.toFile());
   assertEquals(1,valid.rides);
   assertTrue(valid.withinBasicLimits());
   assertEquals(gps.length,valid.rawBytes);
   Files.write(favorite,new byte[]{1});
   BackupReadiness.Summary bad=BackupReadiness.inspect(root.toFile());
   assertFalse("a non-empty favorite marker must block a misleading success status",bad.withinBasicLimits());
   assertTrue(bad.oversizedFiles>0);
   assertEquals(gps.length+1,bad.rawBytes);
   Files.delete(favorite);
   Files.createSymbolicLink(unmerged,root.resolve("missing-sidecar"));
   assertFalse("even dangling sidecar symlinks must fail closed",
       BackupReadiness.inspect(root.toFile()).withinBasicLimits());
  }finally{
   Files.deleteIfExists(unmerged);
   Files.deleteIfExists(favorite);
   Files.deleteIfExists(done);
   Files.deleteIfExists(track);
   Files.deleteIfExists(rides);
   Files.deleteIfExists(root);
  }
 }
 @Test public void freshInstallStillExportsCompleteFullZipWithoutOptionalFolders()throws Exception{
  Path root=Files.createTempDirectory("ux99-empty-garage-");
  try{
   BackupReadiness.Summary fresh=BackupReadiness.inspect(root.toFile());
   assertTrue(fresh.withinBasicLimits());
   assertEquals(0,fresh.rides);
   ByteArrayOutputStream bytes=new ByteArrayOutputStream();
   BackupBundle.export("{}",root.toFile(),bytes,"FULL");
   boolean data=false,readme=false;
   try(ZipInputStream zip=new ZipInputStream(new java.io.ByteArrayInputStream(bytes.toByteArray()))){
    for(ZipEntry entry;(entry=zip.getNextEntry())!=null;zip.closeEntry()){
     if("MOTO-HUB-data.json".equals(entry.getName()))data=true;
     if("README-KO.txt".equals(entry.getName()))readme=true;
    }
   }
   assertTrue(data);assertTrue(readme);
  }finally{Files.deleteIfExists(root);}
 }

 @Test public void presentButUnreadableDirectoryCannotMasqueradeAsEmpty()throws Exception{
  Path root=Files.createTempDirectory("ux99-unreadable-");
  try{
   File unreadable=new File(root.toString()){
    @Override public File[] listFiles(java.io.FilenameFilter filter){return null;}
   };
   for(Class<?> klass:new Class<?>[]{BackupBundle.class,BackupReadiness.class}){
    java.lang.reflect.Method method=klass.getDeclaredMethod(
       "listIfPresent",File.class,java.io.FilenameFilter.class);
    method.setAccessible(true);
    try{
     method.invoke(null,unreadable,
       (java.io.FilenameFilter)(dir,name)->true);
     fail(klass.getSimpleName()+" must not treat an unreadable folder as empty");
    }catch(java.lang.reflect.InvocationTargetException failure){
     assertTrue(failure.getCause() instanceof java.io.IOException);
    }
   }
  }finally{Files.deleteIfExists(root);}
 }
}
