package com.motohub.next;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/** Offline, rollback-safe category tagging for all sections of one GPS ride. */
final class RideSessionCategory {
 private RideSessionCategory(){}
 /** Apply one explicit label to all completed GPS sections from the same recording.
  * On any failure restore the exact old sidecars, including their absence.
  * Never touch .track originals, bike bindings, import flags, or backups.
  */
 static void write(File filesDir,File selected,String category)throws IOException {
  write(filesDir,selected,category,()->{});
 }
 /** Failure injection is only for local regression testing the rollback. */
 static void write(File filesDir,File selected,String category,Runnable afterEachWrite)throws IOException {
  if(!RideCategory.valid(category)||filesDir==null||selected==null||afterEachWrite==null)
   throw new IOException("라이딩 분류 요청이 올바르지 않습니다.");
  File rides=TrackFile.directory(filesDir).getCanonicalFile();
  if(!rides.equals(selected.getCanonicalFile().getParentFile())||
     !RideMileageIntegrity.validCompleted(selected))
   throw new IOException("완료된 로컬 라이딩만 한 번에 분류할 수 있어요.");
  java.util.List<File> members=RideSegmentGroup.members(filesDir,selected.getName());
  if(members.isEmpty()||!members.contains(selected))
   throw new IOException("라이딩의 GPS 구간을 확인할 수 없습니다.");
  // Snapshot every target before writing the first one.
  java.util.Map<File,byte[]> originals=new java.util.LinkedHashMap<>();
  for(File member:members){
   if(!RideMileageIntegrity.validCompleted(member))
    throw new IOException("완료되지 않은 GPS 구간이 있어 분류를 중단했습니다.");
   File sidecar=new File(member.getPath()+".category");
   if(Files.isSymbolicLink(sidecar.toPath()))
    throw new IOException("안전하지 않은 라이딩 분류 파일입니다.");
   byte[] prior=null;
   if(Files.exists(sidecar.toPath(),java.nio.file.LinkOption.NOFOLLOW_LINKS)){
    if(!Files.isRegularFile(sidecar.toPath(),java.nio.file.LinkOption.NOFOLLOW_LINKS)||sidecar.length()>64)
     throw new IOException("기존 라이딩 분류 정보가 손상됐어요.");
    prior=Files.readAllBytes(sidecar.toPath());
   }
   originals.put(member,prior);
  }
  java.util.List<File> attempted=new java.util.ArrayList<>();
  try{
   for(File member:originals.keySet()){
    attempted.add(member);
    RideCategory.write(member,category);
    afterEachWrite.run();
   }
  }catch(Exception cause){
   IOException failure=new IOException("라이딩 전체 분류 저장 실패 · 이전 분류로 되돌립니다.",cause);
   java.util.Collections.reverse(attempted);
   for(File member:attempted){
    try{restoreSidecar(member,originals.get(member));}
    catch(IOException rollback){failure.addSuppressed(rollback);}
   }
   throw failure;
  }
 }
 private static void restoreSidecar(File track,byte[] oldBytes)throws IOException{
  File sidecar=new File(track.getPath()+".category");
  if(Files.isSymbolicLink(sidecar.toPath()))throw new IOException("분류 복원 경로가 안전하지 않습니다.");
  if(oldBytes==null){Files.deleteIfExists(sidecar.toPath());return;}
  File temporary=new File(sidecar.getPath()+".rollback");
  if(Files.isSymbolicLink(temporary.toPath()))throw new IOException("분류 복원 임시 경로가 안전하지 않습니다.");
  try{
   try(java.io.FileOutputStream out=new java.io.FileOutputStream(temporary)){
    out.write(oldBytes);out.getFD().sync();
   }
   try{Files.move(temporary.toPath(),sidecar.toPath(),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
   catch(java.nio.file.AtomicMoveNotSupportedException unsupported){
    Files.move(temporary.toPath(),sidecar.toPath(),StandardCopyOption.REPLACE_EXISTING);
   }
  }finally{temporary.delete();}
 }
}
