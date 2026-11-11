package com.motohub.next;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
/** Read-only integrity proof for ALL parts of one automatic GPS session.
 * Never deletes, renames, relinks or writes recorded tracks. */
final class RideSessionVerification {
 static final class Result {
  final boolean ok;final String reason;final int parts,points;
  Result(boolean ok,String reason,int parts,int points){
   this.ok=ok;this.reason=reason;this.parts=parts;this.points=points;
  }
 }
 private RideSessionVerification(){}
 static Result fail(String reason){return new Result(false,reason,0,0);}
 static Result verify(File filesDir,File finalPart,int rotatedParts,int actualWrites,double km){
  if(finalPart==null||!finalPart.isFile())return fail("마지막 GPS 구간 원본 없음");
  String id=RideActiveSessionPolicy.session(finalPart.getName());
  if(id.isEmpty()||rotatedParts<0||actualWrites<1)return fail("GPS 세션 식별 또는 좌표 수 오류");
  File folder;
  try{folder=TrackFile.directory(filesDir);}
  catch(IOException error){return fail("GPS 저장 폴더 확인 실패 · 원본 보존");}
  final File[] matches=folder.listFiles((dir,name)->
   id.equals(RideActiveSessionPolicy.session(name)));
  if(matches==null)return fail("분할 GPS 파일 목록 확인 실패");
  if(matches.length!=rotatedParts+1)return new Result(false,
   "분할 GPS 구간 누락 · 예상 "+(rotatedParts+1)+"개, 확인 "+matches.length+"개",
   matches.length,0);
  int all=0;
  for(File file:matches){
   try{
    if(Files.isSymbolicLink(file.toPath())||
       !Files.isRegularFile(file.toPath(),LinkOption.NOFOLLOW_LINKS))
     return fail("안전하지 않은 GPS 원본 감지");
    File marker=new File(file.getPath()+".done");
    if(Files.isSymbolicLink(marker.toPath())||
       !Files.isRegularFile(marker.toPath(),LinkOption.NOFOLLOW_LINKS))
     return fail("분할 GPS 완료 마커 누락 · 원본 보존");
    int read=TrackFile.read(file,(lat,lon,accuracy,time)->{});
    if(read<1)return fail("분할 GPS 구간의 좌표가 없음");
    try(java.io.RandomAccessFile in=new java.io.RandomAccessFile(file,"r")){
     if(in.length()==0)return fail("분할 GPS 파일이 비어 있음");
     in.seek(in.length()-1);
     if(in.read()!='\n')return fail("GPS 마지막 좌표 행이 불완전함");
    }
    if(all>Integer.MAX_VALUE-read)return fail("GPS 좌표 수가 너무 큼");
    all+=read;
   }catch(Exception error){return fail("분할 GPS 구간 읽기 실패 · 원본 복구에서 확인");}
  }
  // A size-based part rollover can repeat one real boundary point; a
  // parking-induced split does NOT repeat it. Nothing else may be invented.
  if(all<actualWrites||all>actualWrites+rotatedParts)return new Result(false,
   "분할 구간 좌표 합계 불일치 · 원본 보존",matches.length,all);
  if(all<2)return new Result(false,"GPS 좌표가 2개 미만",matches.length,all);
  if(!Double.isFinite(km)||km<0)return new Result(false,"GPS 이동거리 계산 실패",matches.length,all);
  if(km<0.10)return new Result(false,"GPS 이동거리 0.1km 미만 · 짧은 시험",matches.length,all);
  return new Result(true,
   "전체 GPS "+matches.length+"개 구간·"+all+"좌표 검증 완료",matches.length,all);
 }
}
