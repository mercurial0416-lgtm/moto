package com.motohub.next;
import java.io.*;import java.nio.file.*;import java.util.*;
public final class ReceiptMultiArchiveTest {
 static int checks;static void ok(boolean x,String m){checks++;if(!x)throw new AssertionError(m);}
 static final String ID="12345678-1234-1234-1234-123456789abc";
 static void remove(File file)throws Exception{if(file.isDirectory())for(File f:Objects.requireNonNull(file.listFiles()))remove(f);Files.deleteIfExists(file.toPath());}
 public static void main(String[] x)throws Exception{
  File src=Files.createTempDirectory("moto-multiple-src").toFile();File dest=Files.createTempDirectory("moto-multiple-dest").toFile();
  try {
   for(int i=1;i<=4;i++)Files.write(ReceiptSlots.file(src,ID,i).toPath(),new byte[]{(byte)255,(byte)216,(byte)i,20,(byte)255,(byte)217});
   ByteArrayOutputStream zip=new ByteArrayOutputStream();BackupBundle.export("{}",src,zip);
   BackupIntegrity.Result result=BackupIntegrity.verify(new ByteArrayInputStream(zip.toByteArray()));
   ok(result!=null,"zip integrity");ok(BackupReadiness.inspect(src).receipts==4,"backup readiness");
   try(RestoreArchive.Prepared ready=RestoreArchive.prepare(new ByteArrayInputStream(zip.toByteArray()),dest)){
    ok(ready.receipts==4,"all attachments staged");ready.install(dest,json->{});
   }
   for(int i=1;i<=4;i++)ok(Arrays.equals(Files.readAllBytes(ReceiptSlots.file(src,ID,i).toPath()),Files.readAllBytes(ReceiptSlots.file(dest,ID,i).toPath())),"slot "+i+" roundtrip");
   char[] password="this-is-our-strong-password".toCharArray();ByteArrayOutputStream secure=new ByteArrayOutputStream();
   try(OutputStream enc=BackupCrypto.encrypt(secure,password)){enc.write(zip.toByteArray());}
   File decoded=BackupCrypto.decryptToPrivateFile(new ByteArrayInputStream(secure.toByteArray()),password,dest);
   try(RestoreArchive.Prepared parsed=RestoreArchive.prepare(new FileInputStream(decoded),dest)){ok(parsed.receipts==4,"encrypted attached receipts");}
   Files.deleteIfExists(decoded.toPath());
  }finally{remove(src);remove(dest);}
  System.out.println("PASS UX3 multiple receipt plain and encrypted archive: "+checks+" assertions");
 }
}
