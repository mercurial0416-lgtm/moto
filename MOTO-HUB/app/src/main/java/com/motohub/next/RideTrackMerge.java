package com.motohub.next;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;

/** Explicit local merge of two completed rides. Original tracks are never modified/deleted. */
final class RideTrackMerge {
 static final long MAX_GAP_MS=30L*60_000;
 private RideTrackMerge(){}
 static boolean mergedSource(File track){
  File marker=new File(track.getPath()+".mergedinto");
  if(!marker.isFile())return false;
  try{
   if(marker.length()>100)return true;
   File combined=new File(track.getParentFile(),new String(java.nio.file.Files.readAllBytes(marker.toPath()),StandardCharsets.US_ASCII));
   return !new File(combined.getPath()+".unmerged").isFile();
  }catch(Exception ignored){return true;}
 }
 static boolean archived(File track){return new File(track.getPath()+".unmerged").isFile();}
 /** Delete only independent tracks. Linked originals/targets are kept until a successful undo. */
 static boolean canDelete(File filesDir,File track){
  if(track==null||new File(track.getPath()+".mergedinto").exists())return false;
  try{
   for(File candidate:TrackFile.tracks(filesDir)){
    File link=new File(candidate.getPath()+".mergedinto");
    if(!link.isFile()||link.length()>100)continue;
    if(track.getName().equals(new String(java.nio.file.Files.readAllBytes(link.toPath()),StandardCharsets.US_ASCII)))return false;
   }
   return true;
  }catch(Exception ignored){return false;}
 }
 static boolean canUndo(File filesDir,File combined){
  try{
   if(combined==null||!combined.isFile()||archived(combined)||mergedSource(combined)||
      !new File(combined.getPath()+".done").isFile()||
      !combined.getCanonicalFile().getParentFile().equals(TrackFile.directory(filesDir).getCanonicalFile()))return false;
   int linked=0;
   for(File file:TrackFile.tracks(filesDir)){
    File marker=new File(file.getPath()+".mergedinto");
    if(!marker.isFile()||marker.length()>100)continue;
    if(combined.getName().equals(new String(java.nio.file.Files.readAllBytes(marker.toPath()),StandardCharsets.US_ASCII)))linked++;
   }
   return linked==2;
  }catch(Exception ignored){return false;}
 }

