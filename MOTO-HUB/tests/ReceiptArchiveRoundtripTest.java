package com.motohub.next;
import java.io.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;
public final class ReceiptArchiveRoundtripTest {
 static int checks;
 static void ok(boolean v,String message){checks++;if(!v)throw new AssertionError(message);}
 static final String ID="12345678-1234-1234-1234-123456789abc";
 static final String FILE="record-receipt-"+ID+".jpg";
 static byte[] jpg(){return new byte[]{(byte)0xff,(byte)0xd8,5,12,34,5,(byte)0xff,(byte)0xd9};}
 static byte[] makeZip(String filename,byte[] bytes)throws Exception{
  ByteArrayOutputStream b=new ByteArrayOutputStream();try(ZipOutputStream z=new ZipOutputStream(b)){
   z.putNextEntry(new ZipEntry("MOTO-HUB-data.json"));z.write("{}".getBytes(StandardCharsets.UTF_8));z.closeEntry();
   z.putNextEntry(new ZipEntry(filename));z.write(bytes);z.closeEntry();
  }return b.toByteArray();
 }
 static void reject(byte[] bytes)throws Exception{
  try{BackupIntegrity.verify(new ByteArrayInputStream(bytes));throw new AssertionError("invalid archive accepted");}catch(IOException expected){checks++;}
 }
 static void clean(File f)throws Exception {if(f.isDirectory()){File[] list=f.listFiles();if(list!=null)for(File x:list)clean(x);}Files.deleteIfExists(f.toPath());}
 public static void main(String[] args)throws Exception{
  ok(BackupFormat.isReceipt("receipts/"+FILE),"receipt path accepted");
  ok(!BackupFormat.allowed("receipts/../x.jpg",false),"path traversal rejected");
  ok(!BackupFormat.isReceipt("receipts/bad.jpg"),"unexpected file rejected");
  reject(makeZip("receipts/bad.jpg",jpg()));
  reject(makeZip("receipts/"+FILE,new byte[]{4,1,2,3}));
  reject(makeZip("receipts/"+FILE,new byte[]{(byte)0xff,(byte)0xd8,3,4}));
  File from=Files.createTempDirectory("moto-receipt-backup").toFile();
  File to=Files.createTempDirectory("moto-receipt-restore").toFile();
  try{
   Files.write(new File(from,FILE).toPath(),jpg());
   ByteArrayOutputStream buffer=new ByteArrayOutputStream();
   BackupBundle.export("{}",from,buffer);
   ok(BackupIntegrity.verify(new ByteArrayInputStream(buffer.toByteArray())).tracks==0,"backup validates");
   ok(BackupReadiness.inspect(from).receipts==1,"preflight counts receipts separately");
   try(RestoreArchive.Prepared prepared=RestoreArchive.prepare(new ByteArrayInputStream(buffer.toByteArray()),to)){
    prepared.install(to,json->{});
   }
   ok(Arrays.equals(jpg(),Files.readAllBytes(new File(to,FILE).toPath())),"restored exact JPEG bytes");
   ok(BackupFormat.allowed("receipts/"+FILE,false),"whitelisted new receipt");
   byte[] encrypted;
   char[] pass="MOTO-HUB-Test-Receipt-Strong-123".toCharArray();
   try(ByteArrayOutputStream outer=new ByteArrayOutputStream()){
    try(OutputStream secure=BackupCrypto.encrypt(outer,pass)){secure.write(buffer.toByteArray());}
    encrypted=outer.toByteArray();
   }
   File decrypted=BackupCrypto.decryptToPrivateFile(new ByteArrayInputStream(encrypted),pass,to);
   try(InputStream input=new FileInputStream(decrypted)){
    ok(BackupIntegrity.verify(input).tracks==0,"authenticated encrypted receipt ZIP verified");
   }
   try(RestoreArchive.Prepared prepared=RestoreArchive.prepare(new FileInputStream(decrypted),to)){
    ok(prepared.receipts==1,"encrypted restore recognizes receipt");
   }
   Files.deleteIfExists(decrypted.toPath());

  }finally{clean(from);clean(to);}
  System.out.println("PASS receipt archive restore and validator: "+checks+" assertions");
 }
}
