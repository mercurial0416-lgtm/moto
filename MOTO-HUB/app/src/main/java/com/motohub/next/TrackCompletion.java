package com.motohub.next;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/** Only completed, fully readable GPS files receive the .done marker. */
final class TrackCompletion {
 private TrackCompletion(){}
 static void mark(File track,int expectedPoints)throws Exception{
  if(track==null||!track.isFile()||expectedPoints<1)
   throw new IOException("완료할 GPS 파일이 없습니다.");
  int actual=TrackFile.read(track,(lat,lon,accuracy,time)->{});
  if(actual!=expectedPoints)
   throw new IOException("GPS 좌표 개수가 저장 전후 일치하지 않습니다.");
  // A trailing partial line is NOT counted by TrackFile.read; never claim
  // such a file is a cleanly completed ride.
  try(java.io.RandomAccessFile input=new java.io.RandomAccessFile(track,"r")){
   if(input.length()==0)throw new IOException("빈 GPS 기록입니다.");
   input.seek(input.length()-1);
   if(input.read()!='\n')throw new IOException("GPS 마지막 좌표가 완전히 저장되지 않았습니다.");
  }
  File marker=new File(track.getPath()+".done");
  if(!marker.createNewFile())throw new IOException("이미 완료 표시가 존재합니다.");
  // A failed marker sync must not leave a false positive .done behind.
  try(FileOutputStream out=new FileOutputStream(marker,true)){out.getFD().sync();}
  catch(IOException e){marker.delete();throw e;}
 }
}
