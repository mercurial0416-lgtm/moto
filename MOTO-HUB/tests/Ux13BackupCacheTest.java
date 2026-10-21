package com.motohub.next;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

/** Cache only picker hints, never an already trusted restore payload. */
public final class Ux13BackupCacheTest {
 static int checks;
 static void ok(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
 static void failRead(File file)throws Exception{
  try{LocalBackupFilePolicy.readBounded(file);throw new AssertionError("unsafe file was read");}
  catch(IOException expected){checks++;}
 }
 public static void main(String[] args)throws Exception{
  Path root=Files.createTempDirectory("moto-cache-ux13-");
  Path outside=Files.createTempFile("moto-outside-", ".json");
  LocalBackupValidationCache.clearForTests();
  try{
   File valid=root.resolve("motohub-2026-10-21.json").toFile();
   Files.write(valid.toPath(),"sample-ok".getBytes(StandardCharsets.UTF_8));
   AtomicInteger scanned=new AtomicInteger();
   java.util.function.BooleanSupplier parser=()->{scanned.incrementAndGet();return true;};
   ok(LocalBackupValidationCache.verify(valid,parser),"first positive");
   ok(LocalBackupValidationCache.verify(valid,parser),"repeat positive");
   ok(scanned.get()==1,"unchanged file is not reparsed twice");
   ok(new String(LocalBackupFilePolicy.readBounded(valid),StandardCharsets.UTF_8).equals("sample-ok"),"bounded read preserves bytes");
   Files.write(valid.toPath(),"sample-invalid-long".getBytes(StandardCharsets.UTF_8));
   ok(LocalBackupValidationCache.verify(valid,parser),"updated positive");
   ok(scanned.get()==2,"size change triggers recheck");
   LocalBackupValidationCache.invalidate(valid);
   ok(!LocalBackupValidationCache.verify(valid,()->{scanned.incrementAndGet();return false;}),"negative check");
   ok(!LocalBackupValidationCache.verify(valid,parser),"negative result temporarily reused");
   ok(scanned.get()==3,"negative not reparsed immediately");
   LocalBackupValidationCache.invalidate(valid);
   ok(LocalBackupValidationCache.verify(valid,parser),"invalidated result reparses");
   ok(scanned.get()==4,"invalidation counter");
   File huge=root.resolve("oversize.json").toFile();
   try(java.io.RandomAccessFile f=new java.io.RandomAccessFile(huge,"rw")){
    f.setLength(LocalBackupFilePolicy.MAX_BYTES+1);
   }
   failRead(huge);
   ok(!LocalBackupValidationCache.verify(huge,parser),"oversize never cached");
   File empty=root.resolve("empty.json").toFile();Files.createFile(empty.toPath());
   failRead(empty);
   ok(!LocalBackupValidationCache.verify(empty,parser),"empty never cached");
   for(int i=0;i<85;i++){
    File file=root.resolve("checkpoint-"+i+".json").toFile();
    Files.write(file.toPath(),new byte[]{42});
    LocalBackupValidationCache.verify(file,()->true);
   }
   ok(LocalBackupValidationCache.sizeForTests()<=LocalBackupValidationCache.MAX_ENTRIES,"bounded LRU entry count");
   try{
    File link=root.resolve("symlink.json").toFile();
    Files.createSymbolicLink(link.toPath(),outside);
    failRead(link);
    ok(!LocalBackupValidationCache.verify(link,parser),"symlink never cached");
    Files.deleteIfExists(link.toPath());
   }catch(UnsupportedOperationException|SecurityException ignored){/* Some runners cannot link. */}
   ok(!LocalBackupValidationCache.verify(root.resolve("missing.json").toFile(),parser),"missing file never cached");
  }finally{
   File[] all=root.toFile().listFiles();
   if(all!=null)for(File f:all)Files.deleteIfExists(f.toPath());
   Files.deleteIfExists(root);Files.deleteIfExists(outside);
   LocalBackupValidationCache.clearForTests();
  }
  System.out.println("PASS UX13 bounded validation cache: "+checks+" checks");
 }
}