 /** Only complete, uniquely linked merges may be undone. Combined GPS is archived, never deleted. */
 static synchronized int undo(File filesDir,File combined)throws Exception {
  File dir=TrackFile.directory(filesDir).getCanonicalFile();
  if(combined==null||!combined.getCanonicalFile().getParentFile().equals(dir)||
     !combined.getName().matches("[0-9]{10,15}-[a-f0-9-]{36}\\.track")||
     !combined.isFile()||!new File(combined.getPath()+".done").isFile()||
     archived(combined)||mergedSource(combined))throw new IOException("취소 가능한 통합 라이딩이 아닙니다.");
  java.util.List<File> sources=new java.util.ArrayList<>();
  for(File candidate:TrackFile.tracks(filesDir)){
   File marker=new File(candidate.getPath()+".mergedinto");
   if(!marker.isFile())continue;
   if(marker.length()>100)throw new IOException("통합 연결 정보가 손상됐습니다.");
   String name=new String(java.nio.file.Files.readAllBytes(marker.toPath()),StandardCharsets.US_ASCII);
   if(name.equals(combined.getName())){
    if(!candidate.isFile()||!(new File(candidate.getPath()+".done").isFile()||new File(candidate.getPath()+".recovered").isFile()))
     throw new IOException("통합 원본이 없어 취소할 수 없습니다.");
    sources.add(marker);
   }
  }
  if(sources.size()!=2)throw new IOException("통합 원본 2건을 모두 찾을 수 없습니다.");
  File flag=new File(combined.getPath()+".unmerged");
  if(!flag.createNewFile())throw new IOException("이미 취소한 통합입니다.");
  try(java.io.FileOutputStream out=new java.io.FileOutputStream(flag)){out.getFD().sync();}
  // The archived combined file stays recoverable even if marker cleanup fails midway.
  for(File marker:sources)if(marker.isFile()&&!marker.delete())throw new IOException("원본 연결 표시 제거 실패. 다시 통합 취소를 확인해 주세요.");
  return sources.size();
 }
 static final class Result {
  final File combined;final int points;final long gapMs;
  Result(File combined,int points,long gapMs){this.combined=combined;this.points=points;this.gapMs=gapMs;}
 }
 private static final class Bounds {
  long first,last;int points;
 }
 static synchronized Result merge(File filesDir,File sourceA,File sourceB)throws Exception {
  File dir=TrackFile.directory(filesDir).getCanonicalFile();
  if(sourceA==null||sourceB==null||sourceA.getCanonicalFile().equals(sourceB.getCanonicalFile()))throw new IOException("서로 다른 라이딩 2건을 선택해 주세요.");
  for(File file:Arrays.asList(sourceA,sourceB)){
   if(!file.getCanonicalFile().getParentFile().equals(dir)||!file.getName().matches("[0-9]{10,15}-[a-f0-9-]{36}\\.track"))
    throw new IOException("로컬 라이딩 파일만 선택할 수 있습니다.");
   if(!file.isFile()||file.length()>TrackFile.MAX_BYTES/2||
       !(new File(file.getPath()+".done").isFile()||new File(file.getPath()+".recovered").isFile())||
       new File(file.getPath()+".imported").exists()||new File(file.getPath()+".mergedinto").isFile()||archived(file)||mergedSource(file))
    throw new IOException("완료·복구된 통합 전 원본만 선택할 수 있습니다.");
  }
  File first=sourceA,second=sourceB;
  Bounds boundsA=bounds(first),boundsB=bounds(second);
  if(boundsA.first>boundsB.first){File temp=first;first=second;second=temp;Bounds b=boundsA;boundsA=boundsB;boundsB=b;}
  String firstBike=RideBikeBinding.readStrict(first),secondBike=RideBikeBinding.readStrict(second);
  if(!firstBike.equals(secondBike))throw new IOException("서로 다른 바이크(또는 미지정) 라이딩은 통합할 수 없습니다. 각 기록의 바이크를 먼저 지정하세요.");
  long gap=boundsB.first-boundsA.last;
  if(gap<0||gap>MAX_GAP_MS)throw new IOException("두 경로가 겹치거나 휴식 간격이 30분을 넘습니다.");
  if(boundsA.points+boundsB.points<2)throw new IOException("GPS 좌표가 부족합니다.");
  String name=boundsA.first+"-"+UUID.randomUUID()+".track"; // preserve actual ride date for monthly reports
  File combined=new File(dir,name),tmp=new File(dir,"."+name+".tmp");
  File markA=new File(first.getPath()+".mergedinto"),markB=new File(second.getPath()+".mergedinto");
  try{
   try(FileOutputStream out=new FileOutputStream(tmp)){
    copyPoints(first,out);copyPoints(second,out);out.getFD().sync();
   }
   if(TrackFile.read(tmp,(a,b,c,d)->{})!=boundsA.points+boundsB.points)throw new IOException("통합 좌표 수 검증 실패");
   if(!tmp.renameTo(combined))throw new IOException("통합 파일 이동 실패");
   TrackCompletion.mark(combined,boundsA.points+boundsB.points);
   if(!firstBike.isEmpty())RideBikeBinding.write(combined,firstBike);
   // Marks suppress double counting in the monthly report. Keep both source files.
   writeMarker(markA,name);
   writeMarker(markB,name);
   return new Result(combined,boundsA.points+boundsB.points,gap);
  }catch(Exception failure){
   // Do not leave a partially registered merge or suppress the originals in statistics.
   markA.delete();markB.delete();
   new File(combined.getPath()+".bike").delete();
   new File(combined.getPath()+".done").delete();combined.delete();
   throw failure;
  }finally{tmp.delete();}
 }
 private static Bounds bounds(File file)throws Exception{
  Bounds b=new Bounds();b.points=TrackFile.read(file,(lat,lon,accuracy,time)->{
   if(b.first==0)b.first=time;b.last=time;
  });
  if(b.points==0)throw new IOException("빈 경로는 통합할 수 없습니다.");
  try(java.io.RandomAccessFile input=new java.io.RandomAccessFile(file,"r")){
   if(input.length()==0)throw new IOException("비어 있는 기록");
   input.seek(input.length()-1);if(input.read()!='\n')throw new IOException("미완성 GPS 기록은 통합할 수 없습니다.");
  }
  return b;
 }
 private static void copyPoints(File from,FileOutputStream to)throws Exception {
  TrackFile.read(from,(lat,lon,accuracy,time)->to.write(TrackFile.point(lat,lon,accuracy,time).getBytes(StandardCharsets.UTF_8)));
 }
 private static void writeMarker(File file,String target)throws IOException {
  if(!file.createNewFile())throw new IOException("이미 통합된 기록입니다.");
  try(FileOutputStream out=new FileOutputStream(file)){out.write(target.getBytes(StandardCharsets.US_ASCII));out.getFD().sync();}
 }
}
