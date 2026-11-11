package com.motohub.next;
import java.io.*;
import java.nio.file.*;
import java.util.UUID;
/** Explicit, bounded continuation after process death. Never appends to original bytes. */
final class RideResumePolicy {
 static final long MAX_GAP_MS=900_000L;
 private RideResumePolicy(){}
 static boolean validSession(String uuid){
  if(uuid==null||!uuid.matches("[a-f0-9-]{36}"))return false;
  try{return UUID.fromString(uuid).toString().equals(uuid);}catch(Exception e){return false;}
 }
 static boolean eligible(String name,long modified,long now){
  if(name==null||!name.matches("[0-9]{10,15}-[a-f0-9-]{36}\\.track"))return false;
  return validSession(RideActiveSessionPolicy.session(name))&&
   modified>0&&now>=modified&&now-modified<=MAX_GAP_MS;
 }
 static File source(File dir,String name,long now)throws Exception{
  if(name==null||name.contains("/")||name.contains("\\"))
   throw new IOException("잘못된 경로 이름");
  File parent=TrackFile.directory(dir),track=new File(parent,name);
  if(!Files.isRegularFile(track.toPath(),LinkOption.NOFOLLOW_LINKS)||
   !eligible(name,track.lastModified(),now)||track.length()==0||
   track.length()>TrackFile.MAX_BYTES||
   new File(track.getPath()+".done").exists()||
   new File(track.getPath()+".recovered").exists()||
   new File(track.getPath()+".imported").exists()||
   !track.getCanonicalFile().getParentFile().equals(parent.getCanonicalFile()))
   throw new IOException("최근 중단된 GPS 원본을 확인할 수 없습니다");
  return track;
 }
}
