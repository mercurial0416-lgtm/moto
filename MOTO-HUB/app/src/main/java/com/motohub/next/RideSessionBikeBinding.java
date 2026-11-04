package com.motohub.next;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.StandardCopyOption;
import java.util.*;

/** Change a completed ride's bike association as one logical multi-section ride.
 * Never modify raw GPS, the odometer ledger, or a track already applied to ODO.
 */
final class RideSessionBikeBinding {
 private RideSessionBikeBinding(){}
 static void write(File filesDir, File selected, String bikeId, Set<String> alreadyApplied)throws IOException{
  write(filesDir,selected,bikeId,alreadyApplied,()->{});
 }
 /** Failure hook exists only for local rollback regression tests. */
 static synchronized void write(File filesDir,File selected,String bikeId,
  Set<String> alreadyApplied,Runnable afterEachWrite)throws IOException{
  if(filesDir==null||selected==null||!RideBikeBinding.valid(bikeId)||
     alreadyApplied==null||afterEachWrite==null)
   throw new IOException("라이딩 바이크 연결 요청이 올바르지 않습니다.");
  File rides=TrackFile.directory(filesDir).getCanonicalFile();
  if(!rides.equals(selected.getCanonicalFile().getParentFile())||
     !RideMileageIntegrity.validCompleted(selected))
   throw new IOException("완료된 로컬 GPS 라이딩만 바이크를 변경할 수 있어요.");
  String session=RideSegmentGroup.id(selected);
  List<File> members=RideSegmentGroup.members(filesDir,selected.getName());
  if(members.isEmpty()||!members.contains(selected))
   throw new IOException("라이딩의 완료된 GPS 구간을 찾지 못했어요.");
  requireComplete(filesDir,session,members.size(),alreadyApplied);
  Map<File,byte[]> originals=new LinkedHashMap<>();
  for(File part:members){
   if(!RideMileageIntegrity.validCompleted(part)||alreadyApplied.contains(part.getName()))
    throw new IOException("미완료·주행거리 반영 구간은 바이크를 변경할 수 없어요.");
   // Corrupted bike associations must be explicitly repaired, not overwritten.
   RideBikeBinding.readStrict(part);
   File sidecar=new File(part.getPath()+".bike");
   if(Files.isSymbolicLink(sidecar.toPath()))
    throw new IOException("안전하지 않은 바이크 연결 파일입니다.");
   byte[] before=null;
   if(Files.exists(sidecar.toPath(),LinkOption.NOFOLLOW_LINKS)){
    if(!Files.isRegularFile(sidecar.toPath(),LinkOption.NOFOLLOW_LINKS)||sidecar.length()>64)
     throw new IOException("바이크 연결 정보가 손상돼 저장을 중단했습니다.");
    before=Files.readAllBytes(sidecar.toPath());
   }
   originals.put(part,before);
  }
  List<File> attempted=new ArrayList<>();
  try{
   for(File part:originals.keySet()){
    attempted.add(part);
    RideBikeBinding.write(part,bikeId);
    afterEachWrite.run();
   }
   requireComplete(filesDir,session,members.size(),alreadyApplied);
  }catch(Exception reason){
   IOException failure=new IOException("라이딩 전체 바이크 연결 실패 · 이전 연결을 복원합니다.",reason);
   Collections.reverse(attempted);
   for(File part:attempted){
    try{restore(part,originals.get(part));}
    catch(IOException rollback){failure.addSuppressed(rollback);}
   }
   throw failure;
  }
 }
 private static void requireComplete(File filesDir,String session,int expected,Set<String> applied)throws IOException{
  if(session.isEmpty()||expected<=0)throw new IOException("라이딩 세션 식별 정보가 없습니다.");
  int seen=0;
  for(File part:TrackFile.tracks(filesDir)){
   if(!session.equals(RideSegmentGroup.id(part)))continue;
   seen++;
   if(applied.contains(part.getName()))
    throw new IOException("주행거리에 이미 반영된 GPS 구간은 바이크를 변경할 수 없어요.");
   if(!RideMileageIntegrity.validCompleted(part))
    throw new IOException("완료되지 않은 GPS 구간이 있어 바이크를 변경할 수 없어요.");
  }
  if(seen!=expected)
   throw new IOException("라이딩 GPS 구간이 변경됐어요. 기록 저장 완료 후 다시 시도해 주세요.");
 }
 private static void restore(File track,byte[] oldBytes)throws IOException{
  File sidecar=new File(track.getPath()+".bike");
  if(Files.isSymbolicLink(sidecar.toPath()))
   throw new IOException("바이크 연결 복원 경로가 안전하지 않습니다.");
  if(oldBytes==null){Files.deleteIfExists(sidecar.toPath());return;}
  File temp=new File(sidecar.getPath()+".rollback");
  if(Files.isSymbolicLink(temp.toPath()))
   throw new IOException("바이크 연결 복원 임시 경로가 안전하지 않습니다.");
  try{
   try(java.io.FileOutputStream out=new java.io.FileOutputStream(temp)){
    out.write(oldBytes);out.getFD().sync();
   }
   try{Files.move(temp.toPath(),sidecar.toPath(),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
   catch(java.nio.file.AtomicMoveNotSupportedException unsupported){
    Files.move(temp.toPath(),sidecar.toPath(),StandardCopyOption.REPLACE_EXISTING);
   }
  }finally{temp.delete();}
 }
}
