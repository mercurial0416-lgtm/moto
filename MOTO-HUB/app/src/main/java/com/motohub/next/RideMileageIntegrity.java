package com.motohub.next;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
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
 /** Read a bounded completed GPS original, never hash a symlink, missing or oversized track. */
 static String fingerprint(File track)throws IOException{
  if(!validCompleted(track)||track.length()>TrackFile.MAX_BYTES||track.length()==0)
   throw new IOException("검증되지 않은 GPS 라이딩 파일입니다.");
  try{
   MessageDigest digest=MessageDigest.getInstance("SHA-256");
   try(InputStream in=new FileInputStream(track)){
    byte[] buffer=new byte[16384];int n;long size=0;
    while((n=in.read(buffer))!=-1){
     size+=n;
     if(size>TrackFile.MAX_BYTES)throw new IOException("GPS 원본 크기 초과");
     digest.update(buffer,0,n);
    }
   }
   StringBuilder hex=new StringBuilder();
   for(byte b:digest.digest())hex.append(String.format(java.util.Locale.ROOT,"%02x",b&255));
   return hex.toString();
  }catch(NoSuchAlgorithmException e){throw new IOException("SHA-256 파일 검사 불가",e);}
 }
 /** Hash is read on a worker thread. The UI thread only rechecks metadata at commit. */
 static void requireApplyable(File track,boolean applied,File expectedRidesDir,long expectedBytes,long expectedModified,
                              String expectedSha256)throws IOException{
  requireApplyable(track,applied,expectedRidesDir,expectedBytes,expectedModified);
  if(expectedSha256==null||!expectedSha256.matches("[a-f0-9]{64}"))
   throw new IOException("분석할 때의 GPS 검증값이 없습니다.");
  String current=fingerprint(track);
  if(!MessageDigest.isEqual(expectedSha256.getBytes(StandardCharsets.US_ASCII),
                            current.getBytes(StandardCharsets.US_ASCII)))
   throw new IOException("GPS 내용이 분석 후 변경됐어요. 기록을 다시 열어 확인해 주세요.");
  // File could be replaced during hashing: check metadata again.
  requireApplyable(track,applied,expectedRidesDir,expectedBytes,expectedModified);
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
