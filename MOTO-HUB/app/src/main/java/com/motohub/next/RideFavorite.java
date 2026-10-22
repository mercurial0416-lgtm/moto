package com.motohub.next;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;

/** Private, backup-compatible track favorite marker. No GPS reading or network required. */
final class RideFavorite {
 private RideFavorite(){}
 private static File marker(File track)throws IOException{
  if(track==null||!track.getName().matches(BackupFormat.TRACK)||
     !Files.isRegularFile(track.toPath(),LinkOption.NOFOLLOW_LINKS)||
     Files.isSymbolicLink(track.toPath())||!track.getParentFile().getName().equals("rides"))
   throw new IOException("즐겨찾기할 라이딩 원본을 찾지 못했어요.");
  return new File(track.getPath()+".favorite");
 }
 static boolean isFavorite(File track){
  try{
   File file=marker(track);
   return Files.isRegularFile(file.toPath(),LinkOption.NOFOLLOW_LINKS)&&
      !Files.isSymbolicLink(file.toPath())&&file.length()==0;
  }catch(Exception ignored){return false;}
 }
 static boolean toggle(File track)throws IOException{
  File file=marker(track);
  if(Files.exists(file.toPath(),LinkOption.NOFOLLOW_LINKS)){
   if(!Files.isRegularFile(file.toPath(),LinkOption.NOFOLLOW_LINKS)||
      Files.isSymbolicLink(file.toPath())||file.length()!=0)
    throw new IOException("손상된 즐겨찾기 표시를 변경하지 않았어요.");
   Files.delete(file.toPath());return false;
  }
  if(!file.createNewFile())throw new IOException("즐겨찾기를 저장하지 못했어요.");
  return true;
 }
}
