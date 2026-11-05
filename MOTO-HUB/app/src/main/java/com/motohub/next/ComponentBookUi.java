package com.motohub.next;

import android.app.Dialog;
import android.widget.*;
import java.time.LocalDate;
import java.util.*;
import org.json.*;

final class ComponentBookUi {
 private final MainActivity a;
 ComponentBookUi(MainActivity a){this.a=a;}
 void open(){
  JSONObject bike=a.bike();if(bike==null){a.toast("바이크를 먼저 등록하세요.");return;}String id=bike.optString("id"),snapshot=a.db.toString();double odo=bike.optDouble("odo");
  LinearLayout page=a.page("부품별 관리 장부");a.label(page,bike.optString("name"),18,a.fg,true);EditText search=FeatureUi.input(a,page,"부품 이름 검색","",false);LinearLayout results=a.col();page.addView(results);a.label(results,"정비 이력을 읽는 중…",14,a.muted,false);a.show();Dialog owner=a.dialog;
  new Thread(()->{
   try{JSONObject stable=Store.parse(snapshot);List<JSONObject> rows=new ArrayList<>();JSONArray array=stable.getJSONArray("services");for(int i=0;i<array.length();i++)rows.add(array.getJSONObject(i));List<ComponentHistory.Component> parts=ComponentHistory.inspect(rows,id,LocalDate.parse(a.today()));
    a.runOnUiThread(()->{if(!alive(owner,id))return;final int[] limit={30};Runnable draw=()->{results.removeAllViews();String query=search.getText().toString().trim().toLowerCase(Locale.ROOT);int count=0;for(ComponentHistory.Component part:parts){if(!part.type.toLowerCase(Locale.ROOT).contains(query))continue;if(++count>limit[0])continue;JSONObject last=part.last();double used=part.usedKm(odo);String detail=last==null?"실제 교체 기록 없음":last.optString("date")+" · 교체 시 "+a.num(last.optDouble("mileage"))+" km"+(Double.isFinite(used)?" · 이후 "+a.num(used)+" km":"");a.action(results,part.type+" · "+part.history.size()+"회\n"+detail,false,()->history(part,id));}if(count==0)a.label(results,"해당 부품의 기록이 없습니다.",14,a.muted,false);if(count>limit[0])a.action(results,"부품 더 보기",false,()->{limit[0]+=30;redraw(search);});};search.setTag(draw);search.addTextChangedListener(EntryAssist.watch(()->{limit[0]=30;draw.run();}));draw.run();});
   }catch(Exception error){a.runOnUiThread(()->{if(alive(owner,id)){results.removeAllViews();a.label(results,"기록을 읽지 못했습니다. 원본은 보존했습니다.",14,a.muted,false);}});}
  },"moto-component-ledger").start();
 }
 private void redraw(EditText search){Object action=search.getTag();if(action instanceof Runnable)((Runnable)action).run();}
 private boolean alive(Dialog owner,String bike){return !a.isFinishing()&&!a.isDestroyed()&&a.dialog==owner&&owner.isShowing()&&a.bike()!=null&&bike.equals(a.bike().optString("id"));}
 void history(ComponentHistory.Component part,String bikeId){
  if(a.bike()==null||!bikeId.equals(a.bike().optString("id"))){a.toast("바이크가 바뀌었습니다. 다시 열어 주세요.");return;}
  LinearLayout page=a.page(part.type+" · 교체 이력");a.label(page,"실제 완료한 정비만 교체 이력으로 표시합니다. 예정 기록은 교체 횟수에 넣지 않습니다.",14,a.muted,false);LinearLayout rows=a.col();page.addView(rows);final int[] shown={0};Button more=a.button("이력 더 보기",false,()->{});
  Runnable next=()->{int end=Math.min(part.history.size(),shown[0]+20);while(shown[0]<end){JSONObject record=part.history.get(shown[0]++);JSONObject meta=record.optJSONObject("componentDetails");String details=record.optString("date")+" · "+a.num(record.optDouble("mileage"))+" km · "+a.won(record.optDouble("cost"));if(meta!=null)details+="\n"+meta.optString("product")+" · "+meta.optString("spec")+" · "+meta.optString("shop");a.action(rows,details,false,()->editDetails(record,bikeId));}more.setVisibility(shown[0]<part.history.size()?android.view.View.VISIBLE:android.view.View.GONE);};more.setOnClickListener(v->next.run());page.addView(more);next.run();if(part.history.isEmpty())a.label(page,"아직 교체 완료 이력이 없습니다.",14,a.muted,false);a.action(page,"이 부품 정비주기 설정",false,()->new QuickEntryUi(a).maintenance(part.type));a.show();
 }
 void editDetails(JSONObject record,String bikeId){
  final String original=record.toString();
  LinearLayout page=a.page(record.optString("type")+" · 제품 정보");JSONObject meta=record.optJSONObject("componentDetails");if(meta==null)meta=new JSONObject();
  EditText product=FeatureUi.input(a,page,"제품명",meta.optString("product"),false),spec=FeatureUi.input(a,page,"규격",meta.optString("spec"),false),shop=FeatureUi.input(a,page,"정비소",meta.optString("shop"),false),memo=FeatureUi.input(a,page,"부품 메모",meta.optString("memo"),true);
  a.action(page,"사진 · 영수증 관리",false,()->a.receiptOptions("services",record));
  a.action(page,"저장",true,()->a.attempt(()->{if(a.readOnly||a.bike()==null||!bikeId.equals(a.bike().optString("id")))throw new Exception("바이크 또는 저장 상태가 바뀌었습니다.");JSONObject current=Store.find(a.db,"services",record.optString("id"));if(current==null||!bikeId.equals(current.optString("vehicleId"))||!BackupChangeSet.same(current,new JSONObject(original)))throw new Exception("정비 기록이 변경되었습니다. 다시 열어 주세요.");String before=a.db.toString();JSONObject next=new JSONObject(before);Store.upsert(next,"services",ComponentHistory.annotate(current,product.getText().toString(),spec.getText().toString(),shop.getText().toString(),memo.getText().toString()));a.commitFeatureRecordAsync(next,before,"부품 정보",this::open);}));a.show();
 }
}
