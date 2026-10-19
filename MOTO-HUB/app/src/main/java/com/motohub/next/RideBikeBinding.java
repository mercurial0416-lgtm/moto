package com.motohub.next;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/** An immutable GPS track is associated with a local garage entry by optional UUID sidecar.
 *  No locations, device identifiers or remote data are transmitted by this class. */
final class RideBikeBinding {
 private RideBikeBinding(){}
 static boolean valid(String id){
  if(id==null||!id.matches("[0-9a-fA-F]{8}(-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}"))return false;
  try{return UUID.fromString(id).toString().equalsIgnoreCase(id);}catch(IllegalArgumentException ignored){return false;}
 }
 static String read(File track){
  if(track==null)return "";
  File marker=new File(track.getPath()+".bike");
  if(!marker.isFile()||marker.length()>64)return "";
  try{
   String id=new String(Files.readAllBytes(marker.toPath()),StandardCharsets.US_ASCII);
   return valid(id)?id:"";
  }catch(IOException ignored){return "";}
 }
 /** Unlike the display helper, mutations must fail closed on corrupted association metadata. */
 static String readStrict(File track)throws IOException{
  if(track==null)throw new IOException("라이딩 파일을 찾지 못했어요.");
  File marker=new File(track.getPath()+".bike");
  if(!marker.exists())return ""; // Older tracks have no binding; remain unassigned.
  if(!marker.isFile()||marker.length()>64)throw new IOException("바이크 연결 정보가 손상됐어요. 백업을 확인해 주세요.");
  String id=new String(Files.readAllBytes(marker.toPath()),StandardCharsets.US_ASCII);
  if(!valid(id))throw new IOException("잘못된 바이크 연결 정보입니다. 주행거리 반영을 중단했어요.");
  return id;
 }
 static void write(File track,String id)throws IOException{
  if(!valid(id))throw new IOException("올바른 바이크 식별자가 아닙니다.");
  if(track==null||!track.isFile()||!track.getName().matches("[0-9]{10,15}-[a-f0-9-]{36}\\.track")||
    !"rides".equals(track.getCanonicalFile().getParentFile().getName()))
   throw new IOException("저장된 라이딩 기록만 연결할 수 있어요.");
  File marker=new File(track.getPath()+".bike");
  File temp=new File(marker.getPath()+".writing");
  try{
   try(FileOutputStream output=new FileOutputStream(temp)){
    output.write(id.getBytes(StandardCharsets.US_ASCII));output.getFD().sync();
   }
   try{Files.move(temp.toPath(),marker.toPath(),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
   catch(java.nio.file.AtomicMoveNotSupportedException ignored){Files.move(temp.toPath(),marker.toPath(),StandardCopyOption.REPLACE_EXISTING);}
  }finally{temp.delete();}
 }
}
