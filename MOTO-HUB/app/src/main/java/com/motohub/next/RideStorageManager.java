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
 static final int PAGE=30;
 static void show(MainActivity a){
  if(RestoreRideGate.blocked(RideService.running,RideService.autoArmed)){
   a.toast("GPS 기록·복원·백업 중에는 파일을 관리하지 않아요.");return;
  }
  new AlertDialog.Builder(a).setTitle("GPS 저장공간")
   .setMessage("원본과 복구함 기록을 검색하거나 날짜순으로 확인할 수 있습니다. 복구함은 자동 삭제되지 않습니다. 앱 삭제 전에 전체 ZIP 백업을 보관하세요.")
   .setNegativeButton("닫기",null)
   .setNeutralButton("복구함", (d,w)->showTrash(a))
   .setPositiveButton("원본 관리",(d,w)->showPage(a,false,"",0)).show();
 }
 private static void showTrash(MainActivity a){showPage(a,true,"",0);}
 private static void showPage(MainActivity a,boolean deleted,String search,int offset){
  if(RestoreRideGate.blocked(RideService.running,RideService.autoArmed)){a.toast("GPS 작업 중에는 관리할 수 없습니다.");return;}
  new Thread(()->{
   try{
    List<File> all=deleted?RideTrash.entries(a.getFilesDir()):TrackFile.tracks(a.getFilesDir());
    List<File> matching=new ArrayList<>();
    long total=0;
    String term=search==null?"":search.trim().toLowerCase(Locale.ROOT);
    for(File f:all){
     total+=f.length();
     String label=RideHistoryLabel.title(f,java.time.ZoneId.systemDefault());
     if(term.isEmpty()||f.getName().toLowerCase(Locale.ROOT).contains(term)||
         label.toLowerCase(Locale.ROOT).contains(term))matching.add(f);
    }
    int begin=Math.min(Math.max(0,offset),matching.size());
    int end=Math.min(begin+PAGE,matching.size());
    List<File> page=new ArrayList<>(matching.subList(begin,end));
    final int shownBegin=begin,shownEnd=end,totalCount=all.size(),found=matching.size();
    final long totalBytes=total;
    a.runOnUiThread(()->{
     if(a.isFinishing()||a.isDestroyed())return;
     android.widget.EditText input=new android.widget.EditText(a);
     input.setSingleLine(true);input.setTextSize(15);input.setText(search);
     input.setHint("날짜 · 파일명 검색");
     input.setPadding(a.dp(20),a.dp(8),a.dp(20),a.dp(8));
     String[] labels=new String[page.size()];
     for(int i=0;i<labels.length;i++){
      File file=page.get(i);
      labels[i]=RideHistoryLabel.title(file,java.time.ZoneId.systemDefault())+
       String.format(Locale.KOREA," · %.2f MB",file.length()/1_000_000.0);
     }
     AlertDialog.Builder builder=new AlertDialog.Builder(a)
      .setTitle(deleted?"GPS 복구함":"GPS 원본 관리")
      .setMessage("전체 "+totalCount+"건 · "+
        String.format(Locale.KOREA,"%.1f MB",totalBytes/1_000_000.0)+
        "\n검색 "+found+"건 · "+(found==0?0:shownBegin+1)+"–"+shownEnd+
        " / "+found+"\n검색 후 키보드의 검색 버튼을 누르세요.")
      .setView(input)
      .setItems(labels,(d,index)->{
       File track=page.get(index);
       if(deleted)confirmRestore(a,track);
       else confirm(a,track);
      })
      .setNegativeButton("닫기",null);
     if(shownEnd<found)builder.setPositiveButton("다음 30건",(d,w)->showPage(a,deleted,term,shownEnd));
     if(shownBegin>0)builder.setNeutralButton("이전 30건",(d,w)->showPage(a,deleted,term,Math.max(0,shownBegin-PAGE)));
     AlertDialog dialog=builder.create();
     input.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);
     input.setOnEditorActionListener((v,id,event)->{
      if(id!=android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH)return false;
      String entered=input.getText().toString();
      dialog.dismiss();showPage(a,deleted,entered,0);return true;
     });
     dialog.show();
    });
   }catch(Exception ex){a.runOnUiThread(()->{if(!a.isFinishing()&&!a.isDestroyed())
    a.error("GPS 목록을 읽지 못했습니다: "+ex.getMessage());});}
  },"moto-gps-paged-search").start();
 }
 private static void confirmRestore(MainActivity a,File track){
  new AlertDialog.Builder(a).setTitle("GPS 복구함에서 되돌리기")
   .setMessage(RideHistoryLabel.title(track,java.time.ZoneId.systemDefault())+
    "\n원본·부가 정보를 라이딩 목록으로 복원하며, 완료 요약도 다시 생성합니다.")
   .setNegativeButton("취소",null)
   .setPositiveButton("복원",(d,w)->new Thread(()->{
    boolean held=false;
    try{
     if(!RestoreRideGate.begin(RideService.running,RideService.autoArmed))
      throw new IOException("GPS 기록·백업·복원 중에는 복원할 수 없습니다.");
     held=true;
     if(a.readOnly)throw new IOException("원본 보호 중에는 복원할 수 없습니다.");
     File restored=RideTrash.restore(a.getFilesDir(),track);
     RideCompletionInbox.restored(a,restored);
     a.runOnUiThread(()->{if(!a.isFinishing()&&!a.isDestroyed()){
      a.toast("GPS 원본과 완료 요약을 복원했습니다.");a.render();
     }});
    }catch(Exception ex){
     a.runOnUiThread(()->{if(!a.isFinishing()&&!a.isDestroyed())a.error("GPS 복원 실패: "+ex.getMessage());});
    }finally{if(held)RestoreRideGate.end();}
   },"moto-gps-restore").start()).show();
 }
 private static void confirm(MainActivity a,File track){
  if(a.readOnly||RideService.running||RideService.autoArmed){
   new AlertDialog.Builder(a).setTitle("GPS 원본 삭제 불가")
    .setMessage("해당 기록 생성 이후 검증 완료된 전체 ZIP 백업이 필요합니다. GPS 기록이 진행 중이거나 원본 보호 상태에서도 삭제할 수 없습니다.")
    .setPositiveButton("확인",null).show();return;
  }
  if(RideTrackMerge.mergedSource(track)||RideTrackMerge.archived(track)){
   a.error("통합·보관된 GPS 원본은 다른 기록과 연결될 수 있어 개별 삭제를 지원하지 않아요.");return;
  }
  new AlertDialog.Builder(a).setTitle("이 GPS 원본을 복구함으로 옮길까요?")
   .setMessage(RideHistoryLabel.title(track,java.time.ZoneId.systemDefault())+
    "\n검증된 ZIP에 동일 GPS가 포함된 경우에만 기간 제한 없이 복구함에 보관합니다. 자동 삭제하지 않으며, 앱을 삭제하면 복구함도 삭제됩니다.")
   .setNegativeButton("취소",null)
   .setPositiveButton("복구함 이동 확인",(d,w)->new AlertDialog.Builder(a)
    .setTitle("GPS 복구함 이동 · 최종 확인")
    .setMessage("원본 GPS를 앱 내부 복구함으로 옮겨 라이딩 내역에서 숨깁니다. 나중에 직접 복원할 수 있습니다.")
    .setNegativeButton("돌아가기",null)
    .setPositiveButton("선택한 파일 보관",(dd,ww)->removeInBackground(a,track)).show()).show();
 }
 private static void removeInBackground(MainActivity a,File track){
  new Thread(()->{
   boolean held=false;
   try{
    if(!RestoreRideGate.begin(RideService.running,RideService.autoArmed))
     throw new IOException("GPS 기록·백업·복원이 진행 중입니다.");
    held=true;
    File rides=TrackFile.directory(a.getFilesDir());
    String filename=track.getName();
    if(!filename.matches(BackupFormat.TRACK)||!rides.getCanonicalFile().equals(track.getCanonicalFile().getParentFile()))
     throw new IOException("GPS 경로 검증 실패");
    if(RideService.running||RideService.autoArmed||a.readOnly)
     throw new IOException("기록 또는 보호 상태가 변경돼 보관을 취소했습니다.");
    BackupGpsProof.requireExact(a,track);
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
    RideTrash.move(a.getFilesDir(),track);
    RideCompletionInbox.remove(a,track.getName());
    a.runOnUiThread(()->{if(!a.isFinishing()&&!a.isDestroyed()){a.toast("GPS 원본을 복구함으로 옮겼습니다.");a.render();}});
   }catch(Exception error){
    a.runOnUiThread(()->{if(!a.isFinishing()&&!a.isDestroyed())a.error("GPS 보관 취소: "+error.getMessage());});
   }finally{if(held)RestoreRideGate.end();}
  },"moto-single-gps-delete").start();
 }
}
