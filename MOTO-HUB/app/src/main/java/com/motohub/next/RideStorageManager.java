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
 static void show(MainActivity a){show(a,"");}
 static void show(MainActivity a,String initialFilter){
  if(RestoreRideGate.blocked(RideService.running,RideService.autoArmed)){a.toast("GPS 기록·복원·백업 중에는 파일을 정리하지 않아요.");return;}
  a.toast("GPS 기록 확인 중…");
  new Thread(()->{
   try{
    List<File> tracks=TrackFile.tracks(a.getFilesDir());
    String[] labels=new String[tracks.size()];
    String[] indexed=new String[tracks.size()];
    long total=0;
    for(int i=0;i<tracks.size();i++){
     File f=tracks.get(i);
     total+=f.length();
     labels[i]=RideHistoryLabel.title(f,java.time.ZoneId.systemDefault())+
      " · "+String.format(Locale.KOREA,"%.2f MB",f.length()/1_000_000.0);
     indexed[i]=(labels[i]+" "+f.getName()).toLowerCase(Locale.KOREA);
    }
    final long bytes=total;
    a.runOnUiThread(()->{
     if(!a.isFinishing()&&!a.isDestroyed())showPage(a,tracks,labels,indexed,0,initialFilter,bytes);
    });
   }catch(Exception e){a.runOnUiThread(()->{if(!a.isFinishing()&&!a.isDestroyed())a.error("GPS 저장공간 점검 실패: "+e.getMessage());});}
  },"moto-gps-storage-list").start();
 }
 private static void showPage(MainActivity a,List<File> tracks,String[] labels,String[] indexed,int page,String filter,long bytes){
  if(a.isFinishing()||a.isDestroyed())return;
  List<Integer> matches=new ArrayList<>();
  String query=filter.trim().toLowerCase(Locale.KOREA);
  for(int i=0;i<indexed.length;i++)if(indexed[i].contains(query))matches.add(i);
  int from=Math.max(0,Math.min(page*30,Math.max(0,matches.size()-1)/30*30));
  int to=Math.min(from+30,matches.size());
  String[] items=new String[to-from+2];
  items[0]="기록 검색"+(query.isEmpty()?"":" · "+query);
  items[1]="GPS 복구함 보기";
  for(int i=from;i<to;i++)items[i-from+2]=labels[matches.get(i)];
  AlertDialog.Builder dialog=new AlertDialog.Builder(a)
   .setTitle("GPS 원본 · "+matches.size()+"건")
   .setMessage("약 "+String.format(Locale.KOREA,"%.1f MB",bytes/1_000_000.0)+
     " · "+(matches.isEmpty()?0:from+1)+"–"+to+" / "+matches.size()+"건\n자동 영구 삭제 없음 · 검증된 ZIP 백업 후 복구함 이동 가능")
   .setItems(items,(d,index)->{
    if(index==0){
     android.widget.EditText input=new android.widget.EditText(a);
     input.setSingleLine(true);input.setHint("예: 2026.10.29");input.setText(query);
     new AlertDialog.Builder(a).setTitle("GPS 날짜 검색")
      .setMessage("연·월·일 일부로 검색할 수 있습니다.")
      .setView(input).setNegativeButton("취소",null)
      .setPositiveButton("검색",(x,y)->showPage(a,tracks,labels,indexed,0,input.getText().toString(),bytes)).show();
    }else if(index==1)showTrash(a);
    else confirm(a,tracks.get(matches.get(from+index-2)));
   }).setNegativeButton("닫기",null);
  if(to<matches.size())dialog.setPositiveButton("다음 30건",(d,w)->showPage(a,tracks,labels,indexed,from/30+1,query,bytes));
  if(from>0)dialog.setNeutralButton("이전 30건",(d,w)->showPage(a,tracks,labels,indexed,from/30-1,query,bytes));
  dialog.show();
 }
 private static void showTrash(MainActivity a){
  new Thread(()->{
   try{
    List<File> entries=RideTrash.entries(a.getFilesDir());
    String[] labels=new String[entries.size()];
    for(int i=0;i<labels.length;i++)labels[i]=RideHistoryLabel.title(entries.get(i),java.time.ZoneId.systemDefault());
    a.runOnUiThread(()->{
     if(a.isFinishing()||a.isDestroyed())return;
     showTrashPage(a,entries,labels,0,"");
    });
   }catch(Exception e){a.runOnUiThread(()->{if(!a.isFinishing()&&!a.isDestroyed())a.error(e.getMessage());});}
  },"moto-gps-trash-list").start();
 }
 /** Paginated, searchable recovery without modifying any archived GPS data. */
 private static void showTrashPage(MainActivity a,List<File> entries,String[] labels,int requestedPage,String filter){
  if(a.isFinishing()||a.isDestroyed())return;
  String query=filter.trim().toLowerCase(Locale.KOREA);
  List<Integer> matches=new ArrayList<>();
  for(int i=0;i<labels.length;i++)
   if(labels[i].toLowerCase(Locale.KOREA).contains(query))matches.add(i);
  int page=Math.max(0,Math.min(requestedPage,Math.max(0,(matches.size()-1)/30)));
  int from=page*30,to=Math.min(from+30,matches.size());
  String[] visible=new String[to-from+1];
  visible[0]="복구함 검색"+(query.isEmpty()?"":" · "+query);
  for(int i=from;i<to;i++)visible[i-from+1]=labels[matches.get(i)];
  AlertDialog.Builder dialog=new AlertDialog.Builder(a)
   .setTitle("GPS 복구함 · "+matches.size()+"건 / 전체 "+entries.size()+"건")
   .setMessage(matches.isEmpty()?"일치하는 복구 기록이 없습니다. 검색을 변경하세요.":
    "자동 영구 삭제 없음 · "+(from+1)+"–"+to+" / "+matches.size()+"건 · 직접 선택해서 복원")
   .setItems(visible,(d,index)->{
    if(index==0){
     android.widget.EditText input=new android.widget.EditText(a);
     input.setSingleLine(true);input.setHint("날짜 일부 · 예: 2026.10");
     input.setText(query);
     new AlertDialog.Builder(a).setTitle("GPS 복구함 날짜 검색")
      .setView(input).setNegativeButton("취소",null)
      .setPositiveButton("검색",(dd,which)->showTrashPage(a,entries,labels,0,input.getText().toString()))
      .show();
     return;
    }
    // Filtered row indices are not the backing file indices.
    final File target=entries.get(matches.get(from+index-1));
    new AlertDialog.Builder(a).setTitle("GPS 기록 복원")
     .setMessage("선택한 원본과 연결 정보를 라이딩 기록으로 되돌릴까요?")
     .setNegativeButton("취소",null)
     .setPositiveButton("복원",(dd,w)->new Thread(()->{
      try{
       boolean allowed=RestoreRideGate.begin(RideService.running,RideService.autoArmed);
       if(!allowed||a.readOnly){
        if(allowed)RestoreRideGate.end();
        throw new IOException("GPS 기록·백업·복원 중에는 복원할 수 없습니다.");
       }
       File restored;
       try{restored=RideTrash.restore(a.getFilesDir(),target);}
       finally{RestoreRideGate.end();}
       RideCompletionInbox.record(a,restored);
       a.runOnUiThread(()->{if(!a.isFinishing()&&!a.isDestroyed()){
        a.toast("GPS 원본 복원 완료");a.render();
       }});
      }catch(Exception error){a.runOnUiThread(()->{if(!a.isFinishing()&&!a.isDestroyed())a.error("복원 실패: "+error.getMessage());});}
     },"moto-gps-trash-restore").start()).show();
   }).setNegativeButton("닫기",null);
  if(to<matches.size())dialog.setPositiveButton("다음 30건",(d,w)->showTrashPage(a,entries,labels,page+1,query));
  if(page>0)dialog.setNeutralButton("이전 30건",(d,w)->showTrashPage(a,entries,labels,page-1,query));
  dialog.show();
 }
 static void confirm(MainActivity a,File track){
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
    "\n검증된 ZIP에 동일 GPS가 포함된 경우에만 수동 복구함으로 이동합니다. 원본은 즉시 영구 삭제되지 않습니다.")
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
