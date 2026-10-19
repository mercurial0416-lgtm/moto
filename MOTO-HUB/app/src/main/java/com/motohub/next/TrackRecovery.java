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
 static Result recover(File track)throws Exception {return recover(track,()->{});}
 /** Package-private fault hook exercises rollback after replacement but before marking completion. */
 static Result recover(File track,Runnable beforeMark)throws Exception {
  if(track==null||!track.isFile()||!track.getName().matches("[0-9]{10,15}-[a-f0-9-]{36}\\.track"))
   throw new IOException("올바른 GPS 기록 파일을 선택해 주세요.");
  if(track.length()>TrackFile.MAX_BYTES)throw new IOException("경로 파일 크기 초과");
  File marker=new File(track.getPath()+".recovered");
  if(new File(track.getPath()+".done").exists()||new File(track.getPath()+".imported").exists()||marker.exists())
   throw new IOException("이미 처리된 기록입니다.");
  // All committed rows must parse before touching any source bytes.
  int points=TrackFile.read(track,(a,b,c,d)->{});
  if(points<1)throw new IOException("복구 가능한 GPS 좌표가 없습니다.");
  long length=track.length(),complete=length;
  try(RandomAccessFile input=new RandomAccessFile(track,"r")){
   while(complete>0){input.seek(complete-1);if(input.read()=='\n')break;complete--;}
  }
  if(complete==0)throw new IOException("복구 가능한 완성된 좌표가 없습니다.");
  long dropped=length-complete;
  Path temporary=null,originalCopy=null;
  boolean replaced=false,committed=false;
  try{
   if(dropped>0){
    Path parent=track.getParentFile().toPath();
    temporary=Files.createTempFile(parent,".ride-recovery-",".tmp");
    try(InputStream in=new BufferedInputStream(new FileInputStream(track));
        FileOutputStream out=new FileOutputStream(temporary.toFile())){
     byte[] buffer=new byte[8192];long remaining=complete;
     while(remaining>0){int n=in.read(buffer,0,(int)Math.min(buffer.length,remaining));
      if(n<0)throw new EOFException("GPS 원본을 읽는 중 파일이 변경됐습니다.");
      out.write(buffer,0,n);remaining-=n;
     }
     out.getFD().sync();
    }
    if(TrackFile.read(temporary.toFile(),(a,b,c,d)->{})!=points)
     throw new IOException("복구 전후 GPS 좌표가 일치하지 않습니다.");
    // Keep the exact pre-recovery bytes until BOTH replacement and completion
    // marker have been durably committed. A failed marker must not destroy data.
    originalCopy=Files.createTempFile(parent,".ride-original-",".bak");
    Files.copy(track.toPath(),originalCopy,StandardCopyOption.REPLACE_EXISTING);
    try(FileOutputStream durable=new FileOutputStream(originalCopy.toFile(),true)){
     durable.getFD().sync();
    }
    if(Files.size(originalCopy)!=length)throw new IOException("GPS 원본 임시 보존 실패");
    Files.move(temporary,track.toPath(),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
    replaced=true;temporary=null;
   }
   beforeMark.run();
   if(!marker.createNewFile())throw new IOException("복구 완료 표시를 저장하지 못했습니다.");
   try(FileOutputStream markerOut=new FileOutputStream(marker,true)){
    markerOut.getFD().sync();
   }
   committed=true;
   return new Result(points,dropped);
  }catch(Exception failure){
   if(!committed){
    if(marker.isFile())marker.delete();
    if(replaced&&originalCopy!=null){
     try{
      Files.move(originalCopy,track.toPath(),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
      originalCopy=null;
     }catch(Exception rollback){
      // Preserve the byte-exact hidden backup if the filesystem cannot restore it.
      failure.addSuppressed(rollback);
      throw new IOException("GPS 복구 중단 · 원본 보존 파일 확인 필요: "+originalCopy,failure);
     }
    }
   }
   throw failure;
  }finally{
   if(temporary!=null)Files.deleteIfExists(temporary);
   if(committed||!replaced||originalCopy==null){
    if(originalCopy!=null)Files.deleteIfExists(originalCopy);
   }
  }
 }
}
