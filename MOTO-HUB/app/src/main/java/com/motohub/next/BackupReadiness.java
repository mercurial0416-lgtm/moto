package com.motohub.next;
import java.io.*;
import java.util.*;

/** Lightweight offline preflight before writing a user-owned ZIP. Never opens GPS coordinates. */
final class BackupReadiness {
 private BackupReadiness(){}
 static final class Summary {
  final int rides,photos,emptyRides,unfinishedRides,oversizedFiles;
  final long rawBytes;
  Summary(int rides,int photos,int empty,int unfinished,int oversized,long size){
   this.rides=rides;this.photos=photos;emptyRides=empty;unfinishedRides=unfinished;oversizedFiles=oversized;rawBytes=size;
  }
  boolean withinBasicLimits(){return rides<=BackupFormat.MAX_TRACKS&&photos<=BackupFormat.MAX_PHOTOS&&oversizedFiles==0&&rawBytes<=BackupFormat.MAX_TOTAL_BYTES;}
  String message(){
   String status=withinBasicLimits()?"기본 용량 점검 통과":"백업 전에 용량·파일 수를 확인하세요";
   return status+"\nGPS 원본 "+rides+"건 · 바이크 사진 "+photos+"장"
    +"\n원본 크기 합계 약 "+(rawBytes/1_000_000)+"MB (ZIP의 GPX 추가 용량 제외)"
    +"\n완전히 빈 GPS 기록 "+emptyRides+"개 · 미완료 기록 "+unfinishedRides+"개"
    +"\n개별 25MB 초과 "+oversizedFiles+"개"
    +"\n이 점검은 좌표를 읽거나 백업 성공을 보장하지 않습니다.\n안전한 백업은 내보낸 ZIP 재검증 후 확정됩니다.";
  }
 }
 static Summary inspect(File internal)throws IOException {
  if(internal==null||!internal.isDirectory())throw new IOException("앱 저장 폴더를 찾을 수 없어요.");
  File[] rides=new File(internal,"rides").listFiles((d,name)->name.matches(BackupFormat.TRACK));
  File[] photos=internal.listFiles((d,name)->name.matches(BackupFormat.PHOTO));
  int count=0,photoCount=0,empty=0,unfinished=0,oversized=0;long bytes=0;
  if(rides!=null)for(File f:rides)if(f.isFile()){
   count++;long length=f.length();bytes+=length;
   if(length==0)empty++;
   if(length>BackupFormat.MAX_TRACK_BYTES)oversized++;
   if(!new File(f.getPath()+".done").isFile()&&!new File(f.getPath()+".imported").isFile()&&!new File(f.getPath()+".recovered").isFile())unfinished++;
   for(String suffix:new String[]{".mergedinto",".bike",".category"}){
    File metadata=new File(f.getPath()+suffix);if(metadata.isFile())bytes+=metadata.length();
   }
  }
  if(photos!=null)for(File f:photos)if(f.isFile()){
   photoCount++;long length=f.length();bytes+=length;
   if(length>BackupFormat.MAX_TRACK_BYTES)oversized++;
  }
  return new Summary(count,photoCount,empty,unfinished,oversized,bytes);
 }
}
