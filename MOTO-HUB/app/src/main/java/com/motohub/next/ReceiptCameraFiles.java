package com.motohub.next;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;

/** Private, disposable camera captures. Never removes user receipts, GPS, backup or rollback files. */
final class ReceiptCameraFiles {
 private ReceiptCameraFiles(){}
 static final String DIR="receipt-camera";
 private static final String FILE_PATTERN="moto-camera-[A-Za-z0-9_-]+\\.jpg";
 static File create(File cache)throws IOException{
  if(cache==null||!cache.isDirectory())throw new IOException("임시 촬영 폴더를 사용할 수 없습니다.");
  File folder=new File(cache,DIR);
  if(!folder.isDirectory()&&!folder.mkdirs())throw new IOException("촬영 폴더를 만들 수 없습니다.");
  if(Files.isSymbolicLink(folder.toPath()))throw new IOException("안전하지 않은 촬영 폴더입니다.");
  return File.createTempFile("moto-camera-",".jpg",folder);
 }
 static File recover(File cache,String filename){
  if(cache==null||filename==null||!filename.matches(FILE_PATTERN))return null;
  File folder=new File(cache,DIR);
  if(Files.isSymbolicLink(folder.toPath()))return null;
  return new File(folder,filename);
 }
 static boolean valid(File cache,File file){
  if(file==null)return false;
  File expected=recover(cache,file.getName());
  return expected!=null&&expected.equals(file)&&!Files.isSymbolicLink(file.toPath())&&Files.isRegularFile(file.toPath(),LinkOption.NOFOLLOW_LINKS);
 }
 static boolean delete(File cache,String name){
  File f=recover(cache,name);
  if(f==null||Files.isSymbolicLink(f.toPath()))return false;
  try{return Files.deleteIfExists(f.toPath());}catch(IOException e){return false;}
 }
 static int cleanOld(File cache,long now){
  if(cache==null)return 0;
  File folder=new File(cache,DIR);
  if(!folder.isDirectory()||Files.isSymbolicLink(folder.toPath()))return 0;
  File[] files=folder.listFiles();if(files==null)return 0;
  int cleared=0;
  for(File f:files){
   if(!valid(cache,f))continue;
   long age=now-f.lastModified();
   if(age<24L*60*60*1000||age<0)continue;
   if(delete(cache,f.getName()))cleared++;
  }
  return cleared;
 }
}
