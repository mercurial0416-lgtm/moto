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
 /** Finish interrupted transfers at startup without overwriting any GPS file.
  * The presence of the source track determines which directory owns the session.
  * A crash can occur between sidecar moves and the final track move. */
 static synchronized int recoverInterrupted(File files)throws IOException{
  File rides=TrackFile.directory(files),trash=root(files);
  File[] folders=trash.listFiles();
  int repaired=0;
  if(folders==null)return 0;
  for(File folder:folders){
   if(Files.isSymbolicLink(folder.toPath())||!Files.isDirectory(folder.toPath(),LinkOption.NOFOLLOW_LINKS)||
      !folder.getName().matches(BackupFormat.TRACK))
    throw new IOException("복구함에 알 수 없는 항목이 있습니다.");
   String name=folder.getName();
   File live=new File(rides,name),stored=new File(folder,name);
   boolean inLive=regular(live),inTrash=regular(stored);
   if(inLive&&inTrash)throw new IOException("GPS 원본이 두 위치에 중복되어 복구를 중단합니다: "+name);
   if(!inLive&&!inTrash){
    File[] loose=folder.listFiles();
    if(loose!=null&&loose.length==0){
     // Crash immediately after creating the quarantine folder, before moving sidecars.
     // Empty cleanup is best-effort; never put an otherwise healthy garage in read-only mode.
     folder.delete();
     continue;
    }
    throw new IOException("GPS 원본이 양쪽 모두 없어 자동 복구할 수 없습니다: "+name);
   }
   File owner=inTrash?folder:rides,orphan=inTrash?rides:folder;
   for(String suffix:SIDECARS){
    File stray=new File(orphan,name+suffix);
    if(!Files.exists(stray.toPath(),LinkOption.NOFOLLOW_LINKS))continue;
    if(!regular(stray))throw new IOException("복구함 부속정보가 안전하지 않습니다.");
    File destination=new File(owner,stray.getName());
    if(Files.exists(destination.toPath(),LinkOption.NOFOLLOW_LINKS))
     throw new IOException("중복 GPS 부속정보가 있어 덮어쓰지 않습니다: "+name+suffix);
    Files.move(stray.toPath(),destination.toPath(),StandardCopyOption.ATOMIC_MOVE);
    repaired++;
   }
   if(inLive){
    File[] leftover=folder.listFiles();
    if(leftover==null)throw new IOException("GPS 복구함 남은 파일을 확인하지 못했습니다.");
    if(leftover.length==0)folder.delete(); // Cleanup failure is not a corrupt GPS move.
    else throw new IOException("GPS 복원 후 복구함에 알 수 없는 잔여 파일이 있습니다.");
   }
  }
  return repaired;
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
 static synchronized List<File> entries(File files)throws IOException{
  File[] folders=root(files).listFiles();
  List<File> tracks=new ArrayList<>();
  if(folders!=null)for(File folder:folders){
   if(!folder.getName().matches(BackupFormat.TRACK)||
       Files.isSymbolicLink(folder.toPath())||
       !Files.isDirectory(folder.toPath(),LinkOption.NOFOLLOW_LINKS))continue;
   File track=new File(folder,folder.getName());
   if(regular(track))tracks.add(track);
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
  // GPS and every sidecar have ALREADY moved successfully. Failure to delete
  // the now-empty folder is cleanup only, never a failed GPS restore.
  // recoverInterrupted() removes leftover empty folders on a later startup.
  folder.delete();
  return new File(rides,selected.getName());
 }
}
