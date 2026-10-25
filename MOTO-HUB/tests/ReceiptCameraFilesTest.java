package com.motohub.next;

import java.io.*;
import java.nio.file.*;

public final class ReceiptCameraFilesTest {
 static int count=0;
 static void ok(boolean condition,String message){count++;if(!condition)throw new AssertionError(message);}
 public static void main(String[] args)throws Exception{
  File parent=Files.createTempDirectory("moto-camera-policy-").toFile();
  File cache=new File(parent,"cache");cache.mkdir();
  File files=new File(parent,"files");files.mkdir();
  File own=ReceiptCameraFiles.create(cache);
  ok(own.getName().startsWith("moto-camera-"),"unique capture filename");
  ok(own.getParentFile().getName().equals("receipt-camera"),"camera subtree only");
  ok(ReceiptCameraFiles.valid(cache,own),"valid private capture");
  ok(ReceiptCameraFiles.recover(cache,own.getName()).equals(own),"restore capture after activity recreation");
  ok(ReceiptCameraFiles.recover(cache,"../../db.json")==null,"path traversal rejected");
  ok(ReceiptCameraFiles.recover(cache,"moto-camera-../../x.jpg")==null,"nested traversal rejected");
  ok(ReceiptCameraFiles.recover(cache,"record-receipt-abc.jpg")==null,"receipt names rejected");
  ok(ReceiptCameraFiles.recover(cache,"moto-camera-abc.zip")==null,"other suffix rejected");
  ok(ReceiptCameraFiles.recover(cache,"")==null,"empty name rejected");
  File snapshot=new File(files,"motohub-v3.json");Files.write(snapshot.toPath(),new byte[]{11,22,33});
  ok(ReceiptCameraFiles.cleanOld(cache,System.currentTimeMillis())==0,"fresh capture kept");
  ok(own.exists(),"fresh capture not purged");
  File stale=ReceiptCameraFiles.create(cache);
  long now=System.currentTimeMillis();
  ok(stale.setLastModified(now-25L*60*60*1000),"set test stale age");
  ok(ReceiptCameraFiles.cleanOld(cache,now)==1,"stale file cleared");
  ok(!stale.exists(),"stale capture deleted");
  ok(own.exists(),"active capture remains");
  ok(snapshot.exists(),"database never touched");
  File backup=new File(cache,"moto-full-backup.zip");Files.write(backup.toPath(),new byte[]{1,2,3});
  ok(ReceiptCameraFiles.cleanOld(cache,now+1)==0,"unrelated cache untouched");
  ok(backup.exists(),"backup not purged");
  try{
   File outside=new File(files,"record-receipt-unchanged.jpg");Files.write(outside.toPath(),new byte[]{1});
   Path malicious=new File(own.getParentFile(),"moto-camera-trick.jpg").toPath();
   Files.createSymbolicLink(malicious,outside.toPath());
   ok(!ReceiptCameraFiles.valid(cache,malicious.toFile()),"symlink capture rejected");
   ok(!ReceiptCameraFiles.delete(cache,malicious.getFileName().toString()),"symlink not deleted");
   ok(outside.exists(),"external target untouched");
   Files.delete(malicious);
  }catch(UnsupportedOperationException|SecurityException e){ /* Symlink support varies. */ }
  ok(ReceiptCameraFiles.delete(cache,own.getName()),"capture delete after OCR");
  ok(!own.exists(),"OCR capture really removed");
  ok(!ReceiptCameraFiles.delete(cache,"../../../files/motohub-v3.json"),"unsafe deletion rejected");
  ok(snapshot.exists(),"user data remains");
  System.out.println("PASS receipt private-camera lifecycle: "+count+" checks");
 }
}
