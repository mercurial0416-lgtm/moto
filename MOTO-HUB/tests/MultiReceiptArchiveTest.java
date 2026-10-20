package com.motohub.next;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

public class MultiReceiptArchiveTest {
 private static final String ID="acd936a1-f7de-4001-9402-aabbccddeeff";
 static int checks;
 static void ok(boolean v,String why){checks++;if(!v)throw new AssertionError(why);}
 static byte[] jpeg(int number){return new byte[]{(byte)0xff,(byte)0xd8,10,(byte)number,(byte)0xff,(byte)0xd9};}
 public static void main(String[] args)throws Exception{
  File origin=Files.createTempDirectory("moto-ux3-backup").toFile();
  File restored=Files.createTempDirectory("moto-ux3-restore").toFile();
  try{
   for(int i=1;i<=5;i++)Files.write(ReceiptFiles.file(origin,ID,i).toPath(),jpeg(i));
   ByteArrayOutputStream out=new ByteArrayOutputStream();
   BackupBundle.export("{\"version\":3}",origin,out);
   byte[] zip=out.toByteArray();
   BackupIntegrity.verify(new ByteArrayInputStream(zip));checks++;
   Set<String> names=new HashSet<>();
   try(ZipInputStream read=new ZipInputStream(new ByteArrayInputStream(zip))){ZipEntry entry;while((entry=read.getNextEntry())!=null)names.add(entry.getName());}
   for(int i=1;i<=5;i++)ok(names.contains("receipts/"+ReceiptFiles.name(ID,i)),"slot preserved "+i);
   try(RestoreArchive.Prepared prepared=RestoreArchive.prepare(new ByteArrayInputStream(zip),restored)){
    ok(prepared.receipts==5,"prepared 5 receipt slots");
    String[] saved={""};prepared.install(restored,json->saved[0]=json);
    ok(saved[0].equals("{\"version\":3}"),"JSON write after images");
   }
   for(int i=1;i<=5;i++)ok(Arrays.equals(Files.readAllBytes(ReceiptFiles.file(restored,ID,i).toPath()),jpeg(i)),"original JPEG bytes slot "+i);
   ok(ReceiptFiles.existing(restored,ID).size()==5,"restored attachment count");
   System.out.println("PASS multi receipt ZIP + readback + restore: "+checks+" assertions");
  }finally{for(File dir:new File[]{origin,restored}){
   try(java.util.stream.Stream<Path> paths=Files.walk(dir.toPath())){paths.sorted(Comparator.reverseOrder()).forEach(x->{try{Files.deleteIfExists(x);}catch(Exception ignored){}});}
  }}
 }
}
