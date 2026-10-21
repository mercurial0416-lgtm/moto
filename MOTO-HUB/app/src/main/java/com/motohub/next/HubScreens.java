package com.motohub.next;
import android.app.AlertDialog;import android.graphics.Color;import android.view.*;import android.widget.*;import org.json.*;import java.util.*;
/** Screen composition only. Existing Store, forms and AtomicFile persistence are reused. */
final class HubScreens {
 // Content/schema validation of up to 19 local backups must not freeze settings UI.
 private static final java.util.concurrent.ExecutorService LOCAL_BACKUP_IO=java.util.concurrent.Executors.newSingleThreadExecutor(r->{
  Thread t=new Thread(r,"moto-local-backup-io");t.setDaemon(true);return t;
 });
 final MainActivity a;
 HubScreens(MainActivity activity){a=activity;}
 void title(String name,String detail){a.heading(name,detail);}
 void go(String tab){a.navigate(tab);}
 void empty(String title,String description,String action,Runnable click){a.gap(a.content,16);a.label(a.content,title,22,a.fg,true);a.label(a.content,description,16,a.muted,false);a.gap(a.content,12);a.action(a.content,action,true,click);}
 void line(String title,String detail,String value,Runnable click){line(a.content,title,detail,value,click);}
 void line(LinearLayout target,String title,String detail,String value,Runnable click){
  LinearLayout block=a.col();block.setPadding(a.dp(2),a.dp(17),a.dp(2),a.dp(17));block.setMinimumHeight(a.dp(68));
  LinearLayout head=new LinearLayout(a);head.setGravity(Gravity.CENTER_VERTICAL);
  TextView label=a.text(title,15,a.fg,true);label.setMaxLines(2);
  head.addView(label,new LinearLayout.LayoutParams(0,-2,1));
  if(value!=null&&!value.isEmpty()){
   TextView hint=a.text(value,13,a.muted,true);hint.setGravity(Gravity.END);
   hint.setPadding(a.dp(10),0,0,0);hint.setMaxLines(2);head.addView(hint);
  }block.addView(head);
  if(detail!=null&&!detail.isEmpty()){
   TextView summary=a.text(detail,12,a.muted,false);summary.setLineSpacing(a.dp(2),1f);
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
  List<JSONObject> result=new ArrayList<>();String bikeId=bike.optString("id");
  for(String key:new String[]{"services","expenses"})for(JSONObject record:a.items(key)){
   if(!bikeId.equals(record.optString("vehicleId")))continue;
   boolean service=key.equals("services"),fuel="주유".equals(record.optString("type"));
   if(mode.equals("정비")&&!service||mode.equals("주유")&&!fuel||mode.equals("기타")&&(service||fuel))continue;
   result.add(record);
  }
  result.sort((x,y)->{int date=y.optString("date").compareTo(x.optString("date"));
   return date!=0?date:Double.compare(y.optDouble("mileage"),x.optDouble("mileage"));});
  return result;
 }
 void records(){
  a.gap(a.content,4);a.label(a.content,"주유·정비 내역을 한곳에서 관리하세요.",12,a.muted,false);
  if(a.bike()==null){empty("기록할 바이크가 없어요","먼저 바이크를 등록해 주세요.","바이크 등록",()->a.edit("vehicles",null));return;}
  bikePicker();
  LinearLayout actions=a.col();actions.setPadding(a.dp(14),a.dp(15),a.dp(14),a.dp(8));actions.setBackground(a.shape(a.panel,0,18));
  a.label(actions,"빠른 기록",16,a.fg,true);
  a.pair(actions,"간편 주유",()->new QuickEntryUi(a).fuel(),"정비주기 설정",()->new QuickEntryUi(a).maintenance());
  a.action(actions,"정비 완료 내역 등록",false,()->a.edit("services",null));a.action(actions,"기타 비용·영수증 상세 입력",false,()->a.edit("expenses",null));a.action(actions,"정비 항목 일괄 등록",false,a::batchServiceEntry);
  a.content.addView(actions);a.gap(a.content,14);
  String bikeId=a.bike().optString("id");
  JSONObject lastFuel=null;
  for(JSONObject record:a.items("expenses"))if(bikeId.equals(record.optString("vehicleId"))&&"주유".equals(record.optString("type"))){
   try{FuelRecordValidation.requireFuel(record.optDouble("amount",Double.NaN),record.optDouble("liters",Double.NaN));}
   catch(Exception ignored){continue;}
   if(lastFuel==null||RecentRecordReuse.isNewer(record.optString("date"),record.optDouble("mileage"),lastFuel.optString("date"),lastFuel.optDouble("mileage")))lastFuel=record;
  }
  if(lastFuel!=null){
   LinearLayout summary=a.col();summary.setPadding(a.dp(15),a.dp(15),a.dp(15),a.dp(13));summary.setBackground(a.shape(a.panel,a.line,15));
   a.label(summary,"최근 주유  ·  "+lastFuel.optString("date"),13,a.muted,false);
   a.label(summary,a.won(lastFuel.optDouble("amount"))+"  /  "+a.num(lastFuel.optDouble("liters"))+"L",19,a.fg,true);
   double price=lastFuel.optDouble("amount")/lastFuel.optDouble("liters");
   if(Double.isFinite(price))a.label(summary,"리터당 약 "+a.won(Math.round(price))+" · 실제 영수증 기준",12,a.muted,false);
   a.content.addView(summary);a.gap(a.content,10);
  }
  FuelStats fuelStats=FuelStats.forBike(a.items("expenses"),bikeId);
  if(fuelStats.fills>0){
   LinearLayout insight=a.col();insight.setPadding(a.dp(15),a.dp(12),a.dp(15),a.dp(12));insight.setBackground(a.shape(a.panel,a.line,15));
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
   line((receipt?"▣ ":"")+x.optString("type"),x.optString("date")+" · "+a.num(x.optDouble("mileage"))+" km",
    a.won(x.optDouble(x.has("amount")?"amount":"cost")),()->a.edit(x.has("amount")?"expenses":"services",x));
  }
  if(ListPageLimit.canPrevious(list.size(),a.recordPageOffset))a.action(a.content,"◀ 최근 기록",false,()->{a.recordPageOffset=ListPageLimit.previous(list.size(),a.recordPageOffset);a.render();});
  if(ListPageLimit.canNext(list.size(),a.recordPageOffset))a.action(a.content,"이전 기록 ▶ · "+ListPageLimit.end(list.size(),a.recordPageOffset)+"/"+list.size(),false,()->{a.recordPageOffset=ListPageLimit.next(list.size(),a.recordPageOffset);a.render();});
 }
 void tabs(String[] values,String current,MainActivity.Choice select){HorizontalScrollView sc=new HorizontalScrollView(a);sc.setHorizontalScrollBarEnabled(false);LinearLayout group=new LinearLayout(a);for(String value:values){Button b=a.button(value,current.equals(value),()->select.pick(value));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,-2);lp.rightMargin=a.dp(8);group.addView(b,lp);}sc.addView(group);a.content.addView(sc);a.gap(a.content,12);}
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
 void listings(){
  a.gap(a.content,4);a.label(a.content,"저장한 매물을 검색하고 비교하세요.",12,a.muted,false);
  EditText search=new EditText(a);search.setSingleLine();search.setTextSize(16);search.setTextColor(a.fg);search.setHintTextColor(a.muted);search.setHint("모델명으로 빠르게 찾기");search.setText(a.query);search.setContentDescription("저장한 매물 검색");search.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);
  search.setPadding(a.dp(16),a.dp(12),a.dp(16),a.dp(12));search.setBackground(a.shape(a.panel,0,14));a.content.addView(search,new LinearLayout.LayoutParams(-1,a.dp(53)));
  a.gap(a.content,13);tabs(new String[]{"전체","확인필요","판매중","예약중","판매완료"},a.filter,v->{a.filter=v;a.listingPageOffset=0;a.render();});
  LinearLayout toolbar=new LinearLayout(a);toolbar.setGravity(Gravity.CENTER_VERTICAL);TextView label=a.text("저장한 매물",17,a.fg,true);toolbar.addView(label,new LinearLayout.LayoutParams(0,-2,1));Button order=a.button(a.sort+"  ▾",false,()->a.choose("정렬",new String[]{"최신순","가격 낮은순","주행거리순"},v->{a.sort=v;a.listingPageOffset=0;a.render();}));order.setMinHeight(a.dp(42));order.setTextSize(13);toolbar.addView(order);a.content.addView(toolbar);
  LinearLayout results=a.col();a.content.addView(results);
  search.setOnEditorActionListener((v,id,event)->{a.hideKeyboard();return true;});
  final android.os.Handler searchDelay=new android.os.Handler(android.os.Looper.getMainLooper());
  final Runnable[] pending={null};
  search.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int x,int c,int d){}public void onTextChanged(CharSequence s,int x,int before,int count){
   a.query=s.toString().trim();a.listingPageOffset=0;
   if(pending[0]!=null)searchDelay.removeCallbacks(pending[0]);
   pending[0]=()->{if(!a.isFinishing()&&!a.isDestroyed()&&results.getParent()!=null)showListingResults(results);};
   searchDelay.postDelayed(pending[0],180);
  }public void afterTextChanged(android.text.Editable e){}});
  showListingResults(results);
 }
 void showListingResults(LinearLayout into){
  into.removeAllViews();List<JSONObject> list=a.items("listings");
  final String query=a.query.toLowerCase(Locale.ROOT);
  list.removeIf(x->!x.optString("model").toLowerCase(Locale.ROOT).contains(query)||!a.filter.equals("전체")&&!a.filter.equals(x.optString("status","확인필요")));
  if(a.sort.equals("가격 낮은순"))list.sort(Comparator.comparingDouble(x->x.optDouble("price")));if(a.sort.equals("주행거리순"))list.sort(Comparator.comparingDouble(x->x.optDouble("mileage")));
  a.gap(into,12);a.label(into,list.size()+"개 매물",13,a.muted,false);
  if(list.isEmpty()){TextView none=a.text("조건에 맞는 매물이 없어.\n필터를 바꾸거나 매물을 추가해 봐.",15,a.muted,false);none.setPadding(a.dp(18),a.dp(25),a.dp(18),a.dp(25));none.setBackground(a.shape(a.panel,0,16));into.addView(none);return;}
  for(int i=ListPageLimit.start(list.size(),a.listingPageOffset);i<ListPageLimit.end(list.size(),a.listingPageOffset);i++){JSONObject x=list.get(i);
   LinearLayout card=a.col();card.setPadding(a.dp(16),a.dp(17),a.dp(16),a.dp(10));card.setBackground(a.shape(a.panel,0,17));
   LinearLayout head=new LinearLayout(a);head.setGravity(Gravity.CENTER_VERTICAL);TextView model=a.text(x.optString("model"),19,a.fg,true);head.addView(model,new LinearLayout.LayoutParams(0,-2,1));TextView status=a.text(x.optString("status","확인필요"),12,a.accent,true);status.setPadding(a.dp(8),a.dp(5),a.dp(8),a.dp(5));status.setBackground(a.shape(a.highlight,0,9));head.addView(status);card.addView(head);
   TextView price=a.text(a.won(x.optDouble("price")),25,a.fg,true);LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(-1,-2);pp.topMargin=a.dp(11);card.addView(price,pp);
   TextView specs=a.text(x.optInt("year")+"년식  ·  "+a.num(x.optDouble("mileage"))+" km  ·  "+x.optString("region","지역 미입력"),13,a.muted,false);LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,-2);sp.topMargin=a.dp(9);card.addView(specs,sp);
   card.setClickable(true);card.setFocusable(true);card.setContentDescription(x.optString("model")+", "+a.won(x.optDouble("price"))+". 자세히 보기");card.setOnClickListener(v->a.detail(x));
   CheckBox check=new CheckBox(a);check.setText("비교에 담기");check.setTextSize(13);check.setTextColor(a.muted);check.setMinHeight(a.dp(44));check.setChecked(a.compare.contains(x.optString("id")));check.setOnCheckedChangeListener((v,on)->{String id=x.optString("id");if(on&&a.compare.size()>=3){check.setChecked(false);a.toast("최대 3개까지 비교할 수 있어요.");return;}if(on)a.compare.add(id);else a.compare.remove(id);});
   card.addView(check);LinearLayout.LayoutParams cr=new LinearLayout.LayoutParams(-1,-2);cr.bottomMargin=a.dp(11);into.addView(card,cr);
  }
  if(ListPageLimit.canPrevious(list.size(),a.listingPageOffset))
   into.addView(a.button("◀ 최근 매물",false,()->{a.listingPageOffset=ListPageLimit.previous(list.size(),a.listingPageOffset);showListingResults(into);}));
  if(ListPageLimit.canNext(list.size(),a.listingPageOffset))
   into.addView(a.button("다음 매물 ▶ · "+ListPageLimit.end(list.size(),a.listingPageOffset)+"/"+list.size(),false,()->{a.listingPageOffset=ListPageLimit.next(list.size(),a.listingPageOffset);showListingResults(into);}));
 }
 void compare(){if(a.compare.isEmpty()){a.toast("비교할 매물을 선택해 주세요.");return;}LinearLayout p=a.page("매물 비교");for(JSONObject x:a.items("listings"))if(a.compare.contains(x.optString("id"))){a.label(p,x.optString("model"),22,a.fg,true);a.label(p,a.won(x.optDouble("price")),26,a.accent,true);a.label(p,x.optInt("year")+"년식 · "+a.num(x.optDouble("mileage"))+" km",17,a.fg,false);a.label(p,"퀵시프터 "+x.optString("quick","미확인")+" · "+x.optString("status","확인필요"),16,a.muted,false);a.divider(p);}a.show();}

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
 void settings(){title("설정과 도구","내 기록은 로그인 없이 기기에 보관됩니다");a.section("데이터 및 복구");line("백업 · 복원","JSON 내보내기와 기존 데이터 이전","열기",()->{LinearLayout p=a.page("백업 · 복원");a.label(p,"ZIP 하나로 바이크·정비·주유·GPS 원본·사진을 백업하고 복원할 수 있어요. 복원은 현재 기록을 교체하므로 기존 기록이 필요하다면 먼저 내보내세요.",16,a.muted,false);a.action(p,"전체 ZIP 백업 복원",true,a::importBundle);a.action(p,"암호화 백업 복원 (.mhub)",false,a::importEncryptedBundle);
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
  });a.label(p,"편집 전 일일 백업 7개 + 변경 후 최근 복원 지점 12개를 기기에 보관합니다. 앱 삭제 시 모두 사라지므로 전체 ZIP을 별도로 내보내세요.",14,a.muted,false);a.action(p,"암호화 전체 백업 만들기 (.mhub)",true,a::exportEncryptedBundle);a.action(p,"암호 없는 ZIP으로 내보내기",false,a::exportBundle);a.action(p,"JSON 백업만 내보내기",false,a::exportData);a.action(p,"JSON 백업 가져오기",false,()->{android.content.Intent i=new android.content.Intent(android.content.Intent.ACTION_OPEN_DOCUMENT);i.setType("*/*");i.addCategory(android.content.Intent.CATEGORY_OPENABLE);a.startActivityForResult(i,42);});a.action(p,"이전 JSON 되돌리기 (사진·GPS 제외)",false,()->a.attempt(()->{android.util.AtomicFile f=new android.util.AtomicFile(new java.io.File(a.getFilesDir(),"before-import.json"));a.confirmImport(Store.parse(new String(f.readFully(),java.nio.charset.StandardCharsets.UTF_8)));}));a.show();});
  a.section("표시 및 알림");
  line("위젯 개인정보",a.getSharedPreferences("widget_privacy",0).getBoolean("hide_details",false)?"이름·주행거리·유지비 숨김":"이름·주행거리·유지비 표시","변경",()->{
   boolean hide=a.getSharedPreferences("widget_privacy",0).getBoolean("hide_details",false);
   a.getSharedPreferences("widget_privacy",0).edit().putBoolean("hide_details",!hide).apply();MotoWidgetProvider.refresh(a);a.render();
  });
  line("정비 알림",DailyMaintenanceJob.remindersEnabled(a)?"켜짐 · 날짜 기준 기기 알림":"꺼짐 · 동의 후 활성화","변경",()->{
   boolean enabled=DailyMaintenanceJob.remindersEnabled(a);
   new AlertDialog.Builder(a).setTitle("정비 알림")
    .setMessage(enabled?"정비 알림을 끌까요? 저장 기록은 유지됩니다.":"정비 예정일과 남은 거리를 기기에서 확인해 알립니다. 정비 정보는 서버로 보내지 않으며 알림은 기기 설정에서 언제든 해제할 수 있습니다. 알림은 일정에 따라 지연될 수 있습니다.")
    .setNegativeButton("취소",null)
    .setPositiveButton(enabled?"끄기":"동의하고 켜기",(d,w)->{
     a.getSharedPreferences("notifications",0).edit().putBoolean("maintenance_opt_in",!enabled).apply();
     DailyMaintenanceJob.schedule(a);
     if(enabled){android.app.NotificationManager nm=a.getSystemService(android.app.NotificationManager.class);if(nm!=null)nm.cancel(405);}
     a.render();
     if(!enabled){
      if(android.os.Build.VERSION.SDK_INT>=33&&a.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED)
       a.requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS},72);
      else DailyMaintenanceJob.checkSoon(a);
     }
    }).show();
  });
  a.section("개인정보 및 라이딩");
  line("라이딩·OBD2 사전 점검","GPS·알림·배터리 제한·Bluetooth 상태 확인","열기",this::ridePreflight);
  line("개인정보 없는 진단 보고서","권한·서명·GPS·연결 상태만 요약 · 좌표와 계정 정보 제외","열기",this::localDiagnostics);
  line("친구·그룹 서버 연결",SocialClient.configured()?"서버 주소 설정됨 · 실제 로그인/접속 별도 확인":"서버 미설정 · 로컬 기록 사용 가능","진단",this::serverPreflight);
  line("위치 공유 상태",Sharing.enabled?"그룹 위치 공유 중":"공유 꺼짐 · 명시적 동의 필요",Sharing.enabled?"중지":"확인",()->{
   if(Sharing.enabled)new AlertDialog.Builder(a).setTitle("위치 공유를 중지할까요?")
    .setNegativeButton("취소",null).setPositiveButton("즉시 중지",(d,w)->{Sharing.stop();a.render();}).show();
   else a.toast("위치 공유는 라이딩 화면에서 동의한 경우에만 시작됩니다.");
  });
  a.section("내 바이크와 도구");
  line("기변 계산기","구매·정비·등록 비용과 판매 예상액","열기",a::tradeIn);
  line("화면 테마","현재: "+a.settings().optString("theme","dark"),"변경",()->a.choose("화면 테마",new String[]{"system","light","dark"},v->a.attempt(()->{JSONObject next=new JSONObject(a.db.toString());next.getJSONObject("settings").put("theme",v);a.write(next);a.render();})));
  line("유지비 통계","바이크별 · 최근 6개월 주유/정비 비교","열기",this::costDashboard);
  line("보호장구","장비와 교체 메모","열기",()->{LinearLayout p=a.page("보호장구");a.action(p,"장구 추가",true,()->a.edit("gear",null));for(JSONObject x:a.items("gear"))a.action(p,x.optString("name"),false,()->a.edit("gear",x));a.show();});line("앱 업데이트",BuildConfig.UPDATE_MANIFEST_URL.isEmpty()?"서버 미연동":"최신 버전 확인과 설치","열기",()->new AppUpdater(a).show());a.section("앱 정보");
  // Deliberately hidden until the user taps the version label seven times.
  TextView version=a.text("MOTO HUB " + BuildConfig.VERSION_NAME + " · 기기별 독립 저장 · 계정 불필요",16,a.muted,false);
  LinearLayout.LayoutParams vl=new LinearLayout.LayoutParams(-1,-2);vl.bottomMargin=a.dp(8);a.content.addView(version,vl);
  final int[] taps={0};version.setOnClickListener(v->{
   if(a.getSharedPreferences("developer_experiments",0).getBoolean("enabled",false))return;
   if(++taps[0]>=7){a.getSharedPreferences("developer_experiments",0).edit().putBoolean("enabled",true).apply();a.toast("개발자 모드가 활성화됐어요.");a.render();}
   else if(taps[0]>=4)a.toast("개발자 모드까지 "+(7-taps[0])+"번 남았어요.");
  });
  String signer=SigningIdentity.current(a);
  line("설치 서명 SHA-256",signer.isEmpty()?"인증서 지문을 확인하지 못했습니다":signer.substring(0,Math.min(16,signer.length()))+"… · 업데이트 전 비교",
   "복사",()->{
    if(signer.isEmpty()){a.toast("서명 정보 확인에 실패했어요.");return;}
    android.content.ClipboardManager clipboard=(android.content.ClipboardManager)a.getSystemService(android.content.Context.CLIPBOARD_SERVICE);
    if(clipboard==null){a.toast("클립보드를 사용할 수 없어요.");return;}
    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("MOTO HUB 설치 인증서 SHA-256",signer));
    a.toast("공개 인증서 SHA-256을 복사했어요. 개인키는 포함되지 않습니다.");
   });
  if(a.getSharedPreferences("developer_experiments",0).getBoolean("enabled",false)){
   line("개발자 모드","실험 기능 · 순정 TFT 연동과 다름 · 기기 데이터 외부 전송 없음","열기",this::developerSettings);
  }a.label(a.content,"기존 앱에 덮어쓰기 설치하려면 동일한 서명키로 빌드해야 합니다. 서명이 다른 APK 설치를 위해 기존 앱을 삭제하지 마세요.",13,a.muted,false);a.label(a.content,"앱 삭제 전 전체 ZIP 백업을 저장하세요. GPS는 기기 내 기록입니다. 회원가입과 로그인 없이 이 휴대폰에 기록을 저장합니다. 다른 휴대폰으로 옮길 때는 전체 ZIP 백업을 사용하세요.",16,a.muted,false);
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
 void ridePreflight(){
  LinearLayout p=a.page("라이딩 사전 점검");
  android.location.LocationManager gps=(android.location.LocationManager)a.getSystemService(android.content.Context.LOCATION_SERVICE);
  android.app.NotificationManager nm=(android.app.NotificationManager)a.getSystemService(android.content.Context.NOTIFICATION_SERVICE);
  android.os.PowerManager pm=(android.os.PowerManager)a.getSystemService(android.content.Context.POWER_SERVICE);
  android.bluetooth.BluetoothManager bt=(android.bluetooth.BluetoothManager)a.getSystemService(android.content.Context.BLUETOOTH_SERVICE);
  boolean fine=a.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)==android.content.pm.PackageManager.PERMISSION_GRANTED;
  boolean enabled=false;try{enabled=gps!=null&&gps.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER);}catch(Exception ignored){}
  boolean notify=nm!=null&&nm.areNotificationsEnabled()&&(android.os.Build.VERSION.SDK_INT<33||a.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)==android.content.pm.PackageManager.PERMISSION_GRANTED);
  boolean battery=pm!=null&&pm.isIgnoringBatteryOptimizations(a.getPackageName());
  boolean bluetooth=bt!=null&&bt.getAdapter()!=null&&bt.getAdapter().isEnabled();
  for(String detail:RideReadinessReport.summary(fine,enabled,notify,battery,bluetooth))a.label(p,detail,15,a.muted,false);
  if(RideService.running){
   RideService service=RideService.instance;
   long age=service==null||service.lastFixElapsed<=0?-1:android.os.SystemClock.elapsedRealtime()-service.lastFixElapsed;
   a.label(p,"라이딩 기록: 실행 중 · 마지막 GPS 수신 "+(age<0?"대기 중":(age/1000)+"초 전")+" · 좌표 "+RideService.count+"개",15,a.fg,false);
  }else if(RideService.autoArmed)a.label(p,"OBD2 연결 대기 중 · GPS 기록은 출발 조건 후 시작됩니다.",15,a.muted,false);
  else a.label(p,"라이딩 기록: 중지됨 · 마지막 주행 내용은 기록 탭에서 확인하세요.",15,a.muted,false);

  a.action(p,"위치 권한 확인",false,()->a.startActivity(new android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,android.net.Uri.parse("package:"+a.getPackageName()))));
  a.action(p,"GPS 켜기 설정",false,()->a.startActivity(new android.content.Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)));
  a.action(p,"배터리 최적화 설정 확인",false,()->a.startActivity(new android.content.Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)));
  a.label(p,"이 점검은 설정 상태만 표시하며 위치를 기록하거나 외부에 전송하지 않습니다. OBD2 실제 차량 지원 여부는 별도로 확인해야 합니다.",13,a.muted,false);
  a.show();
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
 void developerSettings(){
  if(!a.getSharedPreferences("developer_experiments",0).getBoolean("enabled",false))return;
  LinearLayout p=a.page("개발자 모드");
  a.label(p,"실험 기능은 일반 화면에 노출되지 않으며 자동 실행되지 않습니다.",15,a.muted,false);
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
