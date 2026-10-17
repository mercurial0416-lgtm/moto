package com.motohub.next;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import org.json.JSONObject;
import java.io.File;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

/** Alpha6-H: MY GARAGE visual home. All numbers are read from local records, never demo values. */
final class GarageHomeUi {
 final MainActivity a;
 final int orange=Color.rgb(255,107,27),light=Color.rgb(237,243,248),soft=Color.rgb(142,159,174);
 final int surface=Color.rgb(19,29,38),card=Color.rgb(23,34,44),stroke=Color.rgb(44,60,71);
 GarageHomeUi(MainActivity a){this.a=a;}

 void header(LinearLayout container){
  container.removeAllViews();container.setPadding(a.dp(20),a.dp(14),a.dp(20),a.dp(7));container.setGravity(Gravity.CENTER_VERTICAL);
  LinearLayout heading=a.col();TextView brand=a.text("MOTO HUB",13,orange,true);brand.setLetterSpacing(.13f);heading.addView(brand);
  TextView name=a.text("MY GARAGE",29,light,true);name.setLetterSpacing(.035f);LinearLayout.LayoutParams nl=new LinearLayout.LayoutParams(-1,-2);nl.topMargin=a.dp(2);heading.addView(name,nl);
  TextView sub=a.text("RIDE  ·  RECORD  ·  CONNECT",10,soft,true);sub.setLetterSpacing(.10f);LinearLayout.LayoutParams sl=new LinearLayout.LayoutParams(-1,-2);sl.topMargin=a.dp(5);heading.addView(sub,sl);
  container.addView(heading,new LinearLayout.LayoutParams(0,-2,1));
  TextView notifications=headerIcon("🔔","정비 알림 확인",this::showNotices);
  LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(a.dp(41),a.dp(43));cp.rightMargin=a.dp(9);container.addView(notifications,cp);
  container.addView(headerIcon("⚙","설정 열기",()->a.navigate("더보기")),new LinearLayout.LayoutParams(a.dp(42),a.dp(43)));
 }
 TextView headerIcon(String symbol,String description,Runnable click){TextView v=a.text(symbol,23,light,true);v.setGravity(Gravity.CENTER);v.setBackground(a.shape(surface,stroke,13));v.setContentDescription(description);v.setClickable(true);v.setFocusable(true);v.setOnClickListener(w->click.run());return v;}
 void showNotices(){JSONObject b=a.bike();if(b==null){a.toast("바이크를 먼저 등록해 주세요.");return;}
  StringBuilder sb=new StringBuilder();int count=0;
  for(JSONObject s:latestServices(b)){if(s.optDouble("nextKm")<=0&&s.optString("nextDate").isEmpty())continue;
   MaintenancePlanner.Status status=HomeMaintenanceStatus.safeStatus(s.optString("nextDate"),s.optDouble("nextKm"),a.today(),b.optDouble("odo"));
   if(count++>=5)break;sb.append("• ").append(s.optString("type")).append("  ").append(MaintenancePlanner.caption(status)).append("\n");
  }
  if(!b.optString("insuranceDate").isEmpty())sb.append("• 보험 만료: ").append(b.optString("insuranceDate"));
  if(sb.length()==0)sb.append("예정된 정비 일정이 없습니다. 정비 기록에서 다음 점검 거리를 설정할 수 있어요.");
  new AlertDialog.Builder(a).setTitle("정비 알림").setMessage(sb.toString()).setNegativeButton("닫기",null).setPositiveButton("정비 기록",(d,w)->a.navigate("차고")).show();
 }

