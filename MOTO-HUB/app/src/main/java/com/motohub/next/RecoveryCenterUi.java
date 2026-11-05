package com.motohub.next;

import android.app.*;
import android.widget.*;
import java.io.File;
import java.util.*;
import org.json.JSONObject;

/** Restore-point review and full-device-transfer entry points, using the existing backup machinery. */
final class RecoveryCenterUi {
 private final MainActivity a;RecoveryCenterUi(MainActivity a){this.a=a;}
 void open(){
  LinearLayout page=a.page("기기 이전 · 복구 센터");a.label(page,"새 휴대폰에는 전체 ZIP을 옮겨 복원하세요. 아래 자동 복구 지점은 기록 JSON만 포함하며 GPS·사진 파일을 대신하지 않습니다.",14,a.muted,false);
  a.action(a.formFooter,"기기 이전 · ZIP 백업 도구",false,()->a.choose("기기 이전 · ZIP 백업 도구",new String[]{"전체 ZIP 만들기","전체 ZIP 가져오기","암호화 백업 만들기","암호화 백업 가져오기"},choice->{
   if("전체 ZIP 만들기".equals(choice))a.exportBundle();else if("전체 ZIP 가져오기".equals(choice))a.importBundle();else if("암호화 백업 만들기".equals(choice))a.exportEncryptedBundle();else a.importEncryptedBundle();
  }));
  a.label(page,"자동 복구 지점 · 편집 전 7개 / 변경 후 12개",18,a.fg,true);LinearLayout list=a.col();page.addView(list);a.label(list,"검증된 복구 지점 확인 중…",14,a.muted,false);a.show();Dialog owner=a.dialog;
  new Thread(()->{File[] points=LocalSafetyBackup.recoveryPoints(a);a.runOnUiThread(()->{if(!alive(owner))return;list.removeAllViews();if(points.length==0)a.label(list,LocalSafetyBackup.recoveryAvailabilityMessage(a),14,a.muted,false);for(File point:points){String stamp=java.time.Instant.ofEpochMilli(point.lastModified()).atZone(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));new HubScreens(a).line(list,stamp,"변경 내용 확인","",()->preview(point));}File rollback=new File(a.getFilesDir(),"before-import.json");if(LocalBackupFilePolicy.readable(rollback))a.action(list,"이전 복원 직전 기록과 비교 · 되돌리기",false,()->preview(rollback));});},"moto-recovery-list").start();
 }
 private boolean alive(Dialog owner){return !a.isFinishing()&&!a.isDestroyed()&&a.dialog==owner&&owner!=null&&owner.isShowing();}
 void preview(File point){
  String reviewed=a.db.toString();Dialog owner=a.dialog;a.toast("복구 지점과 현재 기록을 비교하는 중…");
  new Thread(()->{try{JSONObject incoming=LocalSafetyBackup.read(point),current=Store.parse(reviewed);Map<String,BackupChangeSet.Counts> diff=BackupChangeSet.compare(current,incoming);StringBuilder text=new StringBuilder("현재 → 선택한 복구 지점\n");Map<String,String> names=new LinkedHashMap<>();names.put("vehicles","바이크");names.put("services","정비");names.put("expenses","주유·비용");names.put("routes","코스");names.put("settings","설정·여행기");for(Map.Entry<String,BackupChangeSet.Counts> row:diff.entrySet())text.append(names.getOrDefault(row.getKey(),row.getKey())).append(" · ").append(row.getValue().text()).append('\n');text.append("\nJSON 기록만 교체합니다. GPS·사진 파일은 이 복구 지점에 포함되지 않습니다. 다음 확인에서도 취소할 수 있습니다.");
   a.runOnUiThread(()->{if(!alive(owner))return;new AlertDialog.Builder(a).setTitle("복구로 바뀌는 기록").setMessage(text.toString()).setNegativeButton("취소",null).setPositiveButton("복원 확인으로",(d,w)->{if(!reviewed.equals(a.db.toString())){a.error("비교한 뒤 기록이 바뀌었습니다. 다시 비교해 주세요.");return;}a.confirmImport(incoming,reviewed);}).show();});
  }catch(Exception invalid){a.runOnUiThread(()->{if(alive(owner))a.error("복구 지점을 읽지 못했습니다. 현재 기록과 원본은 변경하지 않았습니다.");});}},"moto-recovery-diff").start();
 }
}
