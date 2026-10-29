package com.motohub.next;
import android.app.AlertDialog;import android.graphics.Color;import android.view.*;import android.widget.*;import org.json.*;import java.util.*;
/** Screen composition only. Existing Store, forms and AtomicFile persistence are reused. */
final class HubScreens {
 // Content/schema validation of up to 19 local backups must not freeze settings UI.
 private static final java.util.concurrent.ExecutorService LOCAL_BACKUP_IO=java.util.concurrent.Executors.newSingleThreadExecutor(r->{
  Thread t=new Thread(r,"moto-local-backup-io");t.setDaemon(true);return t;
 });
 final MainActivity a;
 final Map<JSONObject,String> visibleLedgerCollections=new IdentityHashMap<>();
 static final class GpsSearchRow {
  final String title,filename,bikeId,date,index;
  GpsSearchRow(String title,String filename,String bikeId){
   this.title=title;this.filename=filename;this.bikeId=bikeId;
   this.date=title.length()>=10&&title.charAt(4)=='.'?title.substring(0,10).replace('.','-'):"";
   this.index=(title+" "+filename).toLowerCase(Locale.KOREA);
  }
 }
 static final class LedgerSearchRow {
  final JSONObject item;final String collection,type,detail,amount,search;
  LedgerSearchRow(JSONObject item,String collection,String type,String detail,String amount,String search){
   this.item=item;this.collection=collection;this.type=type;this.detail=detail;this.amount=amount;this.search=search;
  }
 }
 HubScreens(MainActivity activity){a=activity;}
 void title(String name,String detail){a.heading(name,detail);}
 void go(String tab){a.navigate(tab);}
 void empty(String title,String description,String action,Runnable click){a.gap(a.content,16);a.label(a.content,title,22,a.fg,true);a.label(a.content,description,16,a.muted,false);a.gap(a.content,12);a.action(a.content,action,true,click);}
 void line(String title,String detail,String value,Runnable click){line(a.content,title,detail,value,click);}
 void line(LinearLayout target,String title,String detail,String value,Runnable click){
  line(target,title,detail,value,click,null,false);
 }
 void line(LinearLayout target,String title,String detail,String value,Runnable click,Runnable favorite,boolean starred){
  LinearLayout block=a.col();block.setPadding(a.dp(4),a.dp(14),a.dp(4),a.dp(14));block.setMinimumHeight(a.dp(70));
  LinearLayout head=new LinearLayout(a);head.setGravity(Gravity.CENTER_VERTICAL);
  boolean largeText=a.getResources().getConfiguration().fontScale>=1.35f;
  if(largeText)head.setOrientation(LinearLayout.VERTICAL);
  TextView label=a.text(title,16,a.fg,true);label.setMaxLines(2);label.setEllipsize(android.text.TextUtils.TruncateAt.END);
  head.addView(label,largeText?new LinearLayout.LayoutParams(-1,-2):new LinearLayout.LayoutParams(0,-2,1));
  if(value!=null&&!value.isEmpty()){
   boolean monetary=value.endsWith("원");
   TextView hint=monetary?MotoDesign.figure(a,value,16,a.fg):a.text(value,13,a.muted,true);
   hint.setGravity(Gravity.END|Gravity.CENTER_VERTICAL);
   hint.setPadding(a.dp(10),0,0,0);hint.setMaxLines(2);
   hint.setEllipsize(android.text.TextUtils.TruncateAt.END);
   head.addView(hint,largeText?new LinearLayout.LayoutParams(-1,-2):new LinearLayout.LayoutParams(-2,-2));
  }
  if(favorite!=null){
   Button star=a.button(starred?"★":"☆",false,favorite);
   star.setContentDescription(starred?"라이딩 즐겨찾기 해제":"라이딩 즐겨찾기");
   star.setMinHeight(a.dp(44));star.setMinimumWidth(a.dp(44));
   star.setTextSize(20);star.setPadding(0,0,0,0);
   LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(a.dp(46),a.dp(44));
   sp.leftMargin=a.dp(6);head.addView(star,sp);
  }
  block.addView(head);
  if(detail!=null&&!detail.isEmpty()){
   TextView summary=a.text(detail,13,a.muted,false);summary.setMaxLines(3);
   LinearLayout.LayoutParams sl=new LinearLayout.LayoutParams(-1,-2);sl.topMargin=a.dp(7);block.addView(summary,sl);
  }
  if(click!=null){
   block.setFocusable(true);block.setClickable(true);
   MotoDesign.ripple(a,block,a.bg,0,8);
   block.setOnClickListener(v->click.run());
   block.setContentDescription(title+", "+detail+", "+value);
  }
  target.addView(block);
  View rule=new View(a);rule.setBackgroundColor(a.line);
  target.addView(rule,new LinearLayout.LayoutParams(-1,a.dp(1)));
 }
 void bikePicker(){JSONObject b=a.bike();if(b==null)return;Button picker=a.button(b.optString("name")+"  ▾",false,this::chooseBike);picker.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);a.content.addView(picker,new LinearLayout.LayoutParams(-1,-2));a.gap(a.content,12);}
 void chooseBike(){List<JSONObject> list=a.items("vehicles");list.sort((x,y)->Integer.compare(GarageBikeStatus.rank(x.optString("soldDate")),GarageBikeStatus.rank(y.optString("soldDate"))));String[] options=new String[list.size()+1];for(int i=0;i<list.size();i++)options[i]=GarageBikeStatus.label(list.get(i).optString("name"),list.get(i).optString("soldDate"));options[list.size()]="＋ 바이크 추가";new AlertDialog.Builder(a).setTitle("내 바이크").setItems(options,(d,n)->{if(n==list.size()){a.edit("vehicles",null);return;}a.selected=list.get(n).optString("id");a.getSharedPreferences("garage_ui",0).edit().putString("selected_bike",a.selected).apply();a.render();}).show();}
 // Signature screen: one strong garage hero, then one-tap actions and clear records.
 void home(){new GarageHomeUi(a).render();}
 LinearLayout quickTile(String name,String detail,Runnable click){
  LinearLayout tile=a.col();tile.setGravity(Gravity.CENTER_VERTICAL);tile.setPadding(a.dp(16),a.dp(14),a.dp(14),a.dp(14));tile.setMinimumHeight(a.dp(78));tile.setBackground(a.shape(a.panel,a.line,12));
  TextView t=a.text(name,15,a.fg,true);tile.addView(t);TextView d=a.text(detail,12,a.muted,false);LinearLayout.LayoutParams dp=new LinearLayout.LayoutParams(-1,-2);dp.topMargin=a.dp(9);tile.addView(d,dp);
  tile.setClickable(true);tile.setFocusable(true);tile.setContentDescription(name+" "+detail);tile.setOnClickListener(v->click.run());return tile;
 }
 List<JSONObject> records(String mode,JSONObject bike){
  List<JSONObject> result=new ArrayList<>();
  visibleLedgerCollections.clear();String bikeId=bike.optString("id");
  for(String key:new String[]{"services","expenses"})for(JSONObject record:a.items(key)){
   if(!bikeId.equals(record.optString("vehicleId")))continue;
   boolean service=key.equals("services"),fuel="주유".equals(record.optString("type"));
   if(mode.equals("정비")&&!service||mode.equals("주유")&&!fuel||mode.equals("기타")&&(service||fuel))continue;
   result.add(record);visibleLedgerCollections.put(record,key);
  }
  result.sort((x,y)->{int date=y.optString("date").compareTo(x.optString("date"));
   return date!=0?date:Double.compare(y.optDouble("mileage"),x.optDouble("mileage"));});
  return result;
 }
 /** Search every saved garage ledger on this phone, irrespective of the selected bike.
  * Render at most 30 matching rows per page to keep the dialog usable on older devices. */
 void searchAllRecords(){
  List<JSONObject> all=new ArrayList<>();
  Map<JSONObject,String> collection=new IdentityHashMap<>();
  for(JSONObject record:a.items("services")){all.add(record);collection.put(record,"services");}
  for(JSONObject record:a.items("expenses")){all.add(record);collection.put(record,"expenses");}
  all.sort((x,y)->y.optString("date").compareTo(x.optString("date")));
  Map<String,String> bikeNames=new HashMap<>();
  for(JSONObject vehicle:a.items("vehicles"))
   bikeNames.put(vehicle.optString("id"),vehicle.optString("name"));
  // Index text once. Fast typing must not format 10,000 JSON records every keystroke.
  List<LedgerSearchRow> indexed=new ArrayList<>(all.size());
  for(JSONObject item:all){
   String key=collection.get(item);
   String type=(key.equals("services")?"정비":"주유".equals(item.optString("type"))?"주유":"비용")+" · "+item.optString("type","기타");
   String name=bikeNames.getOrDefault(item.optString("vehicleId"),"등록되지 않은 바이크");
   String detail=name+" · "+item.optString("date")+" · "+a.num(item.optDouble("mileage"))+" km";
   String note=item.optString("memo")+" "+item.optString("note")+" "+item.optString("description");
   String amount=a.won(item.optDouble(key.equals("expenses")?"amount":"cost"));
   indexed.add(new LedgerSearchRow(item,key,type,detail,amount,
    (type+" "+detail+" "+note+" "+amount).toLowerCase(Locale.KOREA)));
  }
  LinearLayout body=a.col();body.setPadding(a.dp(16),a.dp(6),a.dp(16),a.dp(8));
  EditText input=new EditText(a);input.setSingleLine(true);input.setHint("바이크 · 주유 · 정비 · 라이딩 · 날짜");
  input.setTextColor(a.fg);input.setHintTextColor(a.muted);
  input.setContentDescription("모든 바이크 기록 검색어");
  body.addView(input,new LinearLayout.LayoutParams(-1,a.dp(52)));
  TextView count=a.text("기록을 불러오는 중",12,a.muted,false);body.addView(count);
  final int[] page={0};final Runnable[] redraw={null};
  final String[] kind={"전체"},bikeScope={RideHistoryBikeFilter.ALL},since={""},until={""};
  final List<JSONObject> motorcycles=a.items("vehicles");
  final Button[] filterControl={null};
  filterControl[0]=a.button("필터 · 전체 / 전체 바이크 / 전체 기간",false,()->{
   String[] options={"기록 종류 · "+kind[0],"바이크 · "+(RideHistoryBikeFilter.ALL.equals(bikeScope[0])?"전체":bikeNames.getOrDefault(bikeScope[0],"바이크")),
    "날짜 범위 · "+(since[0].isEmpty()?"전체":since[0]+" ~ "+(until[0].isEmpty()?"오늘":until[0])),"필터 초기화"};
   new AlertDialog.Builder(a).setTitle("기록 검색 필터").setItems(options,(d,choice)->{
    if(choice==0){
     String[] kinds={"전체","주유","정비","비용","GPS"};
     new AlertDialog.Builder(a).setTitle("기록 종류").setItems(kinds,(dd,index)->{
      kind[0]=kinds[index];page[0]=0;redraw[0].run();
     }).show();
    }else if(choice==1){
     String[] names=new String[motorcycles.size()+1];
     names[0]="전체 바이크";
     for(int i=0;i<motorcycles.size();i++)names[i+1]=motorcycles.get(i).optString("name");
     new AlertDialog.Builder(a).setTitle("바이크 선택").setItems(names,(dd,index)->{
      bikeScope[0]=index==0?RideHistoryBikeFilter.ALL:motorcycles.get(index-1).optString("id");
      page[0]=0;redraw[0].run();
     }).show();
    }else if(choice==2){
     LinearLayout dates=a.col();dates.setPadding(a.dp(18),a.dp(8),a.dp(18),a.dp(4));
     EditText from=new EditText(a);from.setSingleLine(true);from.setHint("시작일 YYYY-MM-DD");
     from.setText(since[0]);dates.addView(from);
     EditText to=new EditText(a);to.setSingleLine(true);to.setHint("종료일 YYYY-MM-DD");
     to.setText(until[0]);dates.addView(to);
     new AlertDialog.Builder(a).setTitle("날짜 범위 · 비우면 제한 없음").setView(dates)
      .setNegativeButton("취소",null).setPositiveButton("적용",(dd,w)->{
       String start=from.getText().toString().trim(),end=to.getText().toString().trim();
       try{
        if(!start.isEmpty())Store.validateDate(start);
        if(!end.isEmpty())Store.validateDate(end);
        if(!start.isEmpty()&&!end.isEmpty()&&start.compareTo(end)>0)
         throw new IllegalArgumentException("시작일이 종료일보다 늦어요.");
       }catch(Exception bad){a.toast("날짜 형식을 확인해 주세요 (YYYY-MM-DD).");return;}
       since[0]=start;until[0]=end;page[0]=0;redraw[0].run();
      }).show();
    }else{kind[0]="전체";bikeScope[0]=RideHistoryBikeFilter.ALL;since[0]="";until[0]="";page[0]=0;redraw[0].run();}
   }).show();
  });
  body.addView(filterControl[0]);
  ScrollView scroll=new ScrollView(a);LinearLayout results=a.col();scroll.addView(results);
  body.addView(scroll,new LinearLayout.LayoutParams(-1,a.dp(340)));
  AlertDialog dialog=new AlertDialog.Builder(a).setTitle("주유 · 정비 · 비용 통합 검색")
    .setView(body).setNegativeButton("닫기",null).create();
  final List<GpsSearchRow> gpsLabels=new ArrayList<>();final boolean[] gpsReady={false},gpsFailed={false};
  redraw[0]=()->{
   if(a.isFinishing()||a.isDestroyed()||!dialog.isShowing())return;
   String q=input.getText().toString().trim().toLowerCase(Locale.KOREA);
   filterControl[0].setText("필터 · "+kind[0]+" / "+(RideHistoryBikeFilter.ALL.equals(bikeScope[0])?"전체 바이크":bikeNames.getOrDefault(bikeScope[0],"선택 바이크"))+
    (since[0].isEmpty()&&until[0].isEmpty()?"":" / 날짜 적용"));
   results.removeAllViews();int found=0,shown=0;
   int from=page[0]*30;
   for(LedgerSearchRow row:indexed){
    if("GPS".equals(kind[0]))continue;
    boolean service="services".equals(row.collection),fuel="주유".equals(row.item.optString("type"));
    if("정비".equals(kind[0])&&!service||"주유".equals(kind[0])&&(!fuel||service)||
      "비용".equals(kind[0])&&(fuel||service))continue;
    if(!RideHistoryBikeFilter.ALL.equals(bikeScope[0])&&!bikeScope[0].equals(row.item.optString("vehicleId")))continue;
    String day=row.item.optString("date");
    if(!since[0].isEmpty()&&day.compareTo(since[0])<0||!until[0].isEmpty()&&day.compareTo(until[0])>0)continue;
    if(!q.isEmpty()&&!row.search.contains(q))continue;
    if(found>=from&&shown<30){
     shown++;
     line(results,row.type,row.detail,row.amount,()->{
      dialog.dismiss();a.edit(row.collection,row.item);
     });
    }
    found++;
   }
   for(GpsSearchRow ride:gpsLabels){
    if(!"전체".equals(kind[0])&&!"GPS".equals(kind[0]))continue;
    if(!RideHistoryBikeFilter.ALL.equals(bikeScope[0])&&!bikeScope[0].equals(ride.bikeId))continue;
    if(!since[0].isEmpty()&&(ride.date.isEmpty()||ride.date.compareTo(since[0])<0)||
      !until[0].isEmpty()&&(ride.date.isEmpty()||ride.date.compareTo(until[0])>0))continue;
    if(!q.isEmpty()&&!ride.index.contains(q))continue;
    if(found>=from&&shown<30){
     shown++;
     line(results,"GPS 라이딩",ride.title,"",()->{
      dialog.dismiss();RideStorageManager.show(a,ride.filename);
     });
    }
    found++;
   }
   if(found==0)results.addView(a.text(gpsFailed[0]?"GPS 기록 검색 실패 · 주유/정비 결과만 표시":gpsReady[0]?"검색 결과가 없어요.":"GPS 라이딩 기록 확인 중…",13,a.muted,false));
   if(from+shown<found)results.addView(a.button("다음 30건 보기",false,()->{
    page[0]++;redraw[0].run();scroll.scrollTo(0,0);
   }));
   if(page[0]>0)results.addView(a.button("처음 30건으로",false,()->{
    page[0]=0;redraw[0].run();scroll.scrollTo(0,0);
   }));
   count.setText("검색 결과 "+found+"건 · "+(found==0?0:Math.min(found,from+1))+"–"+Math.min(found,from+shown)+"건"+(gpsFailed[0]?" · GPS 검색 실패":gpsReady[0]?"":" · GPS 확인 중"));
  };
  input.addTextChangedListener(new android.text.TextWatcher(){
   public void beforeTextChanged(CharSequence s,int start,int count,int after){}
   public void onTextChanged(CharSequence text,int start,int before,int count){page[0]=0;redraw[0].run();}
   public void afterTextChanged(android.text.Editable text){}
  });
  a.action(body,"GPS 라이딩 원본도 검색",false,()->{dialog.dismiss();RideStorageManager.show(a);});
  dialog.show();redraw[0].run();
  new Thread(()->{
   List<GpsSearchRow> loaded=new ArrayList<>();boolean success=true;
   try{
    for(java.io.File track:TrackFile.tracks(a.getFilesDir())){
     String binding;
     try{binding=RideBikeBinding.readStrict(track);}catch(java.io.IOException invalid){binding="__corrupt__";}
     loaded.add(new GpsSearchRow(RideHistoryLabel.title(track,java.time.ZoneId.systemDefault()),track.getName(),binding));
    }
   }catch(Exception unavailable){success=false;}
   final boolean complete=success;
   a.runOnUiThread(()->{
    if(a.isFinishing()||a.isDestroyed()||!dialog.isShowing())return;
    gpsLabels.addAll(loaded);gpsReady[0]=true;gpsFailed[0]=!complete;redraw[0].run();
   });
  },"moto-unified-gps-search").start();
 }
 void searchMaintenanceHistory(){
  JSONObject bike=a.bike();
  if(bike==null){a.toast("바이크를 먼저 등록해 주세요.");return;}
  List<JSONObject> completed=new ArrayList<>();
  for(JSONObject service:records("정비",bike))if(!service.optBoolean("scheduleOnly"))completed.add(service);
  LinearLayout layout=a.col();layout.setPadding(a.dp(16),a.dp(6),a.dp(16),a.dp(8));
  EditText query=new EditText(a);query.setSingleLine(true);
  query.setHint("정비 항목 · 날짜 · 계기판 km 검색");
  query.setTextColor(a.fg);query.setHintTextColor(a.muted);
  query.setContentDescription("정비 이력 검색어 입력");
  layout.addView(query,new LinearLayout.LayoutParams(-1,a.dp(52)));
  TextView summary=a.text("검색 중",12,a.muted,false);layout.addView(summary);
  ScrollView sc=new ScrollView(a);
  LinearLayout results=a.col();sc.addView(results);
  layout.addView(sc,new LinearLayout.LayoutParams(-1,a.dp(340)));
  AlertDialog dialog=new AlertDialog.Builder(a).setTitle("완료한 정비 검색")
   .setView(layout).setNegativeButton("닫기",null).create();
  Runnable redraw=()->{
   if(a.isFinishing()||a.isDestroyed())return;
   String needle=query.getText().toString();
   results.removeAllViews();int found=0,displayed=0;
   for(JSONObject item:completed){
    if(!MaintenanceHistorySearch.matches(needle,item.optString("type"),item.optString("date"),item.optDouble("mileage")))continue;
    found++;
    if(displayed++>=30)continue;
    line(results,item.optString("type"),item.optString("date")+" · "+
     a.num(item.optDouble("mileage"))+" km",
     a.won(item.optDouble("cost")),()->{dialog.dismiss();a.edit("services",item);});
   }
   summary.setText("완료한 정비 "+found+"건"+(found>30?" · 최신 30건 표시":""));
   if(found==0)results.addView(a.text("검색된 정비 기록이 없어요.",13,a.muted,false));
  };
  query.addTextChangedListener(new android.text.TextWatcher(){
   public void beforeTextChanged(CharSequence s,int start,int count,int after){}
   public void onTextChanged(CharSequence text,int start,int before,int count){redraw.run();}
   public void afterTextChanged(android.text.Editable text){}
  });
  dialog.show();redraw.run();
 }
 void records(){
  a.gap(a.content,4);
  if(a.bike()==null){empty("기록할 바이크가 없어요","먼저 바이크를 등록해 주세요.","바이크 등록",()->a.edit("vehicles",null));return;}
  bikePicker();
  LinearLayout actions=a.col();actions.setPadding(a.dp(12),a.dp(12),a.dp(12),a.dp(6));actions.setBackground(a.shape(a.panel,a.line,8));
  a.label(actions,"기록 추가",15,a.fg,true);
  a.pair(actions,"간편 주유",()->new QuickEntryUi(a).fuel(),"정비주기 설정",()->new QuickEntryUi(a).maintenance());
  LinearLayout details=a.col();details.setVisibility(View.GONE);
  a.action(details,"정비 완료 내역 상세 입력",false,()->a.edit("services",null));
  a.action(details,"기타 비용·영수증 상세 입력",false,()->a.edit("expenses",null));
  a.action(details,"정비 항목 일괄 등록",false,a::batchServiceEntry);
  Button reveal=a.button("상세 입력·영수증 도구 보기",false,()->{
   boolean visible=details.getVisibility()==View.VISIBLE;
   details.setVisibility(visible?View.GONE:View.VISIBLE);
  });
  reveal.setContentDescription("추가 비용·영수증·일괄 정비 입력 도구 펼치기");
  actions.addView(reveal);actions.addView(details);
  a.action(actions,"전체 기록 검색 · 주유 / 정비 / 비용 / GPS",false,this::searchAllRecords);
  a.content.addView(actions);a.gap(a.content,16);
  new ServiceStatusUi(a).render(a.content);a.gap(a.content,15);
  String bikeId=a.bike().optString("id");
  JSONObject lastFuel=null;
  for(JSONObject record:a.items("expenses"))if(bikeId.equals(record.optString("vehicleId"))&&"주유".equals(record.optString("type"))){
   try{FuelRecordValidation.requireFuel(record.optDouble("amount",Double.NaN),record.optDouble("liters",Double.NaN));}
   catch(Exception ignored){continue;}
   if(lastFuel==null||RecentRecordReuse.isNewer(record.optString("date"),record.optDouble("mileage"),lastFuel.optString("date"),lastFuel.optDouble("mileage")))lastFuel=record;
  }
  if(lastFuel!=null){
   LinearLayout summary=a.col();summary.setPadding(a.dp(15),a.dp(15),a.dp(15),a.dp(13));summary.setBackground(a.shape(a.panel,a.line,8));
   a.label(summary,"최근 주유  ·  "+lastFuel.optString("date"),13,a.muted,false);
   TextView fuelValue=MotoDesign.figure(a,a.won(lastFuel.optDouble("amount"))+"  ·  "+a.num(lastFuel.optDouble("liters"))+" L",20,a.fg);summary.addView(fuelValue);a.gap(summary,8);
   double price=lastFuel.optDouble("amount")/lastFuel.optDouble("liters");
   if(Double.isFinite(price))a.label(summary,"리터당 약 "+a.won(Math.round(price))+" · 실제 영수증 기준",12,a.muted,false);
   a.content.addView(summary);a.gap(a.content,10);
  }
  FuelStats fuelStats=FuelStats.forBike(a.items("expenses"),bikeId);
  if(fuelStats.fills>0){
   LinearLayout insight=a.col();insight.setPadding(a.dp(15),a.dp(12),a.dp(15),a.dp(12));insight.setBackground(a.shape(a.panel,a.line,8));
   a.label(insight,"주유 기록 "+fuelStats.fills+"회 · 평균 리터당 "+a.won(Math.round(fuelStats.averageWonPerLiter)),14,a.fg,true);
   if(fuelStats.fullIntervals>0&&Double.isFinite(fuelStats.kmPerLiter))
    a.label(insight,"만땅 간격 추정 연비 약 "+String.format(java.util.Locale.KOREA,"%.1f",fuelStats.kmPerLiter)+" km/L · "+fuelStats.fullIntervals+"구간",13,a.muted,false);
   else a.label(insight,"만땅 기록 두 번 이상이면 구간별 예상 연비를 표시해요.",12,a.muted,false);
   a.label(insight,"직접 입력한 주유값만 계산 · 계기판과 실제 주유량을 확인하세요.",12,a.muted,false);
   a.content.addView(insight);a.gap(a.content,10);
  }
  tabs(new String[]{"전체","주유","정비","기타"},a.garageMode,v->{a.garageMode=v;a.recordPageOffset=0;a.render();});
  List<JSONObject> list=records(a.garageMode,a.bike());
  a.label(a.content,"기록 "+list.size()+"건"+(a.garageMode.equals("전체")?"":" · "+a.garageMode),13,a.muted,false);
  if(list.isEmpty())a.note("표시할 기록이 없어요. 위의 빠른 기록을 눌러 추가하세요.");
  String last="";
  for(int i=ListPageLimit.start(list.size(),a.recordPageOffset);i<ListPageLimit.end(list.size(),a.recordPageOffset);i++){
   JSONObject x=list.get(i);String month=x.optString("date");month=month.length()>=7?month.substring(0,7):"날짜 없음";
   if(!month.equals(last)){a.section(month);last=month;}
   boolean receipt=a.receiptCount(x.optString("id"))>0;
   final String collection=visibleLedgerCollections.getOrDefault(x,"services");
   line(x.optString("type"),x.optString("date")+" · "+a.num(x.optDouble("mileage"))+" km"+(receipt?" · 영수증":""),
    a.won(x.optDouble("expenses".equals(collection)?"amount":"cost")),
    ()->a.edit(collection,x));
  }
  if(ListPageLimit.canPrevious(list.size(),a.recordPageOffset))a.action(a.content,"◀ 최근 기록",false,()->{a.recordPageOffset=ListPageLimit.previous(list.size(),a.recordPageOffset);a.render();});
  if(ListPageLimit.canNext(list.size(),a.recordPageOffset))a.action(a.content,"이전 기록 ▶ · "+ListPageLimit.end(list.size(),a.recordPageOffset)+"/"+list.size(),false,()->{a.recordPageOffset=ListPageLimit.next(list.size(),a.recordPageOffset);a.render();});
 }
 void tabs(String[] values,String current,MainActivity.Choice select){
  HorizontalScrollView sc=new HorizontalScrollView(a);sc.setHorizontalScrollBarEnabled(false);
  LinearLayout group=new LinearLayout(a);
  for(String value:values){
   boolean selected=current.equals(value);
   Button b=a.button(value,false,()->select.pick(value));
   b.setTextSize(13);b.setMinHeight(a.dp(44));
   b.setTextColor(selected?a.accent:a.fg);
   MotoDesign.ripple(a,b,selected?a.highlight:a.panel,selected?a.accent:a.line,7);
   LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,a.dp(44));
   lp.rightMargin=a.dp(7);group.addView(b,lp);
  }
  sc.addView(group);a.content.addView(sc);a.gap(a.content,12);
 }
 void manageBike(){JSONObject b=a.bike();if(b==null){a.edit("vehicles",null);return;}LinearLayout p=a.page("내 바이크 관리");a.label(p,b.optString("name"),25,a.fg,true);a.label(p,b.optInt("year")+"년식 · "+a.num(b.optDouble("odo"))+" km",17,a.muted,false);a.label(p,"구매가 "+a.won(b.optDouble("purchase")),17,a.fg,false);
  boolean sold=GarageBikeStatus.sold(b.optString("soldDate"));
  if(sold)a.label(p,"판매 완료 · "+b.optString("soldDate")+" · 주유·정비 이력은 보존됩니다.",14,a.muted,false);
  a.action(p,"바이크 정보 수정",true,()->a.edit("vehicles",b));
  a.action(p,sold?"판매 처리 취소 (기록 유지)":"판매 처리 (기록 보존)",false,()->new AlertDialog.Builder(a)
   .setTitle(sold?"판매 처리를 취소할까요?":"판매한 바이크로 보관할까요?")
   .setMessage("바이크와 연결된 주유·정비 기록을 삭제하지 않습니다. 언제든 정보를 다시 수정할 수 있어요.")
   .setNegativeButton("취소",null).setPositiveButton("확인",(d,w)->a.attempt(()->{
    JSONObject next=new JSONObject(a.db.toString());JSONObject record=Store.find(next,"vehicles",b.optString("id"));
    if(record==null)throw new Exception("바이크가 삭제됐어요.");
    record.put("soldDate",sold?"":a.today());a.write(next);a.close();a.render();
   })).show());a.action(p,"대표 바이크로 설정",false,()->a.attempt(()->{JSONObject next=new JSONObject(a.db.toString());next.getJSONObject("settings").put("primary",b.getString("id"));a.write(next);a.selected="";a.getSharedPreferences("garage_ui",0).edit().remove("selected_bike").apply();a.close();a.render();}));a.action(p,"다른 바이크 추가",false,()->a.edit("vehicles",null));a.divider(p);a.action(p,"바이크와 관련 기록 삭제",false,()->{a.close();a.remove("vehicles",b);});a.show();}
 void costDashboard(){
  LinearLayout page=a.page("바이크별 유지비");
  java.util.List<JSONObject> bikes=a.items("vehicles");
  if(bikes.isEmpty()){a.label(page,"먼저 바이크를 등록해 주세요.",16,a.muted,false);a.show();return;}
  java.time.YearMonth now=java.time.YearMonth.now(java.time.ZoneId.of("Asia/Seoul"));
  // Snapshot each collection once, instead of decoding it repeatedly for every month and bike.
  java.util.List<JSONObject> allExpenses=a.items("expenses"),allServices=a.items("services");
  for(JSONObject bike:bikes){
   a.label(page,bike.optString("name","내 바이크"),21,a.fg,true);
   String id=bike.optString("id");
   RecordMonthlySummary totals=RecordMonthlySummary.forBike(allExpenses,allServices,id,now);
   double allFuel=totals.sumFuelAndOther(),allService=totals.sumService();
   a.label(page,"계기판 기록 "+a.num(bike.optDouble("odo"))+" km · 실제 계기판과 대조 필요",14,a.muted,false);
   int dueCount=0,upcoming=0;
   // An older oil-change record must not create a second overdue warning.
   // Reuse the same latest-per-type policy as MY GARAGE and its reminders.
   String localToday=java.time.LocalDate.now(java.time.ZoneId.of("Asia/Seoul")).toString();
   for(JSONObject service:LatestServiceByType.latest(allServices,id).values()){
    if(Store.due(service,bike,localToday))dueCount++;
    else if(service.optDouble("nextKm",0)>bike.optDouble("odo",0)&&service.optDouble("nextKm",0)-bike.optDouble("odo",0)<=1000)upcoming++;
   }
   a.label(page,"정비 초과/도래 "+dueCount+"건 · 1,000km 이내 예정 "+upcoming+"건",14,dueCount>0?a.accent:a.muted,true);
   for(int m=0;m<6;m++){
    String month=now.minusMonths(m).toString();
    double fuel=totals.fuel[m]+totals.other[m],service=totals.service[m];
    a.label(page,month+"  ·  주유 "+a.won(totals.fuel[m])+"  ·  기타 "+a.won(totals.other[m])+"  ·  정비 "+a.won(service),13,a.muted,false);
    LinearLayout bars=new LinearLayout(a);bars.setMinimumHeight(a.dp(8));
    int fuelWidth=CostDashboard.proportion(fuel,fuel+service),serviceWidth=100-fuelWidth;
    if(fuel+service>0){View orange=new View(a);orange.setBackgroundColor(a.accent);bars.addView(orange,new LinearLayout.LayoutParams(0,a.dp(8),fuelWidth));View grey=new View(a);grey.setBackgroundColor(a.line);bars.addView(grey,new LinearLayout.LayoutParams(0,a.dp(8),serviceWidth));page.addView(bars);a.gap(page,10);}
   }
   double currentMonth=totals.monthTotal(0),previousMonth=totals.monthTotal(1);
   a.label(page,"이번 달 "+a.won(currentMonth)+" · "+CostTrend.caption(currentMonth,previousMonth),14,a.accent,true);
   a.label(page,"최근 6개월 합계  "+a.won(allFuel+allService),16,a.fg,true);a.divider(page);
  }
  a.label(page,"기기에 직접 입력한 기록만 계산합니다. 주유, 기타(보험·부품 등), 정비를 분리했으며 실제 영수증과 대조하세요.",12,a.muted,false);
  a.show();
 }
 void settings(){title("설정","기록·내비·정비·백업만 간편하게 관리");a.section("데이터 및 복구");line("백업 · 복원","바이크·주유·정비·GPS·사진 전체 백업","열기",this::basicBackup);
  line("앱 업데이트",BuildConfig.UPDATE_MANIFEST_URL.isEmpty()?"서버 미연동":"최신 버전 확인 및 설치","열기",()->new AppUpdater(a).show());

  a.section("내비 · 알림");


  line("기본 내비 선택","현재: "+NavigationApps.preferred(a).label+" · 카카오내비 / 네이버지도 / 티맵","변경",()->NavigationApps.choose(a));

  // Both widgets now show only shortcuts and never expose private garage data; no redundant privacy toggle.
  line("정비 알림",DailyMaintenanceJob.remindersEnabled(a)?"켜짐 · 날짜 기준 기기 알림":"꺼짐 · 동의 후 활성화","변경",()->{
   boolean enabled=DailyMaintenanceJob.remindersEnabled(a);
   new AlertDialog.Builder(a).setTitle("정비 알림")
    .setMessage(enabled?"정비 알림을 끌까요? 저장 기록은 유지됩니다.":"정비 예정일과 남은 거리를 기기에서 확인해 알립니다. 정비 정보는 서버로 보내지 않으며 알림은 기기 설정에서 언제든 해제할 수 있습니다. 알림은 일정에 따라 지연될 수 있습니다.")
    .setNegativeButton("취소",null)
    .setPositiveButton(enabled?"끄기":"동의하고 켜기",(d,w)->{
     a.getSharedPreferences("notifications",0).edit().putBoolean("maintenance_opt_in",!enabled).apply();
     DailyMaintenanceJob.schedule(a);
     if(enabled)DailyMaintenanceJob.clearReminderState(a);
     a.render();
     if(!enabled){
      if(android.os.Build.VERSION.SDK_INT>=33&&a.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED)
       a.requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS},72);
      else DailyMaintenanceJob.checkSoon(a);
     }
    }).show();
  });
  a.section("안전");

  // Keep an accessible emergency stop ONLY if group location sharing is actually live.
  if(Sharing.enabled)line("위치 공유 중지","현재 다른 사람에게 위치를 공유 중입니다.","즉시 중지",()->{
   if(Sharing.enabled)new AlertDialog.Builder(a).setTitle("위치 공유를 중지할까요?")
    .setNegativeButton("취소",null).setPositiveButton("즉시 중지",(d,w)->{Sharing.stop();a.render();}).show();
   else a.toast("위치 공유는 라이딩 화면에서 동의한 경우에만 시작됩니다.");
  });
  a.section("화면");
  line("화면 테마","현재: "+a.settings().optString("theme","dark"),"변경",()->a.choose("화면 테마",new String[]{"system","light","dark"},v->a.attempt(()->{JSONObject next=new JSONObject(a.db.toString());next.getJSONObject("settings").put("theme",v);a.write(next);a.render();})));

  // The data structures for gear/trade-in and diagnostic reports stay untouched.
  // Hidden extras can be opened in version-tap developer settings without cluttering normal UI.
  line("고급 설정 · 문제 해결","위젯·기록 검사·기기 정보","열기",this::advancedSettings);
  a.section("앱 정보");
  // Deliberately hidden until the user taps the version label seven times.
  TextView version=a.text("MOTO HUB " + BuildConfig.VERSION_NAME + " · 기기별 독립 저장 · 계정 불필요",16,a.muted,false);
  LinearLayout.LayoutParams vl=new LinearLayout.LayoutParams(-1,-2);vl.bottomMargin=a.dp(8);a.content.addView(version,vl);
  final int[] taps={0};version.setOnClickListener(v->{
   if(a.getSharedPreferences("developer_experiments",0).getBoolean("enabled",false))return;
   if(++taps[0]>=7){a.getSharedPreferences("developer_experiments",0).edit().putBoolean("enabled",true).apply();a.toast("개발자 모드가 활성화됐어요.");a.render();}
   else if(taps[0]>=4)a.toast("개발자 모드까지 "+(7-taps[0])+"번 남았어요.");
  });
  if(a.getSharedPreferences("developer_experiments",0).getBoolean("enabled",false)){
   line("개발자 모드","실험 기능 · 순정 TFT 연동과 다름 · 기기 데이터 외부 전송 없음","열기",this::developerSettings);
  }a.label(a.content,"앱을 삭제하기 전 ZIP 백업을 휴대폰 외부에 보관하세요.",13,a.muted,false);
 }
 void advancedSettings(){
  LinearLayout p=a.page("고급 설정 · 문제 해결");
  a.label(p,"가끔 사용하는 기능을 모았어요. 기존 데이터와 알림 설정은 그대로 유지됩니다.",14,a.muted,false);
  a.action(p,"홈 버튼 표시 · 순서 편집",false,()->new GarageHomeUi(a).editHomeShortcuts());
  a.action(p,"초소형 위젯 추가 (2×1)",false,()->MotoWidgetPin.request(a,true));
  a.action(p,"기본 위젯 추가 (4×1)",false,()->MotoWidgetPin.request(a,false));
  a.action(p,"선택한 내비 실행",false,()->NavigationApps.openPreferred(a));
  a.action(p,"업데이트 데이터 검사",false,this::updateRecoveryReport);
  a.action(p,"라이딩 통합 자가진단",false,this::selfCheck);
  a.action(p,"유지비 통계",false,this::costDashboard);
  String signer=SigningIdentity.current(a);
  a.action(p,"설치 인증서 SHA-256 복사",false,()->{
   if(signer.isEmpty()){a.toast("서명 정보를 확인하지 못했습니다.");return;}
   android.content.ClipboardManager cb=(android.content.ClipboardManager)a.getSystemService(android.content.Context.CLIPBOARD_SERVICE);
   if(cb==null){a.toast("클립보드를 사용할 수 없어요.");return;}
   cb.setPrimaryClip(android.content.ClipData.newPlainText("MOTO HUB 설치 인증서 SHA-256",signer));
   a.toast("공개 인증서 지문만 복사했습니다.");
  });
  a.label(p,"서명이 다른 APK를 설치하려고 기존 앱을 삭제하지 마세요. 먼저 전체 ZIP을 안전한 외부 위치에 백업해야 합니다.",13,a.muted,false);
  a.show();
 }
 void localDiagnostics(){
  boolean fine=a.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)==android.content.pm.PackageManager.PERMISSION_GRANTED;
  android.location.LocationManager lm=(android.location.LocationManager)a.getSystemService(android.content.Context.LOCATION_SERVICE);
  boolean gps=false;try{gps=lm!=null&&lm.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER);}catch(Exception ignored){}
  android.app.NotificationManager nm=(android.app.NotificationManager)a.getSystemService(android.content.Context.NOTIFICATION_SERVICE);
  boolean notify=nm!=null&&nm.areNotificationsEnabled();
  android.bluetooth.BluetoothManager bm=(android.bluetooth.BluetoothManager)a.getSystemService(android.content.Context.BLUETOOTH_SERVICE);
  boolean bluetooth=false;try{bluetooth=bm!=null&&bm.getAdapter()!=null&&bm.getAdapter().isEnabled();}catch(SecurityException ignored){}
  String report=LocalDiagnosticReport.create(BuildConfig.VERSION_NAME,
   UpdateReadiness.fixedIdentity(SigningIdentity.current(a),UpdateManifest.PINNED_CERT_SHA256),
   fine,gps,notify,bluetooth,RideService.running,Sharing.enabled,
   SocialClient.configured(),!SocialClient.INSTANCE.uid.isEmpty(),RideService.count);
  LinearLayout p=a.page("로컬 진단 보고서");
  a.label(p,"개인정보가 제외된 상태 요약입니다. 앱에서 직접 복사하지 않는 한 전송하지 않습니다. 오류 로그·실차 기능 테스트를 대체하지 않습니다.",14,a.muted,false);
  a.label(p,report,14,a.fg,false);
  a.action(p,"진단 보고서 복사",true,()->{
   android.content.ClipboardManager cb=(android.content.ClipboardManager)a.getSystemService(android.content.Context.CLIPBOARD_SERVICE);
   if(cb==null){a.toast("클립보드를 사용할 수 없습니다.");return;}
   cb.setPrimaryClip(android.content.ClipData.newPlainText("MOTO HUB 로컬 진단",report));
   a.toast("좌표·계정 정보 없는 진단 보고서를 복사했어요.");
  });a.show();
 }
 /** One screen, asynchronous backup preflight. The app never tests vehicle controls or uploads a log. */
 void selfCheck(){
  android.location.LocationManager gps=(android.location.LocationManager)a.getSystemService(android.content.Context.LOCATION_SERVICE);
  android.app.NotificationManager nm=(android.app.NotificationManager)a.getSystemService(android.content.Context.NOTIFICATION_SERVICE);
  android.os.PowerManager pm=(android.os.PowerManager)a.getSystemService(android.content.Context.POWER_SERVICE);
  boolean fine=a.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)==android.content.pm.PackageManager.PERMISSION_GRANTED;
  boolean gpsOn=false;try{gpsOn=gps!=null&&gps.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER);}catch(Exception ignored){}
  boolean notify=nm!=null&&nm.areNotificationsEnabled()&&
   (android.os.Build.VERSION.SDK_INT<33||a.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)==android.content.pm.PackageManager.PERMISSION_GRANTED);
  boolean battery=pm!=null&&pm.isIgnoringBatteryOptimizations(a.getPackageName());
  final boolean fineReady=fine,gpsReady=gpsOn,notifyReady=notify,batteryReady=battery;
  LinearLayout p=a.page("MOTO HUB 통합 자가진단");
  a.label(p,"기기 상태와 로컬 백업 파일만 점검합니다. 블루투스 페어링/ECU 쓰기, 위치 전송은 하지 않습니다.",14,a.muted,false);
  TextView result=a.text("저장된 GPS 및 백업 상태 점검 중…",15,a.fg,false);
  p.addView(result);
  a.action(p,"위치·알림 권한 설정",false,()->a.startActivity(new android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
   android.net.Uri.parse("package:"+a.getPackageName()))));
  a.action(p,"배터리 최적화 설정",false,()->a.startActivity(new android.content.Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)));
  a.show();
  final android.app.Dialog opened=a.dialog;
  new Thread(()->{
   boolean backup=false;
   String detail;
   try{
    BackupReadiness.Summary summary=BackupReadiness.inspect(a.getFilesDir());
    backup=summary.withinBasicLimits();
    detail=backup?"파일 수·크기 사전 점검 통과 · 전체 ZIP 복구 가능 여부는 내보내기 후 확인":"파일 수·용량에 주의 필요 · 백업/복원 화면에서 확인";
   }catch(Exception err){detail="백업 사전 점검 실패 · 원본 삭제 금지";}
   final String report=RideDiagnostics.summarize(fineReady,gpsReady,notifyReady,batteryReady,
    RideService.autoArmed,"없음".equals(RideService.obdLastError)?"오류 없음":ObdFailureGuide.classify(RideService.obdLastError),backup)+"\n"+detail;
   a.runOnUiThread(()->{if(!a.isFinishing()&&!a.isDestroyed()&&a.dialog==opened&&opened.isShowing())result.setText(report);});
  },"moto-ondevice-health-check").start();
 }
 void serverPreflight(){
  LinearLayout p=a.page("친구·그룹 서버 진단");
  a.label(p,SocialClient.configured()?"Supabase 앱 설정: 등록됨":"Supabase 앱 설정: 없음 · 친구·그룹 기능만 제한",17,a.fg,true);
  a.label(p,SocialClient.INSTANCE.uid.isEmpty()?"계정 세션: 로그인 안 됨 또는 만료 가능":"계정 세션: 기기에 보관됨 · 실제 서버 권한은 호출 시 확인",15,a.muted,false);
  a.label(p,"위치 공유: "+Sharing.status,15,a.fg,false);
  a.label(p,"진단은 서버로 좌표를 보내지 않습니다. 실제 그룹 공유는 로그인, 그룹 참가, GPS 기록 시작, 명시적 공유 동의가 모두 필요합니다.",14,a.muted,false);
  if(Sharing.enabled)a.action(p,"위치 공유 즉시 중지",false,()->{Sharing.stop();a.toast("이 기기에서 위치 전송을 중단했어요.");});
  a.show();
 }
 void updateRecoveryReport(){
  LinearLayout page=a.page("업데이트 후 데이터 검사");
  a.label(page,UpdateInstallSnapshot.lastReport(a),15,a.fg,false);
  a.label(page,"검사 결과는 참고 정보입니다. 정상으로 표시돼도 전체 ZIP 백업을 따로 보관하세요. 자동 복원은 실행하지 않습니다.",13,a.muted,false);
  a.action(page,"전체 ZIP 백업 만들기",true,a::exportBundle);
  a.action(page,"보존된 로컬 복구 지점 확인",false,this::advancedBackup);
  a.action(page,"전체 백업 복원 도구",false,a::importBundle);
  a.show();
 }
 void basicBackup(){
  LinearLayout p=a.page("백업 · 복원");
  a.label(p,"전체 ZIP에는 주행 기록·GPS 복구함·사진·기본 홈 설정이 포함됩니다. 앱 삭제 전에 휴대폰 밖에 보관하세요.",14,a.muted,false);
  a.action(p,"전체 ZIP 백업",true,a::exportBundle);
  a.action(p,"전체 ZIP 복원",false,a::importBundle);
  a.action(p,"추가 도구 · 대용량 백업 · GPS",false,this::backupMore);
  a.show();
 }
 void backupMore(){
  LinearLayout p=a.page("백업 추가 도구");
  a.label(p,"250MB를 넘는 자료나 암호화 백업이 필요할 때 사용하세요. 분할 ZIP은 기존 자료를 덮어쓰지 않는 방식으로 복원합니다.",14,a.muted,false);
  a.action(p,"GPS·사진 분할 ZIP 자동 만들기",false,a::exportBatchedBackup);
  a.action(p,"GPS만 ZIP으로 저장",false,()->a.exportSplitBundle("GPS"));
  a.action(p,"사진·영수증만 ZIP으로 저장",false,()->a.exportSplitBundle("MEDIA"));
  a.action(p,"분할 ZIP 추가 복원",false,a::importSplitBundle);
  a.action(p,"GPS 원본 검색 · 복구함",false,()->RideStorageManager.show(a));
  a.action(p,"암호화 전체 백업",false,a::exportEncryptedBundle);
  a.action(p,"암호화 백업 복원",false,a::importEncryptedBundle);
  a.action(p,"정밀 검사 · JSON 복원",false,this::advancedBackup);
  a.show();
 }
 void advancedBackup(){
LinearLayout p=a.page("백업 · 복원");a.label(p,"ZIP 하나로 바이크·정비·주유·GPS 원본·사진을 백업하고 복원할 수 있어요. 복원은 현재 기록을 교체하므로 기존 기록이 필요하다면 먼저 내보내세요.",16,a.muted,false);a.action(p,"전체 ZIP 백업 복원",true,a::importBundle);a.action(p,"암호화 백업 복원 (.mhub)",false,a::importEncryptedBundle);
  a.action(p,"지금 안전 백업 확인",false,()->{
   if(a.readOnly){a.toast("원본 보호 중에는 새 백업을 만들 수 없어요.");return;}
   // Snapshot a consistent DB state on the UI thread; schema validation and file IO run off-main.
   final String current=a.db.toString();
   a.toast("로컬 백업 확인 중…");
   LOCAL_BACKUP_IO.execute(()->{
    String result;
    try{
     LocalSafetyBackup.snapshot(a,Store.parse(current));
     java.io.File[] snapshots=LocalSafetyBackup.recoveryPoints(a);
     if(snapshots.length==0)result="백업을 확인하지 못했어요. 전체 ZIP으로 내보내 주세요.";
     else{
      LocalSafetyBackup.read(snapshots[0]);
      result="정상 확인한 안전 백업: "+snapshots[0].getName()+"\n\n앱 삭제 전 전체 ZIP을 별도로 내보내세요.";
     }
    }catch(Exception ex){result="로컬 백업 검증 실패: "+ex.getMessage();}
    final String message=result;
    a.runOnUiThread(()->{if(!a.isFinishing()&&!a.isDestroyed())a.error(message);});
   });
  });
  a.action(p,"전체 ZIP 백업 사전 점검",false,()->{
   new Thread(()->{
    try{
     BackupReadiness.Summary result=BackupReadiness.inspect(a.getFilesDir());
     a.runOnUiThread(()->{if(!a.isFinishing()&&!a.isDestroyed())a.error(result.message());});
    }catch(Exception e){a.runOnUiThread(()->{if(!a.isFinishing()&&!a.isDestroyed())a.error("백업 점검 실패: "+e.getMessage());});}
   },"moto-backup-readiness").start();
  });
  a.action(p,"GPS 원본·데이터 정밀 검사",false,()->{
   a.toast("저장한 GPS 원본과 데이터 구조를 점검합니다.");
   LOCAL_BACKUP_IO.execute(()->{
    String report;
    try{report=BackupDeepCheck.check(a.getFilesDir());}
    catch(Exception ex){report="백업 원본 검사 실패 · 앱을 삭제하지 말고 ZIP 백업과 복구 상태를 확인해 주세요.";}
    final String message=report;
    a.runOnUiThread(()->{if(!a.isFinishing()&&!a.isDestroyed())a.error(message);});
   });
  });
  a.action(p,"최근 자동 백업 복원",false,()->{
   a.toast("사용 가능한 복원 지점 확인 중…");
   LOCAL_BACKUP_IO.execute(()->{
    final java.io.File[] snapshots=LocalSafetyBackup.recoveryPoints(a);
    a.runOnUiThread(()->{
     if(a.isFinishing()||a.isDestroyed())return;
     if(snapshots.length==0){a.toast("정상 확인된 자동 백업이 없습니다.");return;}
     String[] choices=new String[snapshots.length];
     for(int i=0;i<snapshots.length;i++){
      String kind=snapshots[i].getParentFile().getName().equals("updates")?"변경 후":"편집 전";
      String when=java.time.Instant.ofEpochMilli(snapshots[i].lastModified()).atZone(java.time.ZoneId.of("Asia/Seoul")).format(java.time.format.DateTimeFormatter.ofPattern("MM/dd HH:mm:ss"));
      choices[i]=when+" · "+kind+" · "+(i+1);
     }
     a.choose("복원할 백업 날짜",choices,date->{
      for(int i=0;i<choices.length;i++)if(choices[i].equals(date)){
       final java.io.File selected=snapshots[i];
       LOCAL_BACKUP_IO.execute(()->{
        try{
         final JSONObject data=LocalSafetyBackup.read(selected);
         a.runOnUiThread(()->{if(!a.isFinishing()&&!a.isDestroyed())a.confirmImport(data);});
        }catch(Exception error){
         a.runOnUiThread(()->{if(!a.isFinishing()&&!a.isDestroyed())a.error("자동 백업을 읽지 못했어요: "+error.getMessage());});
        }
       });
       return;
      }
     });
    });
   });
  });a.label(p,"편집 전 일일 백업 7개 + 변경 후 최근 복원 지점 12개를 기기에 보관합니다. 앱 삭제 시 모두 사라지므로 전체 ZIP을 별도로 내보내세요.",14,a.muted,false);a.action(p,"암호화 전체 백업 만들기 (.mhub)",true,a::exportEncryptedBundle);a.action(p,"암호 없는 ZIP으로 내보내기",false,a::exportBundle);a.action(p,"기존 ZIP 복구 가능 여부 검사",false,()->{
 if(RideService.running||RideService.autoArmed||RestoreRideGate.isRestoring()){a.toast("라이딩이 끝난 뒤 검사해 주세요.");return;}
 android.content.Intent i=new android.content.Intent(android.content.Intent.ACTION_OPEN_DOCUMENT);
 i.setType("application/zip");i.addCategory(android.content.Intent.CATEGORY_OPENABLE);
 i.putExtra(android.content.Intent.EXTRA_MIME_TYPES,new String[]{"application/zip","application/x-zip-compressed","application/octet-stream"});
 a.startActivityForResult(i,60);
});a.action(p,"JSON 백업만 내보내기",false,a::exportData);a.action(p,"JSON 백업 가져오기",false,()->{android.content.Intent i=new android.content.Intent(android.content.Intent.ACTION_OPEN_DOCUMENT);i.setType("*/*");i.addCategory(android.content.Intent.CATEGORY_OPENABLE);a.startActivityForResult(i,42);});a.action(p,"JSON 기록 중복 없이 합치기",false,a::importJsonMerge);a.action(p,"병합 이전 JSON 되돌리기",false,()->a.attempt(()->{android.util.AtomicFile backup=new android.util.AtomicFile(new java.io.File(a.getFilesDir(),"before-json-merge.json"));a.confirmImport(Store.parse(new String(backup.readFully(),java.nio.charset.StandardCharsets.UTF_8)));}));a.action(p,"이전 JSON 되돌리기 (사진·GPS 제외)",false,()->a.attempt(()->{android.util.AtomicFile f=new android.util.AtomicFile(new java.io.File(a.getFilesDir(),"before-import.json"));a.confirmImport(Store.parse(new String(f.readFully(),java.nio.charset.StandardCharsets.UTF_8)));}));a.show();
 }
 void legacyGearManager(){
  LinearLayout p=a.page("보관된 보호장구 기록");
  a.action(p,"장구 추가",true,()->a.edit("gear",null));
  for(JSONObject x:a.items("gear"))a.action(p,x.optString("name"),false,()->a.edit("gear",x));
  a.show();
 }
 void developerSettings(){
  if(!a.getSharedPreferences("developer_experiments",0).getBoolean("enabled",false))return;
  LinearLayout p=a.page("개발자 모드");
  a.label(p,"일반 화면에서 숨긴 기능입니다. 기존 데이터는 삭제하지 않았습니다. 필요할 때만 사용하세요.",15,a.muted,false);
  a.action(p,"보관된 보호장구 기록",false,this::legacyGearManager);
  a.action(p,"기변 계산기",false,a::tradeIn);
  a.action(p,"저장한 수동 코스",false,a::savedRoutes);
  a.action(p,"보관된 옛 매물 데이터",false,()->
   new AlertDialog.Builder(a).setTitle("예전 매물 기록")
    .setMessage("매물 화면은 제거했지만 저장된 "+a.items("listings").size()+
      "건의 기록은 삭제하지 않았습니다. 전체 ZIP·JSON 백업에 그대로 포함됩니다.")
    .setPositiveButton("확인",null).show());
  a.action(p,"고급 백업 도구",false,this::advancedBackup);
  a.action(p,"개인정보 없는 진단 보고서",false,this::localDiagnostics);
  a.action(p,"친구·그룹 서버 진단",false,this::serverPreflight);
  a.action(p,"듀크 390 · OBD2 읽기 전용 (실험)",true,()->{
   if(a.developerObd!=null)a.developerObd.stop();
   a.developerObd=new DeveloperObdScreen(a);a.developerObd.show();
  });
  a.action(p,"개발자 모드 비활성화",false,()->new AlertDialog.Builder(a)
   .setTitle("개발자 모드를 끌까요?")
   .setMessage("실험 기능을 숨기고 OBD2 연결을 종료합니다. 기존 기록은 유지됩니다.")
   .setNegativeButton("취소",null)
   .setPositiveButton("끄기",(d,w)->{
    if(a.developerObd!=null)a.developerObd.stop();
    if(RideService.autoArmed)a.startService(new android.content.Intent(a,RideService.class).setAction(RideService.AUTO_DISARM));
    a.getSharedPreferences("developer_experiments",0).edit().putBoolean("enabled",false).apply();
    a.close();a.render();
   }).show());
  a.show();
 }

}
