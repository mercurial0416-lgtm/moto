package com.motohub.next;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

public final class ReceiptSlotsRoundTripTest {
 static int checks;
 static void ok(boolean result,String name){checks++;if(!result)throw new AssertionError(name);}
 static byte[] jpeg(int n){return new byte[]{(byte)255,(byte)216,(byte)n,(byte)(n+5),(byte)255,(byte)217};}
 static void clean(File dir)throws Exception{
  if(dir.isDirectory()){File[] a=dir.listFiles();if(a!=null)for(File f:a)clean(f);}Files.deleteIfExists(dir.toPath());
 }
 public static void main(String[] ignored)throws Exception{
  String id="12345678-1234-1234-1234-123456789abc";
  File from=Files.createTempDirectory("moto-ux3-photo-from").toFile();
  File target=Files.createTempDirectory("moto-ux3-photo-to").toFile();
  try{
   for(int slot=1;slot<=ReceiptSlots.MAX_SLOTS;slot++){
    Files.write(new File(from,ReceiptSlots.filename(id,slot)).toPath(),jpeg(slot));
   }
   ByteArrayOutputStream archive=new ByteArrayOutputStream();BackupBundle.export("{}",from,archive);
   BackupIntegrity.Result inspected=BackupIntegrity.verify(new ByteArrayInputStream(archive.toByteArray()));
   ok(BackupReadiness.inspect(from).receipts==4,"four receipts verified in export");
   try(RestoreArchive.Prepared restore=RestoreArchive.prepare(new ByteArrayInputStream(archive.toByteArray()),target)){
    ok(restore.receipts==4,"four receipts in prepared archive");
    restore.install(target,data->{});
   }
   for(int slot=1;slot<=4;slot++){
    ok(Arrays.equals(jpeg(slot),Files.readAllBytes(new File(target,ReceiptSlots.filename(id,slot)).toPath())),"byte-exact slot "+slot);
   }
   char[] password="UX3-Test-Strong-Backup-Receipt-2026".toCharArray();
   ByteArrayOutputStream crypt=new ByteArrayOutputStream();
   try(OutputStream secure=BackupCrypto.encrypt(crypt,password)){secure.write(archive.toByteArray());}
   File decrypted=BackupCrypto.decryptToPrivateFile(new ByteArrayInputStream(crypt.toByteArray()),password,target);
   try(InputStream stream=new FileInputStream(decrypted)){
    BackupIntegrity.verify(stream);ok(true,"encrypted backup authenticated");
   }
   Files.deleteIfExists(decrypted.toPath());
   // Old backups containing only the original unsuffixed name are still accepted.
   File legacy=Files.createTempDirectory("moto-old-receipt").toFile();
   try{
    Files.write(new File(legacy,ReceiptSlots.filename(id,1)).toPath(),jpeg(1));
    ByteArrayOutputStream legacyArchive=new ByteArrayOutputStream();BackupBundle.export("{}",legacy,legacyArchive);
    BackupIntegrity.verify(new ByteArrayInputStream(legacyArchive.toByteArray()));ok(BackupReadiness.inspect(legacy).receipts==1,"legacy single photo still valid");
   }finally{clean(legacy);}
  }finally{clean(from);clean(target);}
  System.out.println("PASS UX3 four-photo backup/restore/encryption: "+checks+" assertions");
 }
}
