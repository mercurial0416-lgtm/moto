package com.motohub.next;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Set;

/** Fail closed on merged or incomplete tracks before GPS mileage enters garage ODO. */
final class RideMileageIntegrity {
 private RideMileageIntegrity(){}
 static boolean eligibleForReview(File track,Set<String> alreadyApplied,String bikeId){
  if(track==null||alreadyApplied==null||bikeId==null||bikeId.isEmpty())return false;
  if(alreadyApplied.contains(track.getName())||!validCompleted(track))return false;
  try{return bikeId.equals(RideBikeBinding.readStrict(track));}
  catch(IOException ignored){return false;}
 }
 static boolean validCompleted(File track){
  if(track==null||!track.isFile()||!track.getName().matches("[0-9]{10,15}-[a-f0-9-]{36}\\.track"))return false;
  if(Files.isSymbolicLink(track.toPath()))return false;
  if(!new File(track.getPath()+".done").isFile()&&!new File(track.getPath()+".recovered").isFile())return false;
  if(RideTrackMerge.mergedSource(track)||RideTrackMerge.archived(track))return false;
  return true;
 }
 static void requireApplyable(File track,boolean applied,File expectedRidesDir,long expectedBytes,long expectedModified)throws IOException{
  if(applied)throw new IOException("이미 주행거리에 반영한 기록입니다.");
  if(!validCompleted(track))throw new IOException("완료·복구된 독립 라이딩만 주행거리에 반영할 수 있어요. 통합 원본이나 보관본은 제외됩니다.");
  if(expectedRidesDir==null||!track.getCanonicalFile().getParentFile().equals(expectedRidesDir.getCanonicalFile()))
   throw new IOException("이 앱의 GPS 라이딩 파일이 아닙니다.");
  if(expectedBytes<=0||track.length()!=expectedBytes||track.lastModified()!=expectedModified)
   throw new IOException("라이딩 원본이 변경됐어요. 분석을 다시 열어 확인해 주세요.");
 }
}
