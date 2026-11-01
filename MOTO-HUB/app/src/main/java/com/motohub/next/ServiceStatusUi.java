package com.motohub.next;

import android.app.AlertDialog;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import org.json.JSONObject;
import java.util.*;

/** Relevant reminders only; completion records are normal editable service records. */
final class ServiceStatusUi {
 private final MainActivity a;
 ServiceStatusUi(MainActivity a){this.a=a;}
 void render(LinearLayout target){
  JSONObject bike=a.bike();if(bike==null||GarageBikeStatus.sold(bike.optString("soldDate")))return;
  List<JSONObject> entries=new ArrayList<>();
  for(JSONObject service:LatestServiceByType.latest(a.items("services"),bike.optString("id")).values()){
   if(service.optDouble("nextKm")>0||!service.optString("nextDate").isEmpty())entries.add(service);
  }
  entries.sort((x,y)->Integer.compare(MaintenancePlanner.order(status(x,bike)),MaintenancePlanner.order(status(y,bike))));
  LinearLayout heading=new LinearLayout(a);heading.setGravity(Gravity.CENTER_VERTICAL);
  heading.addView(a.text("내 정비주기",18,a.fg,true),new LinearLayout.LayoutParams(0,-2,1));
  Button odo=a.button("계기판 갱신",false,()->updateOdo(bike));odo.setTextSize(12);
  odo.setMinHeight(a.dp(42));heading.addView(odo);target.addView(heading);
  a.gap(target,9);
  if(entries.isEmpty()){
   TextView blank=a.text("등록된 정비주기가 없어요. 엔진오일·체인 등 필요한 항목부터 등록하세요.",13,a.muted,false);
   blank.setPadding(a.dp(17),a.dp(15),a.dp(17),a.dp(15));blank.setBackground(a.shape(a.panel,a.line,18));target.addView(blank);
   a.action(target,"정비주기 등록",false,()->new QuickEntryUi(a).maintenance());return;
  }
  int shown=0;
  for(JSONObject item:entries){
   if(shown++>=8)break;
   MaintenancePlanner.Status status=status(item,bike);
   LinearLayout row=new LinearLayout(a);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(a.dp(15),a.dp(13),a.dp(11),a.dp(13));row.setBackground(a.shape(a.panel,a.line,17));
   LinearLayout info=a.col();
   String state=status==MaintenancePlanner.Status.OVERDUE?"점검 필요":status==MaintenancePlanner.Status.SOON?"곧 정비":"정비 예정";
   int tone=status==MaintenancePlanner.Status.OVERDUE?0xFFE28370:status==MaintenancePlanner.Status.SOON?a.accent:a.fg;
   info.addView(a.text(item.optString("type")+" · "+state,15,tone,true));
   String detail=MaintenanceNoticeText.detail(item.optString("nextDate"),item.optDouble("nextKm"),a.today(),bike.optDouble("odo"));
   TextView sub=a.text(detail.isEmpty()?"다음 주기 설정됨":detail,12,a.muted,false);
   LinearLayout.LayoutParams sl=new LinearLayout.LayoutParams(-1,-2);sl.topMargin=a.dp(5);info.addView(sub,sl);
   row.addView(info,new LinearLayout.LayoutParams(0,-2,1));
   Button complete=a.button("완료",false,()->complete(item));complete.setMinHeight(a.dp(44));
   row.addView(complete);
   LinearLayout.LayoutParams rowParams=new LinearLayout.LayoutParams(-1,-2);rowParams.bottomMargin=a.dp(8);target.addView(row,rowParams);
  }
  a.gap(target,9);
  a.label(target,"정비 알림은 저장된 계기판과 예정일을 기준으로 계산합니다. GPS 거리만으로 계기판을 자동 변경하지 않습니다.",12,a.muted,false);
 }
 MaintenancePlanner.Status status(JSONObject item,JSONObject bike){
  return MaintenanceAlertWindow.status(item,a.today(),bike.optDouble("odo"));
 }
 void updateOdo(JSONObject bike){
  EditText field=new EditText(a);
  field.setSingleLine(true);field.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
  field.setText(String.valueOf((long)bike.optDouble("odo")));field.selectAll();field.setPadding(a.dp(18),a.dp(15),a.dp(18),a.dp(15));
  new AlertDialog.Builder(a).setTitle("현재 계기판 거리")
   .setMessage("실제 계기판 숫자를 입력하면 남은 정비 거리와 알림 시점을 다시 계산해요.")
   .setView(field).setNegativeButton("취소",null)
   .setPositiveButton("저장",(d,w)->a.attempt(()->{
    double km=Double.parseDouble(field.getText().toString().trim());
    if(!Double.isFinite(km)||km<bike.optDouble("odo")||km>1e9)
     throw new IllegalArgumentException("현재 계기판과 같거나 큰 주행거리를 입력해 주세요.");
    JSONObject next=new JSONObject(a.db.toString());
    Store.find(next,"vehicles",bike.optString("id")).put("odo",km);
    a.write(next);DailyMaintenanceJob.checkSoon(a);a.render();
    a.toast("정비 잔여 거리를 다시 계산했어요.");
   })).show();
 }
 void complete(JSONObject old){
  JSONObject bike=a.bike();if(bike==null)return;
  LinearLayout body=a.page(old.optString("type")+" 정비 완료");
  a.label(body,"정비를 실제로 완료한 경우에만 저장하세요. 선택한 주기를 기준으로 다음 일정을 계산합니다.",13,a.muted,false);a.gap(body,15);
  a.label(body,"정비 시 계기판 (km)",14,a.fg,true);
  EditText mileage=new EditText(a);mileage.setSingleLine(true);
  mileage.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
  mileage.setText(String.valueOf((long)bike.optDouble("odo")));mileage.setTextColor(a.fg);
  body.addView(mileage,new LinearLayout.LayoutParams(-1,a.dp(54)));
  a.gap(body,12);
  a.label(body,"정비 비용 (원) · 없으면 0",14,a.fg,true);
  EditText cost=new EditText(a);cost.setSingleLine(true);cost.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
  cost.setText("0");cost.setTextColor(a.fg);body.addView(cost,new LinearLayout.LayoutParams(-1,a.dp(54)));
  a.gap(body,11);
  int periodKm=old.optInt("intervalKm",0),periodMonths=old.optInt("intervalMonths",0);
  if(periodKm==0&&periodMonths==0)a.label(body,"현재 기록에 반복 주기가 없어 완료 후에는 별도로 다음 정비주기를 설정해야 합니다.",13,a.muted,false);
  else a.label(body,"다음 정비: "+(periodKm>0?"+"+a.num(periodKm)+" km ":"")+(periodMonths>0?"· "+periodMonths+"개월 뒤":""),13,a.muted,false);
  a.action(body,"정비 완료 저장",true,()->a.attempt(()->{
   double current=Double.parseDouble(mileage.getText().toString().trim()),spent=Double.parseDouble(cost.getText().toString().trim());
   if(current<bike.optDouble("odo"))throw new IllegalArgumentException("현재보다 적은 주행거리로는 완료할 수 없어요.");
   JSONObject done=ServiceCycle.completed(old,a.today(),current,spent);
   Runnable save=()->a.attempt(()->{
    JSONObject next=new JSONObject(a.db.toString());
    Store.upsert(next,"services",done);
    JSONObject updated=Store.find(next,"vehicles",bike.optString("id"));
    if(updated!=null&&current>updated.optDouble("odo"))updated.put("odo",current);
    a.write(next);DailyMaintenanceJob.checkSoon(a);a.close();a.render();
    a.toast("완료 기록을 저장하고 다음 정비주기를 계산했어요.");
   });
   boolean duplicate=false;
   for(JSONObject x:a.items("services"))if(!x.optBoolean("scheduleOnly")
    &&QuickLogDuplicate.same(x,done,"services")){duplicate=true;break;}
   if(duplicate)new AlertDialog.Builder(a).setTitle("이미 완료한 정비로 보여요")
    .setMessage("같은 날짜·주행거리·비용으로 저장한 완료 기록이 있습니다.")
    .setNegativeButton("취소",null).setPositiveButton("그래도 저장",(d,w)->save.run()).show();
   else save.run();
  }));
  a.show();
 }
}
