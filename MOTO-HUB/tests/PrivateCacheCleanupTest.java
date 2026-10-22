package com.motohub.next;
import java.io.*;
import java.nio.file.*;
import java.util.*;
public final class PrivateCacheCleanupTest {
 static int passed=0;
 static void ok(boolean state,String note){if(!state)throw new AssertionError(note);passed++;}
 static void write(File f)throws Exception{Files.writeString(f.toPath(),"GPS-PRIVATE-DATA");}
 public static void main(String[] args)throws Exception{
  File root=Files.createTempDirectory("moto-di-clean-test").toFile();
  File cache=new File(root,"cache"),files=new File(root,"files");ok(cache.mkdirs(),"make cache");ok(files.mkdirs(),"make files");
  File privateZip=new File(cache,"moto-secure-Ab0_321.zip");write(privateZip);
  File unrelated=new File(cache,"family-photos.zip");write(unrelated);
  File unknown=new File(cache,"moto-secure-1234.txt");write(unknown);
  File protectedRollback=new File(files,"moto-rollback-12345678-1234-1234-1234-123456789012");ok(protectedRollback.mkdirs(),"make rollback");
  File rollbackFile=new File(protectedRollback,"some.track");write(rollbackFile);
  String oldId="11111111-1111-1111-1111-111111111111";
  File oldStage=new File(files,"moto-stage-"+oldId);ok(oldStage.mkdirs(),"stage");
  File rideDir=new File(oldStage,"rides");ok(rideDir.mkdirs(),"rides");
  File track=new File(rideDir,"1728459876512-12345678-1234-1234-1234-123456789abc.track");write(track);
  // A real ZIP restore can stage receipts alongside garage photos and GPS.
  String receiptName=ReceiptSlots.filename("12345678-1234-1234-1234-123456789abc",2);
  ok(receiptName.matches(BackupFormat.RECEIPT),"valid staged receipt filename");
  write(new File(oldStage,receiptName));
  File stagedModelDir=new File(oldStage,"moto-year-photos");ok(stagedModelDir.mkdirs(),"model cache staging");
  write(new File(stagedModelDir,"abcdef0123456789abcdef01.jpg"));
  write(new File(stagedModelDir,"abcdef0123456789abcdef01.jpg.json"));
  oldStage.setLastModified(System.currentTimeMillis()-PrivateCacheCleanup.STAGE_EXPIRY_MS-1000);
  File freshStage=new File(files,"moto-stage-22222222-2222-2222-2222-222222222222");ok(freshStage.mkdirs(),"recent stage");
  File invalidStage=new File(files,"moto-stage-33333333-3333-3333-3333-333333333333");ok(invalidStage.mkdirs(),"invalid stage");
  write(new File(invalidStage,"unknown.data"));
  invalidStage.setLastModified(System.currentTimeMillis()-PrivateCacheCleanup.STAGE_EXPIRY_MS-1000);
  int removed=PrivateCacheCleanup.atProcessStart(cache,files,System.currentTimeMillis());
  ok(removed==2,"only secure decrypted zip and old known stage removed: "+removed);
  ok(!privateZip.exists(),"decrypted private zip removed");
  ok(!oldStage.exists(),"old stage removed");
  ok(unrelated.exists()&&unknown.exists(),"unrelated files preserved");
  ok(protectedRollback.exists()&&rollbackFile.exists(),"rollbacks preserved");
  ok(freshStage.exists(),"fresh active stage preserved");
  ok(invalidStage.exists(),"unexpected stage content preserved");
  File reentry=new File(cache,"moto-secure-76543.zip");write(reentry);
  PrivateCacheCleanup.atProcessStart(cache,files,System.currentTimeMillis());
  PrivateCacheCleanup.atProcessStart(cache,files,System.currentTimeMillis());
  ok(reentry.exists(),"cleanup only once per process");
  // Never traverse out of private cache through a symlink.
  File outside=new File(root,"moto-secure-54321.zip");write(outside);
  Path symlink=new File(cache,"moto-secure-54321.zip").toPath();Files.createSymbolicLink(symlink,outside.toPath());
  PrivateCacheCleanup.cleanup(cache,files,System.currentTimeMillis());
  ok(outside.exists(),"symlink target untouched");
  System.out.println("PASS DI secure cache cleanup "+passed+" assertions");
 }
}
