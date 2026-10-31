package com.motohub.next;
import java.io.*;
import java.util.*;
import java.nio.file.Files;
import java.nio.file.LinkOption;

/** Lightweight offline preflight before writing a user-owned ZIP. Never opens GPS coordinates. */
final class BackupReadiness {
 private BackupReadiness(){}
 static final class Summary {
  final int rides,photos,receipts,emptyRides,unfinishedRides,oversizedFiles,completedEmptyRides,trashRides,selectedModels;
  final long rawBytes;
  Summary(int rides,int photos,int empty,int unfinished,int oversized,long size){
   this(rides,photos,0,empty,unfinished,oversized,0,size);
  }
  Summary(int rides,int photos,int empty,int unfinished,int oversized,int completedEmpty,long size){
   this(rides,photos,0,empty,unfinished,oversized,completedEmpty,size);
  }
  Summary(int rides,int photos,int receipts,int empty,int unfinished,int oversized,int completedEmpty,long size){
   this(rides,photos,receipts,empty,unfinished,oversized,completedEmpty,0,0,size);
  }
  Summary(int rides,int photos,int receipts,int empty,int unfinished,int oversized,int completedEmpty,
          int trashRides,int selectedModels,long size){
   this.rides=rides;this.photos=photos;this.receipts=receipts;emptyRides=empty;unfinishedRides=unfinished;
   oversizedFiles=oversized;completedEmptyRides=completedEmpty;this.trashRides=trashRides;this.selectedModels=selectedModels;rawBytes=size;
  }
  boolean withinBasicLimits(){return rides>=0&&photos>=0&&receipts>=0&&rawBytes>=0
   &&(long)rides+trashRides<=BackupFormat.MAX_TRACKS&&photos<=BackupFormat.MAX_PHOTOS&&receipts<=BackupFormat.MAX_RECEIPTS
   &&selectedModels<=BackupFormat.MAX_PHOTOS
   &&oversizedFiles==0&&completedEmptyRides==0&&rawBytes<=BackupFormat.MAX_TOTAL_BYTES;}
  String message(){
   String status=withinBasicLimits()?"기본 용량 점검 통과":"백업 전에 용량·파일 수를 확인하세요";
   return status+"\nGPS 원본 "+rides+"건 · GPS 복구함 "+trashRides+"건 · 바이크 사진 "+photos+"장 · 영수증 "+receipts+"장 · 선택한 모델 사진 "+selectedModels+"장"
    +"\n원본 크기 합계 약 "+(rawBytes/1_000_000)+"MB (ZIP의 GPX 추가 용량 제외)"
    +"\n완전히 빈 GPS 기록 "+emptyRides+"개 · 미완료 기록 "+unfinishedRides+"개"
    +"\n완료 표시가 있지만 내용 없는 GPS 기록 "+completedEmptyRides+"개"
    +"\n용량 초과 또는 안전하지 않은 파일 "+oversizedFiles+"개"
    +"\n이 점검은 좌표를 읽거나 백업 성공을 보장하지 않습니다.\n안전한 백업은 내보낸 ZIP 재검증 후 확정됩니다.";
  }
 }
 private static boolean safeRegular(File file){
  return file!=null&&!Files.isSymbolicLink(file.toPath())
   &&Files.isRegularFile(file.toPath(),LinkOption.NOFOLLOW_LINKS);
 }
 /** Missing folders are empty; a present but unreadable folder must abort preflight. */
 private static File[] listIfPresent(File folder,FilenameFilter filter)throws IOException{
  if(!Files.exists(folder.toPath(),LinkOption.NOFOLLOW_LINKS))return new File[0];
  if(Files.isSymbolicLink(folder.toPath())||
     !Files.isDirectory(folder.toPath(),LinkOption.NOFOLLOW_LINKS))
   throw new IOException("백업 폴더가 안전하지 않습니다: "+folder.getName());
  File[] found=folder.listFiles(filter);
  if(found==null)throw new IOException("백업 폴더 목록을 읽지 못했습니다: "+folder.getName());
  return found;
 }
 private static long addBytes(long current,long amount){
  if(amount<0||current>BackupFormat.MAX_TOTAL_BYTES||amount>BackupFormat.MAX_TOTAL_BYTES-current)
   return BackupFormat.MAX_TOTAL_BYTES+1;
  return current+amount;
 }
 static Summary inspect(File internal)throws IOException {
  if(internal==null||!Files.isDirectory(internal.toPath(),LinkOption.NOFOLLOW_LINKS))
   throw new IOException("앱 저장 폴더를 찾을 수 없어요.");
  File ridesFolder=new File(internal,"rides");
  if(Files.isSymbolicLink(ridesFolder.toPath())
   ||(ridesFolder.exists()&&!Files.isDirectory(ridesFolder.toPath(),LinkOption.NOFOLLOW_LINKS)))
   throw new IOException("GPS 저장 폴더가 안전하지 않습니다.");
  File[] rides=listIfPresent(ridesFolder,(d,name)->name.matches(BackupFormat.TRACK));
  File[] photos=listIfPresent(internal,(d,name)->name.matches(BackupFormat.PHOTO));
  File[] receipts=listIfPresent(internal,(d,name)->name.matches(BackupFormat.RECEIPT));
  int count=0,photoCount=0,receiptCount=0,empty=0,unfinished=0,oversized=0,completedEmpty=0;long bytes=0;
  if(rides!=null)for(File f:rides){
   count++;
   if(!safeRegular(f)){oversized++;continue;}
   long length=f.length();bytes=addBytes(bytes,length);
   if(length==0)empty++;
   if(length>BackupFormat.MAX_TRACK_BYTES)oversized++;
   boolean done=false;
   // Mirror every sidecar emitted by BackupBundle; do not silently skip state flags.
   for(String suffix:new String[]{".done",".imported",".recovered",".unmerged",
            ".mergedinto",".bike",".category",".favorite"}){
    File marker=new File(f.getPath()+suffix);
    if(!Files.exists(marker.toPath(),LinkOption.NOFOLLOW_LINKS))continue;
    if(!safeRegular(marker)){oversized++;continue;}
    long markerBytes=marker.length();
    bytes=addBytes(bytes,markerBytes);
    if(markerBytes>BackupFormat.entryLimit("rides/"+f.getName()+suffix))oversized++;
    if(markerBytes==0&&(".done".equals(suffix)||".imported".equals(suffix)||
                         ".recovered".equals(suffix)))done=true;
   }
   if(!done)unfinished++;
   else if(length==0)completedEmpty++;
  }
  if(photos!=null)for(File f:photos){
   photoCount++;
   if(!safeRegular(f)){oversized++;continue;}
   long length=f.length();bytes=addBytes(bytes,length);
   if(length>BackupFormat.MAX_TRACK_BYTES)oversized++;
  }
  if(receipts!=null)for(File f:receipts){
   receiptCount++;
   if(!safeRegular(f)){oversized++;continue;}
   long length=f.length();bytes=addBytes(bytes,length);
   if(length>BackupFormat.MAX_TRACK_BYTES)oversized++;
  }
  // The full ZIP also includes selected model photos and their provenance JSON.
  File modelFolder=new File(internal,"moto-year-photos");
  File[] models=listIfPresent(modelFolder,(d,n)->n.matches(BackupFormat.MODEL_PHOTO));
  int selected=0;
  for(File image:models){
   if(!safeRegular(image)){oversized++;continue;}
   File metadata=new File(modelFolder,image.getName()+".json");
   if(!Files.exists(metadata.toPath(),LinkOption.NOFOLLOW_LINKS))continue;
   if(!safeRegular(metadata)){oversized++;continue;}
   if(metadata.length()>8192){oversized++;continue;}
   byte[] label=Files.readAllBytes(metadata.toPath());
   if(!BackupFormat.selectedModelMetadata(new String(label,java.nio.charset.StandardCharsets.UTF_8)))continue;
   selected++;
   if(image.length()>BackupFormat.MAX_TRACK_BYTES)oversized++;
   bytes=addBytes(bytes,image.length());
   bytes=addBytes(bytes,label.length);
  }
  // Quarantined GPS files have exactly the same backup safety and size constraints.
  File trashFolder=new File(internal,"rides-trash");
  File[] trashedFolders=listIfPresent(trashFolder,(d,n)->true);
  int trashCount=0;
  for(File folder:trashedFolders){
   if(!folder.getName().matches(BackupFormat.TRACK)
      ||Files.isSymbolicLink(folder.toPath())
      ||!Files.isDirectory(folder.toPath(),LinkOption.NOFOLLOW_LINKS))
    throw new IOException("GPS 복구함 폴더가 안전하지 않습니다.");
   File track=new File(folder,folder.getName());
   trashCount++;
   if(!safeRegular(track)){oversized++;continue;}
   if(track.length()==0||track.length()>BackupFormat.MAX_TRACK_BYTES)oversized++;
   bytes=addBytes(bytes,track.length());
   String prefix="rides-trash/"+track.getName()+"/"+track.getName();
   for(String suffix:new String[]{".done",".imported",".recovered",".unmerged",
              ".mergedinto",".bike",".category",".favorite"}){
    File marker=new File(track.getPath()+suffix);
    if(!Files.exists(marker.toPath(),LinkOption.NOFOLLOW_LINKS))continue;
    if(!safeRegular(marker)){oversized++;continue;}
    long size=marker.length();
    if(size>BackupFormat.entryLimit(prefix+suffix))oversized++;
    bytes=addBytes(bytes,size);
   }
  }
  return new Summary(count,photoCount,receiptCount,empty,unfinished,oversized,completedEmpty,
   trashCount,selected,bytes);
 }
}