 void render(){
  JSONObject b=a.bike();if(b==null){emptyGarage();return;}
  addHero(b);stats(b);a.gap(a.content,12);rideAction();a.gap(a.content,14);quickActions();a.gap(a.content,20);insights(b);a.gap(a.content,20);
  listings();a.gap(a.content,16);friends();a.gap(a.content,18);recent(b);
 }
 void emptyGarage(){
  LinearLayout blank=a.col();blank.setGravity(Gravity.CENTER);blank.setPadding(a.dp(24),a.dp(45),a.dp(24),a.dp(44));blank.setBackground(a.shape(surface,stroke,20));
  TextView intro=a.text("MY FIRST BIKE",12,orange,true);intro.setLetterSpacing(.16f);blank.addView(intro);
  TextView big=a.text("첫 바이크를 등록해 봐",24,light,true);LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-1,-2);bp.topMargin=a.dp(15);big.setGravity(Gravity.CENTER);blank.addView(big,bp);
  TextView hint=a.text("등록하면 이곳에 내 바이크 사진과 주행거리, 정비 상태가 자동으로 표시돼.",14,soft,false);hint.setGravity(Gravity.CENTER);LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(-1,-2);hp.topMargin=a.dp(12);blank.addView(hint,hp);
  Button add=a.button("＋ 내 바이크 등록하기",true,()->a.edit("vehicles",null));add.setTextColor(Color.WHITE);add.setBackground(a.shape(orange,0,15));LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(-1,a.dp(54));ap.topMargin=a.dp(24);blank.addView(add,ap);a.content.addView(blank);
 }

 void addHero(JSONObject bike){
  LinearLayout box=a.col();box.setBackground(a.shape(surface,stroke,19));box.setClipToOutline(true);
  FrameLayout imageArea=new FrameLayout(a);imageArea.setBackgroundColor(Color.rgb(14,20,26));imageArea.setLayoutParams(new LinearLayout.LayoutParams(-1,a.dp(215)));
  ImageView visual=new ImageView(a);visual.setScaleType(ImageView.ScaleType.CENTER_CROP);
  boolean own=loadBikePhoto(visual,bike);
  imageArea.addView(visual,new FrameLayout.LayoutParams(-1,-1));
  View shade=new View(a);shade.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
   new int[]{0x00000000,0x15060B0F,0xC9081016}));imageArea.addView(shade,new FrameLayout.LayoutParams(-1,-1));
  int bikeCount=a.items("vehicles").size();
  boolean canSwitch=HomeBikeSwitch.visible(bikeCount);
  TextView tag=a.text(HomeBikeSwitch.label(bikeCount),11,Color.WHITE,true);tag.setGravity(Gravity.CENTER);tag.setLetterSpacing(.07f);tag.setPadding(a.dp(12),a.dp(7),a.dp(12),a.dp(7));tag.setBackground(a.shape(orange,0,9));FrameLayout.LayoutParams tp=new FrameLayout.LayoutParams(-2,-2,Gravity.TOP|Gravity.START);tp.setMargins(a.dp(15),a.dp(15),0,0);imageArea.addView(tag,tp);
  if(canSwitch){tag.setContentDescription("다른 바이크로 전환");tag.setClickable(true);tag.setFocusable(true);tag.setOnClickListener(v->new HubScreens(a).chooseBike());}
  TextView photo=a.text("▧  사진 변경",12,light,true);photo.setGravity(Gravity.CENTER);photo.setPadding(a.dp(10),a.dp(8),a.dp(10),a.dp(8));photo.setBackground(a.shape(0xB9121A20,0,9));photo.setContentDescription("내 바이크 사진 선택");FrameLayout.LayoutParams pp=new FrameLayout.LayoutParams(-2,-2,Gravity.TOP|Gravity.RIGHT);pp.setMargins(0,a.dp(15),a.dp(15),0);imageArea.addView(photo,pp);photo.setOnClickListener(v->a.chooseGaragePhoto());
  TextView example=a.text(BikeVisual.caption(bike.optString("name"),own),10,0xFFD0DCE3,false);FrameLayout.LayoutParams ep=new FrameLayout.LayoutParams(-2,-2,Gravity.BOTTOM|Gravity.RIGHT);ep.setMargins(0,0,a.dp(15),a.dp(9));imageArea.addView(example,ep);
  if(!own){
   JSONObject known=MotorcycleCatalog.search(a,bike.optString("name"));
   String brand=bike.optString("catalogBrand",known==null?"":known.optString("brand"));
   String model=bike.optString("catalogModel",known==null?"":known.optString("model"));
   int year=bike.optInt("year");
   if(!brand.isEmpty()&&!model.isEmpty())MotorcyclePhoto.show(a,visual,example,brand,model,year);
   else example.setText("모델 사진 미확인 · 사진 변경으로 직접 등록");
  }
  imageArea.setClickable(true);imageArea.setFocusable(true);imageArea.setContentDescription("바이크 사진. 탭하면 사진 변경");imageArea.setOnClickListener(v->a.chooseGaragePhoto());box.addView(imageArea);
  LinearLayout meta=new LinearLayout(a);meta.setPadding(a.dp(17),a.dp(15),a.dp(14),a.dp(17));meta.setGravity(Gravity.CENTER_VERTICAL);
  LinearLayout info=a.col();TextView name=a.text(bike.optString("name"),24,light,true);name.setMaxLines(2);info.addView(name);
  TextView sub=a.text((bike.optInt("year")>0?bike.optInt("year")+" · ":"")+(condition(bike).equals("점검 필요")?"MY BIKE · MAINTENANCE DUE":condition(bike).equals("곧 점검")?"MY BIKE · SERVICE SOON":condition(bike).equals("정비 일정 없음")?"MY BIKE · SET MAINTENANCE":"MY BIKE · READY TO RIDE"),12,soft,true);LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,-2);sp.topMargin=a.dp(6);info.addView(sub,sp);meta.addView(info,new LinearLayout.LayoutParams(0,-2,1));
  String condition=condition(bike);TextView badge=a.text(condition,11,condition.equals("점검 필요")?0xFFFFBEAA:condition.equals("곧 점검")?0xFFFFD58A:condition.equals("정비 정상")?0xFF62E3AA:soft,true);badge.setPadding(a.dp(9),a.dp(9),a.dp(9),a.dp(9));badge.setBackground(a.shape(condition.equals("점검 필요")?0xFF502A20:condition.equals("곧 점검")?0xFF503E20:condition.equals("정비 정상")?0xFF153E30:0xFF27343F,0,11));meta.addView(badge);
  meta.setClickable(true);meta.setOnClickListener(v->new HubScreens(a).manageBike());meta.setContentDescription("바이크 상세 관리 열기");box.addView(meta);a.content.addView(box);
 }
 boolean loadBikePhoto(ImageView iv,JSONObject bike){
  File custom=a.garagePhotoFile(bike.optString("id"));
  if(custom!=null&&custom.isFile()){
   android.graphics.BitmapFactory.Options options=new android.graphics.BitmapFactory.Options();options.inJustDecodeBounds=true;android.graphics.BitmapFactory.decodeFile(custom.getAbsolutePath(),options);
   options.inJustDecodeBounds=false;options.inSampleSize=1;while(options.outWidth/options.inSampleSize>1400||options.outHeight/options.inSampleSize>1400)options.inSampleSize*=2;
   android.graphics.Bitmap photo=android.graphics.BitmapFactory.decodeFile(custom.getAbsolutePath(),options);
   if(photo!=null){iv.setImageBitmap(photo);return true;}
  }
  iv.setImageResource(BikeVisual.superCub(bike.optString("name"))?R.drawable.garage_super_cub:R.drawable.garage_model_placeholder);return false;
 }
 List<JSONObject> latestServices(JSONObject b){Map<String,JSONObject> latest=new LinkedHashMap<>();for(JSONObject s:a.items("services"))if(b.optString("id").equals(s.optString("vehicleId")))latest.putIfAbsent(s.optString("type"),s);return new ArrayList<>(latest.values());}
 String condition(JSONObject b){
  List<HomeMaintenanceStatus.Schedule> schedules=new ArrayList<>();
  for(JSONObject service:latestServices(b))
   schedules.add(new HomeMaintenanceStatus.Schedule(service.optString("nextDate"),service.optDouble("nextKm")));
  return HomeMaintenanceStatus.label(HomeMaintenanceStatus.summary(schedules,a.today(),b.optDouble("odo")));
 }
 String nextService(JSONObject b){
  String summary=condition(b);
  if(summary.equals("점검 필요")||summary.equals("곧 점검"))return summary;
  double odo=b.optDouble("odo"),min=Double.POSITIVE_INFINITY;String date="";
  for(JSONObject service:latestServices(b)){
   double next=service.optDouble("nextKm");String d=service.optString("nextDate");
   if(next>0)min=Math.min(min,next-odo);
   if(d.matches("\\d{4}-\\d{2}-\\d{2}")&&(date.isEmpty()||d.compareTo(date)<0))date=d;
  }
  if(min!=Double.POSITIVE_INFINITY)return a.num(Math.max(0,min))+" km";
  return date.isEmpty()?"미설정":date.substring(5);
 }
 int monthRides(){int count=0;String ym=a.today().substring(0,7);try{
  for(File f:TrackFile.tracks(a.getFilesDir())){
   try {String raw=f.getName().split("-")[0];long time=Long.parseLong(raw);String period=Instant.ofEpochMilli(time).atZone(ZoneId.of("Asia/Seoul")).toLocalDate().toString();if(period.startsWith(ym))count++;}
   catch(NumberFormatException|java.time.DateTimeException ignored){}
  }
 }catch(Exception ignored){}return count;
 }
 void stats(JSONObject b){LinearLayout row=new LinearLayout(a);row.setPadding(0,a.dp(10),0,0);
  String[] values={a.num(b.optDouble("odo"))+" km",nextService(b),monthRides()+" 회"};String[] names={"누적 주행거리","다음 정비까지","이번 달 주행"};String[] symbols={"◈","⚒","▥"};
  for(int i=0;i<3;i++){
   final int n=i;LinearLayout card=a.col();card.setGravity(Gravity.CENTER);card.setPadding(a.dp(5),a.dp(13),a.dp(5),a.dp(13));card.setBackground(a.shape(surface,stroke,15));
   TextView icon=a.text(symbols[i],20,orange,true);icon.setGravity(Gravity.CENTER);card.addView(icon);
   TextView number=a.text(values[i],i==1&&values[i].length()>8?15:18,light,true);number.setGravity(Gravity.CENTER);number.setMaxLines(1);number.setAutoSizeTextTypeUniformWithConfiguration(12,18,1,android.util.TypedValue.COMPLEX_UNIT_SP);LinearLayout.LayoutParams vl=new LinearLayout.LayoutParams(-1,a.dp(28));vl.topMargin=a.dp(7);card.addView(number,vl);
   TextView label=a.text(names[i],11,soft,false);label.setGravity(Gravity.CENTER);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=a.dp(5);card.addView(label,lp);
   if(n==1){card.setContentDescription("다음 정비까지 "+values[i]+". 정비 기록으로 이동");card.setOnClickListener(v->a.navigate("차고"));card.setClickable(true);}
   LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(0,-2,1);if(i>0)rp.leftMargin=a.dp(8);row.addView(card,rp);
  }
  a.content.addView(row);
 }
 void rideAction(){Button btn=a.button(RideService.running?"■  라이딩 종료":"➤  라이딩 시작  ›",true,()->{
  if(RideService.running){new AlertDialog.Builder(a).setTitle("주행 기록을 종료할까요?").setMessage("기록을 종료하면 현재 GPS 위치 수집을 중단합니다.").setNegativeButton("취소",null).setPositiveButton("종료",(d,w)->{a.startService(new Intent(a,RideService.class).setAction(RideService.STOP));a.toast("정상 종료를 요청했어요.");a.root.postDelayed(()->{if(!a.isFinishing())a.render();},750);}).show();}
  else {a.navigate("라이딩");new RideUi(a).requestStart();}
 });btn.setTextColor(Color.WHITE);btn.setTextSize(19);btn.setMinHeight(a.dp(62));btn.setBackground(a.shape(RideService.running?Color.rgb(218,58,51):orange,0,16));a.content.addView(btn,new LinearLayout.LayoutParams(-1,a.dp(62)));}
 void quickActions(){
  String[] icons={"⛽","⚒","▤","▣"};
  String[] labels={"주유 기록","정비 기록","라이딩 · GPX","내 차고"};
  String[] details={"주유비 입력","정비 내역 입력","경로 보기 · 가져오기","바이크 관리"};
  Runnable[] clicks={()->a.edit("expenses",null),()->a.edit("services",null),()->a.navigate("라이딩"),()->new HubScreens(a).manageBike()};
  for(int rowIndex=0;rowIndex<2;rowIndex++){
   LinearLayout row=new LinearLayout(a);
   for(int colIndex=0;colIndex<2;colIndex++){
    int i=rowIndex*2+colIndex;
    LinearLayout tile=a.col();tile.setGravity(Gravity.CENTER);tile.setPadding(a.dp(8),a.dp(14),a.dp(8),a.dp(12));tile.setMinimumHeight(a.dp(92));tile.setBackground(a.shape(surface,stroke,15));
    TextView ico=a.text(icons[i],22,orange,true);ico.setGravity(Gravity.CENTER);tile.addView(ico);
    TextView name=a.text(labels[i],15,light,true);name.setGravity(Gravity.CENTER);LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(-1,-2);np.topMargin=a.dp(7);tile.addView(name,np);
    TextView small=a.text(details[i],11,soft,false);small.setGravity(Gravity.CENTER);LinearLayout.LayoutParams dp=new LinearLayout.LayoutParams(-1,-2);dp.topMargin=a.dp(4);tile.addView(small,dp);
    Runnable click=clicks[i];tile.setClickable(true);tile.setFocusable(true);tile.setContentDescription(labels[i]+" 바로가기");
    android.util.TypedValue ripple=new android.util.TypedValue();
    if(a.getTheme().resolveAttribute(android.R.attr.selectableItemBackground,ripple,true))tile.setForeground(a.getDrawable(ripple.resourceId));
    tile.setOnClickListener(v->click.run());
    LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(0,-2,1);if(colIndex>0)rp.leftMargin=a.dp(9);row.addView(tile,rp);
   }
   LinearLayout.LayoutParams rowParams=new LinearLayout.LayoutParams(-1,-2);if(rowIndex>0)rowParams.topMargin=a.dp(9);a.content.addView(row,rowParams);
  }
 }
 void sectionTitle(String title,String action,Runnable click){LinearLayout row=new LinearLayout(a);row.setGravity(Gravity.CENTER_VERTICAL);
  TextView t=a.text(title,17,light,true);row.addView(t,new LinearLayout.LayoutParams(0,-2,1));if(action!=null){TextView more=a.text(action,12,soft,true);more.setPadding(a.dp(8),a.dp(9),a.dp(3),a.dp(9));more.setOnClickListener(v->click.run());row.addView(more);}a.content.addView(row);a.gap(a.content,10);
 }
 void insights(JSONObject b){
  sectionTitle("MY BIKE INSIGHTS","기록 관리  ›",()->a.navigate("차고"));
  String month=a.today().substring(0,7);double spent=0;int expenseCount=0;
  for(String key:new String[]{"services","expenses"})for(JSONObject item:a.items(key))
   if(b.optString("id").equals(item.optString("vehicleId"))&&item.optString("date").startsWith(month)){
    spent+=item.optDouble(key.equals("services")?"cost":"amount");expenseCount++;
   }
  double efficiency=0;int segments=0;
  try{JSONObject fuel=Store.fuelStats(a.db,b.optString("id"));efficiency=fuel.optDouble("kmPerLiter");segments=fuel.optInt("segments");}
  catch(Exception ignored){}
  LinearLayout frame=a.col();frame.setPadding(a.dp(15),a.dp(15),a.dp(15),a.dp(12));frame.setBackground(a.shape(surface,stroke,15));
  LinearLayout cells=new LinearLayout(a);
  String[] title={"이번 달 유지비","실측 연비"};
  String[] values={a.won(spent),segments>0?String.format(Locale.KOREA,"%.1f km/L",efficiency):"기록 부족"};
  for(int i=0;i<2;i++){
   LinearLayout tile=a.col();tile.addView(a.text(title[i],12,soft,false));
   TextView value=a.text(values[i],i==0?17:19,light,true);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=a.dp(7);tile.addView(value,lp);
   LinearLayout.LayoutParams size=new LinearLayout.LayoutParams(0,-2,1);if(i==1)size.leftMargin=a.dp(11);cells.addView(tile,size);
  }
  frame.addView(cells);a.gap(frame,12);
  TextView detail=a.text("유지비 "+expenseCount+"건 · 연비는 가득 주유→가득 주유 구간만 계산",11,soft,false);
  frame.addView(detail);a.content.addView(frame);
 }
 void listings(){sectionTitle("저장한 매물","모두 보기  ›",()->a.navigate("매물"));List<JSONObject> cars=a.items("listings");
  if(cars.isEmpty()){TextView blank=a.text("저장한 매물이 없어. 마음에 드는 바이크를 추가해 봐.",13,soft,false);blank.setPadding(a.dp(13),a.dp(16),a.dp(13),a.dp(16));blank.setBackground(a.shape(surface,stroke,14));blank.setOnClickListener(v->a.navigate("매물"));a.content.addView(blank);return;}
  LinearLayout row=new LinearLayout(a);int index=0;for(JSONObject x:cars){if(index++>=2)break;
   LinearLayout tile=a.col();tile.setPadding(a.dp(13),a.dp(13),a.dp(11),a.dp(13));tile.setBackground(a.shape(surface,stroke,13));
   TextView name=a.text(x.optString("model","모델 미입력"),14,light,true);name.setMaxLines(1);name.setEllipsize(android.text.TextUtils.TruncateAt.END);tile.addView(name);
   TextView info=a.text(x.optInt("year")+"년 · "+a.num(x.optDouble("mileage"))+" km",11,soft,false);LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(-1,-2);ip.topMargin=a.dp(6);tile.addView(info,ip);
   TextView price=a.text(a.won(x.optDouble("price")),15,orange,true);LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(-1,-2);pp.topMargin=a.dp(9);tile.addView(price,pp);
   tile.setOnClickListener(v->a.detail(x));tile.setFocusable(true);tile.setClickable(true);LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(0,-2,1);if(index>1)rp.leftMargin=a.dp(8);row.addView(tile,rp);
  }a.content.addView(row);
 }
 void friends(){sectionTitle("친구 & 그룹 라이딩","열기  ›",()->a.navigate("지도"));LinearLayout row=new LinearLayout(a);row.setPadding(a.dp(14),a.dp(16),a.dp(12),a.dp(16));row.setBackground(a.shape(surface,stroke,14));row.setGravity(Gravity.CENTER_VERTICAL);
  TextView people=a.text("♧",25,orange,true);row.addView(people,new LinearLayout.LayoutParams(a.dp(34),-2));
  LinearLayout details=a.col();details.addView(a.text(Sharing.enabled?"실시간 위치 공유 중":"친구와 함께 달리기",14,light,true));TextView sub=a.text(Sharing.enabled?"친구 화면에서 공유 상태 확인":"친구 초대 · 그룹 라이딩 · 위치 공유",11,soft,false);LinearLayout.LayoutParams sl=new LinearLayout.LayoutParams(-1,-2);sl.topMargin=a.dp(5);details.addView(sub,sl);row.addView(details,new LinearLayout.LayoutParams(0,-2,1));row.addView(a.text("›",22,orange,true));row.setClickable(true);row.setOnClickListener(v->a.navigate("지도"));a.content.addView(row);
 }
 void recent(JSONObject b){sectionTitle("최근 기록","전체 보기  ›",()->a.navigate("차고"));List<JSONObject> r=new HubScreens(a).records("전체",b);
  if(r.isEmpty()){a.label(a.content,"주유·정비 기록을 남기면 여기에 표시돼.",13,soft,false);return;}
  int count=0;for(JSONObject x:r){if(count++>=2)break;
   LinearLayout row=new LinearLayout(a);row.setPadding(a.dp(12),a.dp(12),a.dp(12),a.dp(12));row.setGravity(Gravity.CENTER_VERTICAL);row.setBackground(a.shape(surface,0,12));LinearLayout details=a.col();details.addView(a.text(x.optString("type","기록"),13,light,true));TextView date=a.text(x.optString("date"),11,soft,false);LinearLayout.LayoutParams dp=new LinearLayout.LayoutParams(-1,-2);dp.topMargin=a.dp(3);details.addView(date,dp);row.addView(details,new LinearLayout.LayoutParams(0,-2,1));row.addView(a.text(a.won(x.optDouble(x.has("amount")?"amount":"cost")),13,orange,true));row.setOnClickListener(v->a.edit(x.has("amount")?"expenses":"services",x));row.setClickable(true);a.content.addView(row);a.gap(a.content,7);
  }
 }
}
