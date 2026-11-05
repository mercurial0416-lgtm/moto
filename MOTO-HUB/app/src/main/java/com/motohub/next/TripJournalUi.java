package com.motohub.next;

import android.app.AlertDialog;
import android.content.Intent;
import android.view.View;
import android.widget.*;
import java.io.File;
import org.json.*;

/** A route, existing cover photo, title/memo and explicitly authored stops in one task flow. */
final class TripJournalUi {
 private final MainActivity a;private File track;private RideInsights.Result result;private android.app.Dialog owner;
 TripJournalUi(MainActivity a){this.a=a;}
 void open(File track,RideInsights.Result result){
  if(!RideMileageIntegrity.validCompleted(track)){a.toast("완료된 라이딩을 선택하세요.");return;}this.track=track;this.result=result;
  LinearLayout page=a.page("라이딩 여행기");String title=RideNotes.title(a.db,track);a.label(page,title.isEmpty()?"나의 라이딩":title,22,a.fg,true);a.label(page,result.distanceLabel(),18,a.fg,true);a.label(page,RideNotes.memo(a.db,track),14,a.muted,false);
  a.action(page,"경로와 방문 지점 보기",true,()->map(false));
  a.label(page,"직접 기록한 휴식 · 주유 · 방문 지점",18,a.fg,true);JSONArray stops=TripNotes.stops(a.db,track);if(stops.length()==0)a.label(page,"아직 기록한 지점이 없습니다. 자동 감지한 정차 기록이 아닌 직접 등록하는 일지입니다.",14,a.muted,false);
  for(int i=0;i<Math.min(stops.length(),TripNotes.MAX_STOPS);i++){JSONObject item=stops.optJSONObject(i);if(item==null)continue;String location=item.has("latitude")&&item.has("longitude")?"지도 위치 직접 등록됨":"위치 없음";new HubScreens(a).line(page,item.optString("name"),item.optString("memo"),location,()->stopActions(item));}
  a.action(a.formFooter,"여행기 관리",false,this::manage);
  a.show();owner=a.dialog;
 }
 private void manage(){
  a.choose("여행기 관리",new String[]{"제목 · 메모 수정","사진 · 공유 카드","지도에서 방문 지점 선택","지점 이름 · 메모 직접 추가"},choice->{
   if("제목 · 메모 수정".equals(choice))a.editRideNote(track);
   else if("사진 · 공유 카드".equals(choice))new RidePhotoUi(a).open(track,result);
   else if("지도에서 방문 지점 선택".equals(choice))map(true);
   else editStop(null,null);
  });
 }
 private void stopActions(JSONObject item){
  new AlertDialog.Builder(a).setTitle(item.optString("name")).setMessage(item.optString("memo")).setNegativeButton("닫기",null).setNeutralButton("경로 보기",(d,w)->map(false)).setPositiveButton("삭제",(menu,index)->{
   new AlertDialog.Builder(a).setTitle("이 방문 지점만 삭제할까요?").setMessage("GPS 원본과 사진, 다른 지점은 유지됩니다.").setNegativeButton("취소",null).setPositiveButton("삭제",(d,w)->a.attempt(()->{String before=a.db.toString();JSONObject next=new JSONObject(before);TripNotes.remove(next,track,item.getString("id"));a.commitFeatureRecordAsync(next,before,"여행 지점 삭제",()->open(track,result));})).show();
  }).show();
 }
 private void map(boolean choose){
  if(!RideMileageIntegrity.validCompleted(track)){a.toast("라이딩 원본이 변경됐습니다.");return;}Intent intent=new Intent(a,RideMapActivity.class).putExtra("track",track.getName()).putExtra("trip_stops",TripNotes.mapStops(a.db,track).toString());
  if(choose){a.pendingTripJournal=this;intent.putExtra("choose_trip_stop",true);a.startActivityForResult(intent,95);}else a.startActivity(intent);
 }
 void receive(Intent data){if(a.dialog!=owner||owner==null||!owner.isShowing()||a.readOnly)return;if(track==null||data==null||!data.hasExtra("latitude")||!data.hasExtra("longitude"))return;double lat=data.getDoubleExtra("latitude",Double.NaN),lon=data.getDoubleExtra("longitude",Double.NaN);if(!Double.isFinite(lat)||!Double.isFinite(lon)||Math.abs(lat)>90||Math.abs(lon)>180){a.error("지도 위치를 확인하지 못했습니다.");return;}editStop(lat,lon);}
 void editStop(Double latitude,Double longitude){
  if(!RideMileageIntegrity.validCompleted(track)){a.toast("라이딩 원본이 변경됐습니다.");return;}String snapshot=a.db.toString();LinearLayout page=a.page("여행 지점 추가");EditText name=FeatureUi.input(a,page,"지점 이름","",false),memo=FeatureUi.input(a,page,"지점 메모","",true);Spinner type=new Spinner(a);type.setAdapter(new ArrayAdapter<>(a,android.R.layout.simple_spinner_dropdown_item,new String[]{"휴식","주유","방문","메모"}));type.setContentDescription("지점 종류");page.addView(type);if(latitude!=null)a.label(page,"지도에서 직접 선택한 위치입니다. GPS 경로의 정차를 자동 인식한 결과가 아닙니다.",13,a.muted,false);
  a.action(page,"저장",true,()->a.attempt(()->{if(a.readOnly||!snapshot.equals(a.db.toString()))throw new Exception("기록이 바뀌었습니다. 다시 열어 주세요.");JSONObject stop=TripNotes.stop(name.getText().toString(),memo.getText().toString(),new String[]{"rest","fuel","visit","note"}[type.getSelectedItemPosition()],latitude,longitude);JSONObject next=new JSONObject(snapshot);TripNotes.append(next,track,stop);a.commitFeatureRecordAsync(next,snapshot,"여행 지점",()->open(track,result));}));a.show();
 }
}
