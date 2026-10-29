package com.motohub.next;

import android.app.AlertDialog;
import android.os.SystemClock;
import org.json.JSONObject;

/** Short-lived, transaction-guarded undo for the immediately prior local JSON save. */
final class UndoLastSave {
 private UndoLastSave(){}
 private static final long WINDOW_MS=120000;
 private static final class State {
  final String before,after,label; final long time;
  State(String before,String after,String label){
   this.before=before;this.after=after;this.label=label;
   this.time=SystemClock.elapsedRealtime();
  }
 }
 private static State last;
 static boolean available(MainActivity a){
  State state=last;
  return state!=null&&!a.readOnly
    &&SystemClock.elapsedRealtime()-state.time<WINDOW_MS
    &&a.db!=null&&state.after.equals(a.db.toString());
 }
 static void offer(MainActivity a,String before,String after,String label){offer(a,before,after,label,null);}
 static void offer(MainActivity a,String before,String after,String label,Runnable onConfirmed){
  if(before==null||after==null||before.equals(after))return;
  last=new State(before,after,label);
  new AlertDialog.Builder(a).setTitle(label+" 저장 완료")
   .setMessage("방금 저장한 기록을 되돌릴 수 있어요. 2분 이내에만 가능하며 다른 변경이 있으면 취소가 차단됩니다.")
   .setNegativeButton("확인",(d,w)->{if(onConfirmed!=null)onConfirmed.run();})
   .setPositiveButton("저장 취소",(d,w)->undo(a)).show();
 }
 static void undo(MainActivity a){
  State state=last;
  if(state==null||!available(a)){a.toast("저장 취소 기한이 지났거나 다른 기록이 변경됐어요.");return;}
  a.attempt(()->{
   if(!available(a))throw new IllegalStateException("기록이 변경되어 취소할 수 없습니다.");
   JSONObject restore=Store.parse(state.before);
   a.write(restore);
   last=null;DailyMaintenanceJob.checkSoon(a);a.render();
   a.toast("마지막 "+state.label+" 저장을 취소했어요.");
  });
 }
}