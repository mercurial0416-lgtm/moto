package com.motohub.next;

import java.io.*;
import java.nio.file.*;

/** Explicit, offline salvage of an interrupted GPS track. Never invents coordinates. */
final class TrackRecovery {
 private TrackRecovery() {}
 static final class Result {
  final int points;
  final long discardedBytes;
  Result(int points,long discardedBytes){this.points=points;this.discardedBytes=discardedBytes;}
 }
 static Result recover(File track)throws Exception {
  if(track==null||!track.isFile()||!track.getName().matches("[0-9]{10,15}-[a-f0-9-]{36}\\.track"))
   throw new IOException("올바른 GPS 기록 파일을 선택해 주세요.");
  if(track.length()>TrackFile.MAX_BYTES)throw new IOException("경로 파일 크기 초과");
  if(new File(track.getPath()+".done").exists()||new File(track.getPath()+".imported").exists()||new File(track.getPath()+".recovered").exists())
   throw new IOException("이미 처리된 기록입니다.");
  // All newline-committed rows must validate before we modify any bytes.
  int points=TrackFile.read(track,(a,b,c,d)->{});
  if(points<1)throw new IOException("복구 가능한 GPS 좌표가 없습니다.");
  long length=track.length(),complete=length;
  try(RandomAccessFile input=new RandomAccessFile(track,"r")){
   while(complete>0){input.seek(complete-1);if(input.read()=='\n')break;complete--;}
  }
  if(complete==0)throw new IOException("복구 가능한 완성된 좌표가 없습니다.");
  long dropped=length-complete;
  if(dropped>0){
   Path temp=Files.createTempFile(track.getParentFile().toPath(),".ride-recovery-",".tmp");
   try{
    try(InputStream in=new BufferedInputStream(new FileInputStream(track.toPath().toFile()));
        FileOutputStream out=new FileOutputStream(temp.toFile())){
     byte[] buffer=new byte[8192];long remaining=complete;
     while(remaining>0){int n=in.read(buffer,0,(int)Math.min(buffer.length,remaining));if(n<0)throw new EOFException("GPS 원본을 읽는 중 파일이 변경됐습니다.");out.write(buffer,0,n);remaining-=n;}
     out.getFD().sync();
    }
    if(TrackFile.read(temp.toFile(),(a,b,c,d)->{})!=points)throw new IOException("복구 전후 GPS 좌표가 일치하지 않습니다.");
    // Same-directory atomic replacement: never truncate the only original in place.
    Files.move(temp,track.toPath(),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
   }finally{Files.deleteIfExists(temp);}
  }
  if(!new File(track.getPath()+".recovered").createNewFile())throw new IOException("복구 완료 표시를 저장하지 못했습니다.");
  return new Result(points,dropped);
 }
}
