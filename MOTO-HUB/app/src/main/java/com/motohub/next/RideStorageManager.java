package com.motohub.next;
import android.app.AlertDialog;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.util.*;
/** Local GPS storage overview. No scheduled deletion. Only user-selected, post-backup files. */
final class RideStorageManager {
 private static final String[] SIDECARS={".done",".imported",".recovered",".unmerged",".category",".bike",".favorite",".mergedinto"};
 private RideStorageManager(){}
 static void show(MainActivity a){
  if(RideService.running||RideService.autoArmed){a.toast("GPS 또는 자동 감지가 작동 중에는 파일을 정리하지 않아요.");return;}
  a.toast("GPS 파일 크기 확인 중…");
  new Thread(()->{
   try{
    List<File> tracks=TrackFile.tracks(a.getFilesDir());
    long bytes=0;for(File f:tracks)bytes+=f.length();
    final long total=bytes;
    a.runOnUiThread(()->{
     if(a.isFinishing()||a.isDestroyed())return;
     String[] labels=new String[Math.min(30,tracks.size())];
     for(int i=0;i<labels.length;i++){
      File f=tracks.get(i);labels[i]=RideHistoryLabel.title(f,java.time.ZoneId.systemDefault())+
       " · "+String.format(Locale.KOREA,"%.2f MB",f.length()/1_000_000.0);
     }
     new AlertDialog.Builder(a).setTitle("GPS 원본 저장공간")
      .setMessage("기록 "+tracks.size()+"건 · 합계 약 "+String.format(Locale.KOREA,"%.1f MB",total/1_000_000.0)+
       "\n최근 "+labels.length+"건을 확인할 수 있어요. 자동 삭제는 없습니다."+
       "\n삭제는 전체 ZIP 백업을 만들고 정상 검증한 후, 해당 기록을 직접 선택할 때만 가능합니다.")
      .setItems(labels,(d,index)->confirm(a,tracks.get(index)))
      .setNegativeButton("닫기",null).show();
    });
   }catch(Exception e){a.runOnUiThread(()->{if(!a.isFinishing()&&!a.isDestroyed())a.error("GPS 저장공간 점검 실패: "+e.getMessage());});}
  },"moto-gps-storage-list").start();
 }
 private static void confirm(MainActivity a,File track){
  long backedUpAt=a.getSharedPreferences("verified_full_export",0).getLong("completed_at",0);
  if(a.readOnly||RideService.running||RideService.autoArmed||backedUpAt<=track.lastModified()||
     backedUpAt==0){
   new AlertDialog.Builder(a).setTitle("GPS 원본 삭제 불가")
    .setMessage("해당 기록 생성 이후 검증 완료된 전체 ZIP 백업이 필요합니다. GPS 기록이 진행 중이거나 원본 보호 상태에서도 삭제할 수 없습니다.")
    .setPositiveButton("확인",null).show();return;
  }
  if(RideTrackMerge.mergedSource(track)||RideTrackMerge.archived(track)){
   a.error("통합·보관된 GPS 원본은 다른 기록과 연결될 수 있어 개별 삭제를 지원하지 않아요.");return;
  }
  new AlertDialog.Builder(a).setTitle("이 GPS 원본을 삭제할까요?")
   .setMessage(RideHistoryLabel.title(track,java.time.ZoneId.systemDefault())+
    "\n삭제하면 앱에서는 복구할 수 없습니다. 검증된 외부 ZIP 백업은 별도로 보관하세요.")
   .setNegativeButton("취소",null)
   .setPositiveButton("삭제 절차 진행",(d,w)->new AlertDialog.Builder(a)
    .setTitle("GPS 파일 최종 확인")
    .setMessage("선택한 GPS와 부가정보 파일을 영구 삭제합니다. 정말 삭제할까요?")
    .setNegativeButton("돌아가기",null)
    .setPositiveButton("선택한 파일만 삭제",(dd,ww)->removeInBackground(a,track)).show()).show();
 }
 private static void removeInBackground(MainActivity a,File track){
  new Thread(()->{
   try{
    File rides=TrackFile.directory(a.getFilesDir());
    String filename=track.getName();
    if(!filename.matches(BackupFormat.TRACK)||!rides.getCanonicalFile().equals(track.getCanonicalFile().getParentFile()))
     throw new IOException("GPS 경로 검증 실패");
    long backup=a.getSharedPreferences("verified_full_export",0).getLong("completed_at",0);
    if(backup<=0||backup<=track.lastModified()||RideService.running||RideService.autoArmed||a.readOnly)
     throw new IOException("백업 또는 기록 상태가 변경됐어요. 삭제를 취소했습니다.");
    if(!RideMileageIntegrity.validCompleted(track))throw new IOException("미완료 GPS는 수동 삭제하지 않습니다.");
    // Prevent removal of a merge target referenced by another saved source.
    for(File other:TrackFile.tracks(a.getFilesDir())){
     if(other.equals(track))continue;
     File link=new File(other.getPath()+".mergedinto");
     if(link.isFile()){
      String target=new String(Files.readAllBytes(link.toPath()),java.nio.charset.StandardCharsets.US_ASCII);
      if(filename.equals(target))throw new IOException("다른 통합 기록이 참조하는 GPS라 삭제할 수 없어요.");
     }
    }
    for(String suffix:SIDECARS){
     File file=new File(track.getPath()+suffix);
     if(Files.isSymbolicLink(file.toPath()))throw new IOException("안전하지 않은 GPS 부가정보 경로입니다.");
    }
    for(String suffix:SIDECARS)Files.deleteIfExists(new File(track.getPath()+suffix).toPath());
    Files.delete(track.toPath());
    a.runOnUiThread(()->{if(!a.isFinishing()&&!a.isDestroyed()){a.toast("선택한 GPS 원본만 삭제했습니다.");a.render();}});
   }catch(Exception error){
    a.runOnUiThread(()->{if(!a.isFinishing()&&!a.isDestroyed())a.error("GPS 삭제 취소: "+error.getMessage());});
   }
  },"moto-single-gps-delete").start();
 }
}
