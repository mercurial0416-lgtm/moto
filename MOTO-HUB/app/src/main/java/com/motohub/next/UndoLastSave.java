package com.motohub.next;

import android.app.AlertDialog;
import android.os.SystemClock;
import org.json.JSONObject;

/** Short-lived, transaction-guarded undo for the immediately prior local JSON save. */
final class UndoLastSave {
 private UndoLastSave(){}
 private static final long WINDOW_MS=120000;
 private static final class State {
  final String before,after,label; final long time; final Runnable onUndone;
  State(String before,String after,String label,Runnable onUndone){
   this.before=before;this.after=after;this.label=label;this.onUndone=onUndone;
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
  last=new State(before,after,label,null);
  a.toast(label+" 저장 완료 · 홈의 실행 취소에서 2분 내 복원 가능");
  if(onConfirmed!=null)onConfirmed.run();
 }
 /** Post-rollback UI restoration runs only when database rollback actually succeeds. */
 static void offerOnUndo(MainActivity a,String before,String after,String label,Runnable onUndone){
  if(before==null||after==null||before.equals(after))return;
  last=new State(before,after,label,onUndone);
  a.toast(label+" 저장 완료 · 홈이나 기록에서 2분 내 실행 취소 가능");
 }
 static void undo(MainActivity a){
  State state=last;
  if(state==null||!available(a)){a.toast("저장 취소 기한이 지났거나 다른 기록이 변경됐어요.");return;}
  a.attempt(()->{
   if(!available(a))throw new IllegalStateException("기록이 변경되어 취소할 수 없습니다.");
   JSONObject restore=Store.parse(state.before);
   a.write(restore);
   last=null;DailyMaintenanceJob.checkSoon(a);a.render();
   if(state.onUndone!=null)state.onUndone.run();
   a.toast("마지막 "+state.label+" 저장을 취소했어요.");
  });
 }
}