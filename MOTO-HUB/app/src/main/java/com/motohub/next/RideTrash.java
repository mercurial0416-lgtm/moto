package com.motohub.next;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Manually reversible offline GPS quarantine; no automatic purge, no server. */
final class RideTrash {
 static final String[] SIDECARS={".done",".imported",".recovered",".unmerged",".category",".bike",".favorite",".mergedinto"};
 private RideTrash(){}
 private static boolean regular(File file){
  return Files.isRegularFile(file.toPath(),LinkOption.NOFOLLOW_LINKS)&&!Files.isSymbolicLink(file.toPath());
 }
 private static File root(File files)throws IOException{
  File root=new File(files,"rides-trash");
  if(Files.isSymbolicLink(root.toPath()))throw new IOException("복구함 경로가 안전하지 않습니다.");
  if(!root.isDirectory()&&!root.mkdirs())throw new IOException("GPS 복구함 생성 실패");
  return root;
 }
 private static List<File> members(File folder,String name)throws IOException{
  List<File> paths=new ArrayList<>();
  File track=new File(folder,name);
  if(!regular(track))throw new IOException("GPS 파일이 존재하지 않습니다.");
  for(String ext:SIDECARS){
   File file=new File(folder,name+ext);
   if(Files.exists(file.toPath(),LinkOption.NOFOLLOW_LINKS)){
    if(!regular(file))throw new IOException("안전하지 않은 GPS 부속 정보");
    paths.add(file);
   }
  }
  paths.add(track); // Commit the track last. Sidecars never outlive a confirmed GPS move.
  return paths;
 }
 private static void relocate(List<File> files,File target)throws IOException{
  List<File> moved=new ArrayList<>();
  try{
   for(File source:files){
    File destination=new File(target,source.getName());
    if(Files.exists(destination.toPath(),LinkOption.NOFOLLOW_LINKS))
     throw new IOException("동일 GPS 복구 파일이 이미 있습니다.");
    Files.move(source.toPath(),destination.toPath(),StandardCopyOption.ATOMIC_MOVE);
    moved.add(source);
   }
  }catch(IOException failure){
   // Restore already-moved source files; never silently discard failed rollback.
   for(int i=moved.size()-1;i>=0;i--){
    File original=moved.get(i),stored=new File(target,original.getName());
    try{Files.move(stored.toPath(),original.toPath(),StandardCopyOption.ATOMIC_MOVE);}
    catch(IOException rollback){failure.addSuppressed(rollback);}
   }
   throw failure;
  }
 }
 static synchronized void move(File files,File selected)throws IOException{
  File rides=TrackFile.directory(files);
  if(selected==null||!selected.getName().matches(BackupFormat.TRACK)||
      !rides.getCanonicalFile().equals(selected.getCanonicalFile().getParentFile()))
   throw new IOException("앱 저장 경로 밖의 GPS 원본입니다.");
  File parent=root(files),folder=new File(parent,selected.getName());
  if(Files.exists(folder.toPath(),LinkOption.NOFOLLOW_LINKS))throw new IOException("이미 복구함에 같은 GPS가 있습니다.");
  List<File> original=members(rides,selected.getName());
  if(!folder.mkdir())throw new IOException("GPS 복구함에 저장할 수 없습니다.");
  try{relocate(original,folder);}
  catch(IOException failure){
   File[] still=folder.listFiles();
   if(still!=null&&still.length==0)folder.delete();
   throw failure;
  }
 }
 /** Complete interrupted GPS moves based on which directory contains the committed .track.
   * Never guess when both originals exist or an orphan has no track. */
 static synchronized int recoverInterrupted(File files)throws IOException{
  File active=TrackFile.directory(files),vault=root(files);
  File[] folders=vault.listFiles();
  if(folders==null)throw new IOException("GPS 복구함을 읽을 수 없습니다.");
  int count=0;
  for(File folder:folders){
   if(!folder.getName().matches(BackupFormat.TRACK)||
       Files.isSymbolicLink(folder.toPath())||
       !Files.isDirectory(folder.toPath(),LinkOption.NOFOLLOW_LINKS))
    throw new IOException("복구함에 알 수 없는 폴더가 있습니다. 원본을 보존합니다.");
   String id=folder.getName();
   File saved=new File(folder,id),original=new File(active,id);
   boolean hasSaved=regular(saved),hasOriginal=regular(original);
   if(Files.exists(saved.toPath(),LinkOption.NOFOLLOW_LINKS)&&!hasSaved||
      Files.exists(original.toPath(),LinkOption.NOFOLLOW_LINKS)&&!hasOriginal)
    throw new IOException("GPS 경로가 안전하지 않아 복구를 중단했습니다.");
   if(hasSaved&&hasOriginal)throw new IOException("동일 GPS가 원본과 복구함 양쪽에 있습니다.");
   if(!hasSaved&&!hasOriginal){
    File[] orphan=folder.listFiles();
    if(orphan==null)throw new IOException("복구함 목록 확인 실패");
    if(orphan.length>0)throw new IOException("원본 없는 GPS 부가 파일을 보존했습니다.");
    if(!folder.delete())throw new IOException("비어 있는 복구함을 정리할 수 없습니다.");
    continue;
   }
   File from=hasSaved?active:folder,to=hasSaved?folder:active;
   for(String ext:SIDECARS){
    File sidecar=new File(from,id+ext),destination=new File(to,id+ext);
    if(!Files.exists(sidecar.toPath(),LinkOption.NOFOLLOW_LINKS))continue;
    if(!regular(sidecar)||Files.exists(destination.toPath(),LinkOption.NOFOLLOW_LINKS))
     throw new IOException("GPS 중단 복구 시 부가 파일 충돌: "+id);
    Files.move(sidecar.toPath(),destination.toPath(),StandardCopyOption.ATOMIC_MOVE);
    count++;
   }
   if(!hasSaved&&!folder.delete())throw new IOException("GPS 복원 후 빈 복구함 정리 실패");
  }
  return count;
 }
 static synchronized List<File> entries(File files)throws IOException{
  File[] folders=root(files).listFiles();
  List<File> tracks=new ArrayList<>();
  if(folders!=null)for(File folder:folders){
   if(!folder.getName().matches(BackupFormat.TRACK)||
       Files.isSymbolicLink(folder.toPath())||
       !Files.isDirectory(folder.toPath(),LinkOption.NOFOLLOW_LINKS))
    throw new IOException("잘못된 GPS 복구함 경로입니다.");
   File track=new File(folder,folder.getName());
   if(!regular(track))throw new IOException("복구함 GPS 원본이 누락되거나 안전하지 않습니다.");
   tracks.add(track);
  }
  tracks.sort((a,b)->b.getName().compareTo(a.getName()));
  return tracks;
 }
 static synchronized File restore(File files,File selected)throws IOException{
  File root=root(files),rides=TrackFile.directory(files);
  if(selected==null||!selected.getName().matches(BackupFormat.TRACK))throw new IOException("잘못된 GPS 복원 대상");
  File folder=selected.getParentFile();
  if(!root.getCanonicalFile().equals(folder.getCanonicalFile().getParentFile())||
      !folder.getName().equals(selected.getName())||
      Files.isSymbolicLink(folder.toPath()))throw new IOException("GPS 복구함 경로 확인 실패");
  List<File> items=members(folder,selected.getName());
  for(File source:items)if(Files.exists(new File(rides,source.getName()).toPath(),LinkOption.NOFOLLOW_LINKS))
    throw new IOException("이미 같은 GPS가 라이딩 내역에 있습니다. 덮어쓰지 않습니다.");
  relocate(items,rides);
  if(!folder.delete())throw new IOException("GPS는 복원됐으나 빈 복구함 정리에 실패했습니다.");
  return new File(rides,selected.getName());
 }
}
