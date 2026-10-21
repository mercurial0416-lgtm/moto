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
 final int orange,light,soft,surface,card,stroke;
 GarageHomeUi(MainActivity a){this.a=a;
  orange=a.accent;light=a.fg;soft=a.muted;surface=a.panel;card=a.panel;stroke=a.line;
 }

 void showNotices(){JSONObject b=a.bike();if(b==null){a.toast("바이크를 먼저 등록해 주세요.");return;}
  List<MaintenanceNoticeOrder.Notice> history=new ArrayList<>();
  for(JSONObject s:latestServices(b))history.add(new MaintenanceNoticeOrder.Notice(
   s.optString("type"),s.optString("date"),s.optString("nextDate"),s.optDouble("nextKm")));
  List<MaintenanceNoticeOrder.Notice> notices=MaintenanceNoticeOrder.prioritized(history,a.today(),b.optDouble("odo"));
  int overdue=0,soon=0;
  for(MaintenanceNoticeOrder.Notice n:notices){
   MaintenancePlanner.Status status=HomeMaintenanceStatus.safeStatus(n.nextDate,n.nextKm,a.today(),b.optDouble("odo"));
   if(status==MaintenancePlanner.Status.OVERDUE)overdue++;
   else if(status==MaintenancePlanner.Status.SOON)soon++;
  }
  StringBuilder sb=new StringBuilder();
  if(overdue>0||soon>0)sb.append("점검 필요 ").append(overdue).append("건 · 곧 점검 ").append(soon).append("건\n\n");
  for(int i=0;i<Math.min(5,notices.size());i++){
   MaintenanceNoticeOrder.Notice n=notices.get(i);
   MaintenancePlanner.Status status=HomeMaintenanceStatus.safeStatus(n.nextDate,n.nextKm,a.today(),b.optDouble("odo"));
   sb.append("• ").append(n.type.isEmpty()?"정비":n.type).append("  ").append(MaintenancePlanner.caption(status));
   String detail=MaintenanceNoticeText.detail(n.nextDate,n.nextKm,a.today(),b.optDouble("odo"));
   if(!detail.isEmpty())sb.append("\n   ").append(detail);
   sb.append("\n");
  }
  if(notices.size()>5)sb.append("외 ").append(notices.size()-5).append("건 · 정비 기록에서 모두 확인\n");
  if(!b.optString("insuranceDate").isEmpty())sb.append("• 보험 만료: ").append(b.optString("insuranceDate")).append("\n");
  if(sb.length()==0)sb.append("예정된 정비 일정이 없습니다. 정비 기록에서 다음 점검 거리를 설정할 수 있어요.");
  new AlertDialog.Builder(a).setTitle("정비 알림").setMessage(sb.toString().trim()).setNegativeButton("닫기",null).setPositiveButton("정비 기록",(d,w)->a.navigate("차고")).show();
 }

 void render(){
  JSONObject b=a.bike();if(b==null){emptyGarage();return;}
  addHero(b);stats(b);a.gap(a.content,12);
  String condition=condition(b);
  if("점검 필요".equals(condition)||"곧 점검".equals(condition)){
   TextView alert=a.text(condition+" · 정비 일정 확인",13,a.fg,true);
   alert.setPadding(a.dp(15),a.dp(14),a.dp(15),a.dp(14));alert.setBackground(a.shape(a.highlight,a.line,12));
   alert.setClickable(true);alert.setFocusable(true);alert.setContentDescription(condition+" 정비 알림 열기");
   alert.setOnClickListener(v->showNotices());a.content.addView(alert);a.gap(a.content,10);
  }
  if(RideService.autoArmed&&!RideService.running){TextView arm=a.text("OBD2 자동 감지 대기 · 출발하면 기록 시작",13,orange,true);arm.setPadding(a.dp(12),a.dp(10),a.dp(12),a.dp(10));arm.setBackground(a.shape(surface,stroke,13));a.content.addView(arm);a.gap(a.content,10);}
  if(RideService.running){TextView live=a.text("GPS 기록 중 · "+RideService.status,13,orange,true);live.setPadding(a.dp(12),a.dp(10),a.dp(12),a.dp(10));live.setBackground(a.shape(surface,stroke,13));a.content.addView(live);a.gap(a.content,10);}
  rideAction();a.gap(a.content,10);interruptedRideShortcut();a.gap(a.content,14);quickActions();a.gap(a.content,20);insights(b);a.gap(a.content,20);
  listings();a.gap(a.content,16);friends();a.gap(a.content,18);recent(b);
 }
 void interruptedRideShortcut(){
  if(RideService.running)return;
  try{
   int broken=0;
   for(File track:TrackFile.tracks(a.getFilesDir()))
    if(RideHistoryFilter.INTERRUPTED.equals(RideHistoryFilter.status(track,RideService.active,false)))broken++;
   if(broken==0)return;
   final int count=broken;
   Button recover=a.button("중단된 GPS 기록 "+count+"건 · 복구 확인",false,()->{
    a.navigate("라이딩");a.selectRideHistoryFilter(RideHistoryFilter.INTERRUPTED);
   });
   recover.setContentDescription("중단된 GPS 기록 "+count+"건, 직접 확인 후 복구");a.content.addView(recover);
  }catch(Exception ignored){} // Home remains usable if storage is temporarily unavailable.
 }
 void emptyGarage(){
  LinearLayout blank=a.col();blank.setGravity(Gravity.CENTER);blank.setPadding(a.dp(24),a.dp(45),a.dp(24),a.dp(44));blank.setBackground(a.shape(surface,stroke,20));
  TextView intro=a.text("GARAGE",12,orange,true);intro.setLetterSpacing(.16f);blank.addView(intro);
  TextView big=a.text("첫 바이크 등록",24,light,true);LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-1,-2);bp.topMargin=a.dp(15);big.setGravity(Gravity.CENTER);blank.addView(big,bp);
  TextView hint=a.text("등록하면 이곳에 내 바이크 사진과 주행거리, 정비 상태가 자동으로 표시돼.",14,soft,false);hint.setGravity(Gravity.CENTER);LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(-1,-2);hp.topMargin=a.dp(12);blank.addView(hint,hp);
  Button add=a.button("바이크 등록",true,()->a.edit("vehicles",null));add.setTextColor(a.dark?0xFF18191B:Color.WHITE);LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(-1,a.dp(54));ap.topMargin=a.dp(24);blank.addView(add,ap);a.content.addView(blank);
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
  TextView tag=a.text(HomeBikeSwitch.label(bikeCount),11,Color.WHITE,true);tag.setGravity(Gravity.CENTER);tag.setLetterSpacing(.07f);tag.setPadding(a.dp(12),a.dp(7),a.dp(12),a.dp(7));tag.setBackground(a.shape(0xB91A1D21,0,7));FrameLayout.LayoutParams tp=new FrameLayout.LayoutParams(-2,-2,Gravity.TOP|Gravity.START);tp.setMargins(a.dp(15),a.dp(15),0,0);imageArea.addView(tag,tp);
  if(canSwitch){
   tag.setContentDescription("다른 바이크로 전환");tag.setClickable(true);tag.setFocusable(true);tag.setOnClickListener(v->new HubScreens(a).chooseBike());
   for(int direction:new int[]{-1,1}){
    TextView arrow=a.text(direction<0?"‹":"›",30,light,true);
    arrow.setGravity(Gravity.CENTER);arrow.setBackground(a.shape(0xAA15212B,0,12));
    arrow.setContentDescription(direction<0?"이전 바이크":"다음 바이크");
    arrow.setFocusable(true);arrow.setClickable(true);arrow.setOnClickListener(v->switchBike(direction));
    FrameLayout.LayoutParams ap=new FrameLayout.LayoutParams(a.dp(36),a.dp(50),Gravity.CENTER_VERTICAL|(direction<0?Gravity.LEFT:Gravity.RIGHT));
    ap.leftMargin=a.dp(5);ap.rightMargin=a.dp(5);imageArea.addView(arrow,ap);
   }
  }
  TextView photo=a.text("사진 변경",12,Color.WHITE,true);photo.setGravity(Gravity.CENTER);photo.setPadding(a.dp(10),a.dp(8),a.dp(10),a.dp(8));photo.setBackground(a.shape(0xB9121A20,0,9));photo.setContentDescription("내 바이크 사진 선택");FrameLayout.LayoutParams pp=new FrameLayout.LayoutParams(-2,-2,Gravity.TOP|Gravity.RIGHT);pp.setMargins(0,a.dp(15),a.dp(15),0);imageArea.addView(photo,pp);photo.setClickable(true);photo.setFocusable(true);photo.setOnClickListener(v->changeHeroPhoto(bike,own));
  TextView example=a.text(BikeVisual.caption(bike.optString("name"),own),10,0xFFD0DCE3,false);FrameLayout.LayoutParams ep=new FrameLayout.LayoutParams(-2,-2,Gravity.BOTTOM|Gravity.RIGHT);ep.setMargins(0,0,a.dp(15),a.dp(9));imageArea.addView(example,ep);
  if(!own){
   JSONObject known=MotorcycleCatalog.search(a,bike.optString("name"));
   String brand=bike.optString("catalogBrand",known==null?"":known.optString("brand"));
   String model=bike.optString("catalogModel",known==null?"":known.optString("model"));
   int year=bike.optInt("year");
   if(!brand.isEmpty()&&!model.isEmpty())MotorcyclePhoto.show(a,visual,example,brand,model,year,bike.optString("id"));
   else example.setText("모델 사진 미확인 · 사진 변경으로 직접 등록");
  }
  imageArea.setClickable(true);imageArea.setFocusable(true);
  imageArea.setContentDescription(canSwitch?"바이크 사진. 좌우로 밀면 다른 바이크, 탭하면 사진 관리":"바이크 사진. 탭하면 사진 관리");
  imageArea.setOnClickListener(v->changeHeroPhoto(bike,own));
  if(canSwitch)imageArea.setOnTouchListener(new View.OnTouchListener(){float downX,downY;
   @Override public boolean onTouch(View v,android.view.MotionEvent event){
    if(event.getActionMasked()==android.view.MotionEvent.ACTION_DOWN){downX=event.getX();downY=event.getY();return true;}
    if(event.getActionMasked()==android.view.MotionEvent.ACTION_UP){float dx=event.getX()-downX,dy=event.getY()-downY;
     if(Math.abs(dx)>a.dp(75)&&Math.abs(dx)>Math.abs(dy)*1.5){switchBike(dx<0?1:-1);return true;}
     v.performClick();return true;
    }
    if(event.getActionMasked()==android.view.MotionEvent.ACTION_MOVE){
     if(Math.abs(event.getY()-downY)>a.dp(18)&&Math.abs(event.getY()-downY)>Math.abs(event.getX()-downX)){
      v.getParent().requestDisallowInterceptTouchEvent(false);return false;
     }
     return true;
    }
    return false;
   }
  });
  box.addView(imageArea);
  LinearLayout meta=new LinearLayout(a);meta.setPadding(a.dp(17),a.dp(15),a.dp(14),a.dp(17));meta.setGravity(Gravity.CENTER_VERTICAL);
  LinearLayout info=a.col();TextView name=a.text(bike.optString("name"),24,light,true);name.setMaxLines(2);info.addView(name);
  TextView sub=a.text((bike.optInt("year")>0?bike.optInt("year")+"년식 · ":"")+"내 바이크",12,soft,false);LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,-2);sp.topMargin=a.dp(6);info.addView(sub,sp);meta.addView(info,new LinearLayout.LayoutParams(0,-2,1));
  if(GarageBikeStatus.sold(bike.optString("soldDate"))){
   TextView archived=a.text("판매 완료 · 기록 보관 중",13,0xFFFFD58A,true);archived.setPadding(a.dp(12),a.dp(10),a.dp(12),a.dp(10));archived.setBackground(a.shape(0xFF503E20,0,10));box.addView(archived);
  }
  String condition=condition(bike);TextView badge=a.text(condition,11,condition.equals("점검 필요")?0xFFFFBEAA:condition.equals("곧 점검")?0xFFFFD58A:condition.equals("정비 정상")?0xFF62E3AA:soft,true);badge.setPadding(a.dp(9),a.dp(9),a.dp(9),a.dp(9));badge.setBackground(a.shape(condition.equals("점검 필요")?0xFF502A20:condition.equals("곧 점검")?0xFF503E20:condition.equals("정비 정상")?0xFF153E30:0xFF27343F,0,11));meta.addView(badge);
  meta.setClickable(true);meta.setOnClickListener(v->new HubScreens(a).manageBike());meta.setContentDescription("바이크 상세 관리 열기");box.addView(meta);a.content.addView(box);
 }
 /** The hero photo control offers a real online gallery when a model is known. */
 void switchBike(int direction){
  List<JSONObject> bikes=a.items("vehicles");if(bikes.size()<2)return;
  String current=a.bike()==null?"":a.bike().optString("id");int index=0;
  for(int i=0;i<bikes.size();i++)if(current.equals(bikes.get(i).optString("id"))){index=i;break;}
  a.selected=bikes.get((index+direction+bikes.size())%bikes.size()).optString("id");
  a.getSharedPreferences("garage_ui",android.content.Context.MODE_PRIVATE).edit().putString("selected_bike",a.selected).apply();a.render();
 }
 /** Always let the owner choose which source is allowed to replace a personal photo. */
 void changeHeroPhoto(JSONObject bike,boolean manualPhoto){
  JSONObject known=MotorcycleCatalog.search(a,bike.optString("name"));
  String brand=bike.optString("catalogBrand",known==null?"":known.optString("brand"));
  String model=bike.optString("catalogModel",known==null?"":known.optString("model"));
  List<String> options=new ArrayList<>();options.add("휴대폰 사진 선택");
  boolean gallery=!brand.isEmpty()&&!model.isEmpty();
  if(gallery)options.add("모델·연식 사진 검색 (온라인 · 선택 적용)");
  if(manualPhoto)options.add("내 사진 제거 (기본 사진으로)");
  new AlertDialog.Builder(a).setTitle("바이크 사진 관리")
   .setItems(options.toArray(new String[0]),(d,choice)->{
    if(choice==0){a.chooseGaragePhoto();return;}
    if(gallery&&choice==1){MotorcyclePhotoGallery.open(a,brand,model,bike.optInt("year"));return;}
    new AlertDialog.Builder(a).setTitle("내 사진을 제거할까요?")
     .setMessage("휴대폰에 저장한 이 바이크의 대표 사진을 제거합니다. 다른 기록은 그대로 유지됩니다.")
     .setNegativeButton("취소",null).setPositiveButton("사진 제거",(dialog,which)->{
      File target=a.garagePhotoFile(bike.optString("id"));
      if(target!=null&&target.isFile()&&!target.delete()){a.error("사진을 제거하지 못했어요.");return;}
      a.render();
     }).show();
   }).setNegativeButton("닫기",null).show();
 }
 boolean loadBikePhoto(ImageView iv,JSONObject bike){
  File custom=a.garagePhotoFile(bike.optString("id"));
  if(custom!=null&&custom.isFile()){
   android.graphics.BitmapFactory.Options options=new android.graphics.BitmapFactory.Options();options.inJustDecodeBounds=true;android.graphics.BitmapFactory.decodeFile(custom.getAbsolutePath(),options);
   options.inJustDecodeBounds=false;options.inSampleSize=1;while(options.outWidth/options.inSampleSize>1024||options.outHeight/options.inSampleSize>1024)options.inSampleSize*=2;
   android.graphics.Bitmap photo;
   try{photo=android.graphics.BitmapFactory.decodeFile(custom.getAbsolutePath(),options);}
   catch(OutOfMemoryError memory){photo=null;}
   if(photo!=null){iv.setImageBitmap(photo);return true;}
  }
  iv.setImageResource(BikeVisual.superCub(bike.optString("name"))?R.drawable.garage_super_cub:R.drawable.garage_model_placeholder);return false;
 }
 List<JSONObject> latestServices(JSONObject b){
  // One consistent policy for home, notifications and the cost dashboard.
  return new ArrayList<>(LatestServiceByType.latest(a.items("services"),b.optString("id")).values());
 }
 String condition(JSONObject b){
  List<HomeMaintenanceStatus.Schedule> schedules=new ArrayList<>();
  for(JSONObject service:latestServices(b))
   schedules.add(new HomeMaintenanceStatus.Schedule(service.optString("nextDate"),service.optDouble("nextKm")));
  return HomeMaintenanceStatus.label(HomeMaintenanceStatus.summary(schedules,a.today(),b.optDouble("odo")));
 }
 String nextService(JSONObject b){
  String summary=condition(b);
  java.util.List<String> dates=new java.util.ArrayList<>();java.util.List<Double> kms=new java.util.ArrayList<>();
  for(JSONObject service:latestServices(b)){
   dates.add(service.optString("nextDate"));kms.add(service.optDouble("nextKm",Double.NaN));
  }
  String nearest=ServiceDueSummary.nearest(dates,kms,a.today(),b.optDouble("odo",Double.NaN));
  return nearest.equals("미설정")?summary:nearest;
 }
 int monthRides(){int count=0;String ym=a.today().substring(0,7);try{
  for(File f:TrackFile.tracks(a.getFilesDir())){
   try {String raw=f.getName().split("-")[0];long time=Long.parseLong(raw);String period=Instant.ofEpochMilli(time).atZone(ZoneId.of("Asia/Seoul")).toLocalDate().toString();if(period.startsWith(ym))count++;}
   catch(NumberFormatException|java.time.DateTimeException ignored){}
  }
 }catch(Exception ignored){}return count;
 }
 void stats(JSONObject b){
  String[] values={a.num(b.optDouble("odo"))+" km",nextService(b),monthRides()+"회"};
  String[] names={"총 주행거리","다음 정비","이번 달 라이딩"};
  LinearLayout panel=a.col();
  panel.setPadding(a.dp(16),a.dp(17),a.dp(16),a.dp(17));
  panel.setBackground(a.shape(surface,stroke,13));
  LinearLayout row=new LinearLayout(a);row.setGravity(Gravity.CENTER_VERTICAL);
  for(int i=0;i<3;i++){
   final int n=i;
   LinearLayout item=a.col();
   TextView label=a.text(names[i],11,soft,false);
   item.addView(label);
   TextView number=a.text(values[i],17,light,true);
   number.setMaxLines(1);
   number.setAutoSizeTextTypeUniformWithConfiguration(12,17,1,android.util.TypedValue.COMPLEX_UNIT_SP);
   LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(-1,a.dp(27));
   np.topMargin=a.dp(8);item.addView(number,np);
   if(n==1){item.setContentDescription("다음 정비 "+values[i]+" · 정비 기록 보기");item.setClickable(true);item.setFocusable(true);item.setOnClickListener(v->a.navigate("차고"));}
   LinearLayout.LayoutParams cell=new LinearLayout.LayoutParams(0,-2,1);row.addView(item,cell);
   if(i<2){View rule=new View(a);rule.setBackgroundColor(stroke);
    LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(a.dp(1),a.dp(39));rp.leftMargin=a.dp(10);rp.rightMargin=a.dp(12);row.addView(rule,rp);}
  }
  panel.addView(row);a.content.addView(panel);
 }
 void rideAction(){Button btn=a.button(RideService.autoArmed?"자동 감지 관리":RideService.running?"라이딩 종료":"라이딩 시작",true,()->{
  if(RideService.autoArmed){a.navigate("라이딩");a.toast("자동 감지는 라이딩 탭에서 중지할 수 있어요.");}
  else if(RideService.running){new AlertDialog.Builder(a).setTitle("주행 기록을 종료할까요?").setMessage("기록을 종료하면 현재 GPS 위치 수집을 중단합니다.").setNegativeButton("취소",null).setPositiveButton("종료",(d,w)->{a.startService(new Intent(a,RideService.class).setAction(RideService.STOP));a.toast("정상 종료를 요청했어요.");a.root.postDelayed(()->{if(!a.isFinishing())a.render();},750);}).show();}
  else {a.navigate("라이딩");new RideUi(a).requestStart();}
 });btn.setTextColor(RideService.running?Color.WHITE:(a.dark?0xFF18191B:Color.WHITE));btn.setTextSize(17);btn.setMinHeight(a.dp(56));btn.setBackground(a.shape(RideService.running?0xFFC34A43:orange,0,12));a.content.addView(btn,new LinearLayout.LayoutParams(-1,a.dp(56)));}
 void quickActions(){
  sectionTitle("빠른 실행",null,null);
  String[] labels={"주유 등록","정비 기록","주행 기록","바이크 관리"};
  String[] details={"주유 금액과 주행거리","정비 내역과 점검 주기","GPS 기록과 GPX","바이크 정보 변경"};
  Runnable[] clicks={()->a.edit("expenses",null),()->a.edit("services",null),
   ()->a.navigate("라이딩"),()->new HubScreens(a).manageBike()};
  LinearLayout panel=a.col();panel.setBackground(a.shape(surface,stroke,13));
  for(int i=0;i<labels.length;i++){
   final Runnable action=clicks[i];
   LinearLayout row=new LinearLayout(a);row.setPadding(a.dp(16),a.dp(13),a.dp(15),a.dp(13));
   row.setGravity(Gravity.CENTER_VERTICAL);row.setMinimumHeight(a.dp(69));
   TextView index=a.text(String.format(Locale.ROOT,"%02d",i+1),12,orange,true);
   index.setLetterSpacing(.08f);
   LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(a.dp(37),-2);row.addView(index,ip);
   LinearLayout text=a.col();text.addView(a.text(labels[i],15,light,true));
   TextView description=a.text(details[i],12,soft,false);
   LinearLayout.LayoutParams dp=new LinearLayout.LayoutParams(-1,-2);dp.topMargin=a.dp(5);text.addView(description,dp);
   row.addView(text,new LinearLayout.LayoutParams(0,-2,1));
   TextView chevron=a.text("›",23,soft,false);row.addView(chevron);
   row.setClickable(true);row.setFocusable(true);
   row.setContentDescription(labels[i]+" 열기");
   row.setOnClickListener(v->action.run());panel.addView(row);
   if(i<labels.length-1){View line=new View(a);line.setBackgroundColor(stroke);
    LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,a.dp(1));
    lp.leftMargin=a.dp(53);panel.addView(line,lp);}
  }
  a.content.addView(panel);
 }
 void sectionTitle(String title,String action,Runnable click){LinearLayout row=new LinearLayout(a);row.setGravity(Gravity.CENTER_VERTICAL);
  TextView t=a.text(title,17,light,true);row.addView(t,new LinearLayout.LayoutParams(0,-2,1));if(action!=null){TextView more=a.text(action,12,soft,true);more.setPadding(a.dp(8),a.dp(9),a.dp(3),a.dp(9));more.setOnClickListener(v->click.run());row.addView(more);}a.content.addView(row);a.gap(a.content,10);
 }
 void insights(JSONObject b){
  sectionTitle("MY BIKE INSIGHTS","기록 관리  ›",()->a.navigate("차고"));
  String month=a.today().substring(0,7);double spent=0;int expenseCount=0;
  for(String key:new String[]{"services","expenses"})for(JSONObject item:a.items(key))
   if(b.optString("id").equals(item.optString("vehicleId"))&&item.optString("date").startsWith(month)){
    double amount=item.optDouble(key.equals("services")?"cost":"amount");
    if(Double.isFinite(amount)&&amount>=0&&amount<1_000_000_000){spent+=amount;expenseCount++;}
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
  frame.addView(detail);
  String lastMonth=java.time.YearMonth.parse(month).minusMonths(1).toString();double previous=0;
  for(String key:new String[]{"services","expenses"})for(JSONObject item:a.items(key))
   if(b.optString("id").equals(item.optString("vehicleId"))&&item.optString("date").startsWith(lastMonth)){
    double value=item.optDouble(key.equals("services")?"cost":"amount");
    if(Double.isFinite(value)&&value>=0&&value<1_000_000_000)previous+=value;
   }
  TextView trend=a.text("지난달 "+a.won(previous)+"  ·  "+CostTrend.caption(spent,previous),12,soft,false);
  LinearLayout.LayoutParams tl=new LinearLayout.LayoutParams(-1,-2);tl.topMargin=a.dp(8);frame.addView(trend,tl);
  a.content.addView(frame);
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
 void friends(){sectionTitle("내 기록 · 기기 저장","열기  ›",()->a.navigate("더보기"));LinearLayout row=new LinearLayout(a);row.setPadding(a.dp(14),a.dp(16),a.dp(12),a.dp(16));row.setBackground(a.shape(surface,stroke,14));row.setGravity(Gravity.CENTER_VERTICAL);
  TextView people=a.text("♧",25,orange,true);row.addView(people,new LinearLayout.LayoutParams(a.dp(34),-2));
  LinearLayout details=a.col();details.addView(a.text("계정 없이 사용하는 MOTO HUB",14,light,true));TextView sub=a.text("내 기록은 이 휴대폰에 저장 · ZIP 백업 가능",11,soft,false);LinearLayout.LayoutParams sl=new LinearLayout.LayoutParams(-1,-2);sl.topMargin=a.dp(5);details.addView(sub,sl);row.addView(details,new LinearLayout.LayoutParams(0,-2,1));row.addView(a.text("›",22,orange,true));row.setClickable(true);row.setOnClickListener(v->a.navigate("더보기"));a.content.addView(row);
 }
 void recent(JSONObject b){sectionTitle("최근 기록","전체 보기  ›",()->a.navigate("차고"));List<JSONObject> r=new HubScreens(a).records("전체",b);
  if(r.isEmpty()){a.label(a.content,"주유·정비 기록을 남기면 여기에 표시돼.",13,soft,false);return;}
  int count=0;for(JSONObject x:r){if(count++>=2)break;
   LinearLayout row=new LinearLayout(a);row.setPadding(a.dp(12),a.dp(12),a.dp(12),a.dp(12));row.setGravity(Gravity.CENTER_VERTICAL);row.setBackground(a.shape(surface,0,12));LinearLayout details=a.col();details.addView(a.text(x.optString("type","기록"),13,light,true));TextView date=a.text(x.optString("date"),11,soft,false);LinearLayout.LayoutParams dp=new LinearLayout.LayoutParams(-1,-2);dp.topMargin=a.dp(3);details.addView(date,dp);row.addView(details,new LinearLayout.LayoutParams(0,-2,1));row.addView(a.text(a.won(x.optDouble(x.has("amount")?"amount":"cost")),13,orange,true));row.setOnClickListener(v->a.edit(x.has("amount")?"expenses":"services",x));row.setClickable(true);a.content.addView(row);a.gap(a.content,7);
  }
 }
}
