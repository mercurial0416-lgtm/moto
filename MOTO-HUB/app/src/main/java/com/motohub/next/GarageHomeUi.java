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
 private static final java.util.concurrent.ExecutorService HOME_IO=java.util.concurrent.Executors.newSingleThreadExecutor(r->{
  Thread t=new Thread(r,"moto-home-status");t.setDaemon(true);return t;
 });
 private static final java.util.concurrent.atomic.AtomicLong HOME_SCAN=new java.util.concurrent.atomic.AtomicLong();
 final MainActivity a;
 final int orange,light,soft,surface,card,stroke;
 GarageHomeUi(MainActivity a){this.a=a;
  orange=a.accent;light=a.fg;soft=a.muted;surface=a.panel;card=a.panel;stroke=a.line;
 }

 void showNotices(){
  JSONObject bike=a.bike();if(bike==null){a.toast("바이크를 먼저 등록해 주세요.");return;}
  List<JSONObject> services=latestServices(bike);
  services.sort((x,y)->Integer.compare(
   MaintenancePlanner.order(MaintenanceAlertWindow.status(x,a.today(),bike.optDouble("odo"))),
   MaintenancePlanner.order(MaintenanceAlertWindow.status(y,a.today(),bike.optDouble("odo")))));
  StringBuilder details=new StringBuilder();int overdue=0,soon=0,listed=0;
  for(JSONObject service:services){
   MaintenancePlanner.Status status=MaintenanceAlertWindow.status(service,a.today(),bike.optDouble("odo"));
   if(status==MaintenancePlanner.Status.OVERDUE)overdue++;
   else if(status==MaintenancePlanner.Status.SOON)soon++;
   if(service.optDouble("nextKm")<=0&&service.optString("nextDate").isEmpty())continue;
   if(listed++>=6)continue;
   details.append("• ").append(service.optString("type")).append(" · ")
    .append(MaintenancePlanner.caption(status)).append("\n  ")
    .append(MaintenanceNoticeText.detail(service.optString("nextDate"),
      service.optDouble("nextKm"),a.today(),bike.optDouble("odo"))).append("\n");
  }
  if(overdue>0||soon>0)details.insert(0,"점검 필요 "+overdue+"건 · 곧 정비 "+soon+"건\n\n");
  if(listed>6)details.append("외 ").append(listed-6).append("건\n");
  if(!bike.optString("insuranceDate").isEmpty())details.append("보험 만료: ").append(bike.optString("insuranceDate")).append("\n");
  if(details.length()==0)details.append("예정된 정비가 없어요. 차고에서 정비주기를 등록할 수 있습니다.");
  new AlertDialog.Builder(a).setTitle("정비 알림").setMessage(details.toString().trim())
   .setNegativeButton("닫기",null).setPositiveButton("정비 관리",(d,w)->a.navigate("차고")).show();
 }

 void render(){
  JSONObject b=a.bike();if(b==null){emptyGarage();return;}
  addHero(b);a.gap(a.content,8);stats(b);a.gap(a.content,18);
  quickActions();a.gap(a.content,10);quickNavigation();a.gap(a.content,10);rideAction();a.gap(a.content,13);
  String condition=condition(b);
  if("점검 필요".equals(condition)||"곧 점검".equals(condition)){
   TextView alert=a.text(condition+" · 정비 일정 확인",13,a.fg,true);
   alert.setPadding(a.dp(15),a.dp(14),a.dp(15),a.dp(14));alert.setBackground(a.shape(a.highlight,a.line,12));
   alert.setClickable(true);alert.setFocusable(true);alert.setContentDescription(condition+" 정비 알림 열기");
   alert.setOnClickListener(v->showNotices());a.content.addView(alert);a.gap(a.content,10);
  }
  if(RideService.autoArmed&&!RideService.running){TextView arm=a.text("OBD2 자동 감지 대기 · 출발하면 기록 시작",13,orange,true);arm.setPadding(a.dp(12),a.dp(10),a.dp(12),a.dp(10));arm.setBackground(a.shape(surface,stroke,13));a.content.addView(arm);a.gap(a.content,10);}
  if(RideService.running){TextView live=a.text("GPS 기록 중 · "+RideService.status,13,orange,true);live.setPadding(a.dp(12),a.dp(10),a.dp(12),a.dp(10));live.setBackground(a.shape(surface,stroke,13));a.content.addView(live);a.gap(a.content,10);}
  interruptedRideShortcut();a.gap(a.content,19);
  recent(b);a.gap(a.content,20);insights(b);
  if(!a.items("listings").isEmpty()){a.gap(a.content,20);listings();}
 }
 /** Scan local GPS metadata off the UI thread and ignore results for disposed home screens. */
 void interruptedRideShortcut(){
  if(RideService.running)return;
  final long generation=HOME_SCAN.incrementAndGet();
  final LinearLayout requestContent=a.content;
  final LinearLayout slot=a.col();
  requestContent.addView(slot);
  HOME_IO.execute(()->{
   int broken;
   try{
    if(generation!=HOME_SCAN.get())return;
    broken=RideHomeStatus.interrupted(a.getFilesDir()); // stream metadata; never sort 10k tracks
    if(generation!=HOME_SCAN.get())return;
   }catch(Exception ignored){return;} // A storage error must not stop the home screen.
   final int total=broken;
   a.runOnUiThread(()->{
    if(generation!=HOME_SCAN.get()||a.isFinishing()||a.isDestroyed()||
       !"홈".equals(a.tab)||a.content!=requestContent||slot.getParent()!=requestContent||
       RideService.running||total==0)return;
    Button recover=a.button("중단된 GPS 기록 "+total+"건 · 복구 확인",false,()->{
     a.navigate("라이딩");a.selectRideHistoryFilter(RideHistoryFilter.INTERRUPTED);
    });
    recover.setContentDescription("중단된 GPS 기록 "+total+"건, 직접 확인 후 복구");
    slot.addView(recover);
   });
  });
 }
 void emptyGarage(){
  LinearLayout blank=a.col();blank.setGravity(Gravity.CENTER);blank.setPadding(a.dp(24),a.dp(45),a.dp(24),a.dp(44));blank.setBackground(a.shape(surface,stroke,8));
  TextView intro=a.text("내 차고",12,orange,true);intro.setLetterSpacing(.16f);blank.addView(intro);
  TextView big=a.text("첫 바이크 등록",24,light,true);LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-1,-2);bp.topMargin=a.dp(15);big.setGravity(Gravity.CENTER);blank.addView(big,bp);
  TextView hint=a.text("바이크를 등록하면 주행거리와 정비 기록을 볼 수 있어.",14,soft,false);hint.setGravity(Gravity.CENTER);LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(-1,-2);hp.topMargin=a.dp(12);blank.addView(hint,hp);
  Button add=a.button("바이크 등록",true,()->a.edit("vehicles",null));add.setTextColor(a.dark?0xFF18191B:Color.WHITE);LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(-1,a.dp(54));ap.topMargin=a.dp(24);blank.addView(add,ap);a.content.addView(blank);
 }

 void addHero(JSONObject bike){
  LinearLayout box=a.col();box.setBackground(a.shape(surface,stroke,8));box.setClipToOutline(true);
  FrameLayout imageArea=new FrameLayout(a);imageArea.setBackgroundColor(a.dark?0xFF141619:0xFFEDECEA);int displayWidth=a.getResources().getDisplayMetrics().widthPixels;
  int photoHeight=displayWidth<a.dp(390)?190:211;
  imageArea.setLayoutParams(new LinearLayout.LayoutParams(-1,a.dp(photoHeight)));
  ImageView visual=new ImageView(a);visual.setScaleType(ImageView.ScaleType.FIT_CENTER);
  boolean own=loadBikePhoto(visual,bike);
  imageArea.addView(visual,new FrameLayout.LayoutParams(-1,-1));
  View shade=new View(a);shade.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
   new int[]{0x00000000,0x00000000,0x58000000}));imageArea.addView(shade,new FrameLayout.LayoutParams(-1,-1));
  int bikeCount=a.items("vehicles").size();
  boolean canSwitch=HomeBikeSwitch.visible(bikeCount);
  if(canSwitch){
   for(int direction:new int[]{-1,1}){
    TextView arrow=a.text(direction<0?"‹":"›",30,light,true);
    arrow.setGravity(Gravity.CENTER);arrow.setBackground(a.shape(0xAA15212B,0,6));
    arrow.setContentDescription(direction<0?"이전 바이크":"다음 바이크");
    arrow.setFocusable(true);arrow.setClickable(true);arrow.setOnClickListener(v->switchBike(direction));
    FrameLayout.LayoutParams ap=new FrameLayout.LayoutParams(a.dp(36),a.dp(50),Gravity.CENTER_VERTICAL|(direction<0?Gravity.LEFT:Gravity.RIGHT));
    ap.leftMargin=a.dp(5);ap.rightMargin=a.dp(5);imageArea.addView(arrow,ap);
   }
  }
  TextView photo=a.text("사진 변경",12,Color.WHITE,true);photo.setGravity(Gravity.CENTER);photo.setPadding(a.dp(12),a.dp(9),a.dp(12),a.dp(9));photo.setBackground(a.shape(0xB51B1C1F,0,6));photo.setContentDescription("내 바이크 사진 선택");FrameLayout.LayoutParams pp=new FrameLayout.LayoutParams(-2,-2,Gravity.TOP|Gravity.RIGHT);pp.setMargins(0,a.dp(15),a.dp(15),0);imageArea.addView(photo,pp);photo.setClickable(true);photo.setFocusable(true);photo.setOnClickListener(v->changeHeroPhoto(bike,own));
  TextView example=a.text(BikeVisual.caption(bike.optString("name"),own),11,0xFFF6F7F7,false);example.setPadding(a.dp(5),a.dp(3),a.dp(5),a.dp(3));example.setBackground(a.shape(0xB21B1E22,0,4));example.setMaxLines(2);FrameLayout.LayoutParams ep=new FrameLayout.LayoutParams(-2,-2,Gravity.BOTTOM|Gravity.RIGHT);ep.setMargins(0,0,a.dp(15),a.dp(9));imageArea.addView(example,ep);
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
  LinearLayout meta=a.col();meta.setPadding(a.dp(15),a.dp(13),a.dp(13),a.dp(14));
  LinearLayout info=a.col();TextView name=a.text(bike.optString("name"),21,light,true);name.setMaxLines(2);name.setEllipsize(android.text.TextUtils.TruncateAt.END);info.addView(name);
  TextView sub=a.text(bike.optInt("year")>0?bike.optInt("year")+"년식":"모델 정보",12,soft,false);LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,-2);sp.topMargin=a.dp(6);info.addView(sub,sp);meta.addView(info,new LinearLayout.LayoutParams(-1,-2));
  if(GarageBikeStatus.sold(bike.optString("soldDate"))){
   TextView archived=a.text("판매 완료 · 기록 보관 중",13,0xFFFFD58A,true);archived.setPadding(a.dp(12),a.dp(10),a.dp(12),a.dp(10));archived.setBackground(a.shape(0xFF503E20,0,10));box.addView(archived);
  }
  // Compact status instead of multicolored health badges.
  String condition=condition(bike);
  int statusColor=("점검 필요".equals(condition)||"곧 점검".equals(condition))?orange:soft;
  LinearLayout statusRow=new LinearLayout(a);statusRow.setGravity(Gravity.CENTER_VERTICAL);
  TextView state=a.text("●  "+condition,12,statusColor,true);
  state.setPadding(0,a.dp(9),a.dp(8),a.dp(9));
  statusRow.addView(state,new LinearLayout.LayoutParams(0,-2,1));
  if(canSwitch){
   TextView picker=a.text("전환",12,orange,true);
   picker.setGravity(Gravity.CENTER);picker.setPadding(a.dp(6),a.dp(11),a.dp(4),a.dp(11));
   picker.setMinHeight(a.dp(44));picker.setContentDescription("다른 바이크 선택");
   picker.setClickable(true);picker.setFocusable(true);
   picker.setOnClickListener(v->new HubScreens(a).chooseBike());
   statusRow.addView(picker);
  }
  meta.addView(statusRow);meta.setClickable(true);meta.setOnClickListener(v->new HubScreens(a).manageBike());meta.setContentDescription("바이크 상세 관리 열기");box.addView(meta);a.content.addView(box);
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
  MaintenancePlanner.Status worst=MaintenancePlanner.Status.NONE;
  for(JSONObject service:latestServices(b)){
   MaintenancePlanner.Status now=MaintenanceAlertWindow.status(service,a.today(),b.optDouble("odo"));
   if(MaintenancePlanner.order(now)<MaintenancePlanner.order(worst))worst=now;
  }
  return HomeMaintenanceStatus.label(worst);
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
  String[] values={a.num(b.optDouble("odo"))+" km",nextService(b)};
  String[] names={"계기판 거리","다음 정비"};
  LinearLayout row=new LinearLayout(a);row.setPadding(a.dp(2),a.dp(11),a.dp(2),a.dp(11));
  if(a.getResources().getConfiguration().fontScale>=1.35f){row.setOrientation(LinearLayout.VERTICAL);}
  for(int i=0;i<2;i++){
   final int index=i;
   LinearLayout group=a.col();group.setMinimumHeight(a.dp(49));
   TextView label=a.text(names[i],12,soft,false);group.addView(label);
   TextView value=i==0?MotoDesign.figure(a,values[i],22,light):a.text(values[i],18,light,true);
   value.setMaxLines(2);value.setMinHeight(a.dp(30));
   value.setAutoSizeTextTypeUniformWithConfiguration(14,i==0?22:18,1,android.util.TypedValue.COMPLEX_UNIT_SP);
   LinearLayout.LayoutParams vp=new LinearLayout.LayoutParams(-1,-2);vp.topMargin=a.dp(6);group.addView(value,vp);
   if(index==1){group.setClickable(true);group.setFocusable(true);group.setContentDescription("다음 정비 "+values[i]+" · 정비 내역 보기");group.setOnClickListener(v->a.navigate("차고"));}
   if(row.getOrientation()==LinearLayout.VERTICAL){
    row.addView(group,new LinearLayout.LayoutParams(-1,-2));
    if(i==0){View separator=new View(a);separator.setBackgroundColor(stroke);
     LinearLayout.LayoutParams bar=new LinearLayout.LayoutParams(-1,a.dp(1));bar.setMargins(0,a.dp(9),0,a.dp(11));row.addView(separator,bar);}
   }else{
    row.addView(group,new LinearLayout.LayoutParams(0,-2,1));
    if(i==0){View separator=new View(a);separator.setBackgroundColor(stroke);
     LinearLayout.LayoutParams bar=new LinearLayout.LayoutParams(a.dp(1),a.dp(42));bar.setMargins(a.dp(10),0,a.dp(19),0);row.addView(separator,bar);}
   }
  }
  a.content.addView(row);
 }
 void rideAction(){
  // Keep consent and stop confirmation; idle GPS action is deliberately secondary.
  boolean recording=RideService.running, armed=RideService.autoArmed;
  String title=armed?"OBD 자동 감지 관리":recording?"GPS 기록 종료":"GPS 기록 시작";
  Button btn=a.button(title,false,()->{
   if(RideService.autoArmed){
    a.navigate("라이딩");a.toast("자동 감지는 라이딩 탭에서 중지할 수 있어요.");
   }else if(RideService.running){
    new AlertDialog.Builder(a).setTitle("주행 기록을 종료할까요?")
     .setMessage("기록을 종료하면 현재 GPS 위치 수집을 중단합니다.")
     .setNegativeButton("취소",null)
     .setPositiveButton("종료",(d,w)->{
      a.startService(new Intent(a,RideService.class).setAction(RideService.STOP));
      a.toast("정상 종료를 요청했어요.");
      a.root.postDelayed(()->{if(!a.isFinishing())a.render();},750);
     }).show();
   }else{a.navigate("라이딩");new RideUi(a).requestStart();}
  });
  btn.setTextColor(recording?(a.dark?0xFFFFC5BE:0xFF8F352E):light);btn.setTextSize(14);btn.setMinHeight(a.dp(48));
  btn.setBackground(a.shape(recording?(a.dark?0xFF372422:0xFFFCE8E6):surface,
   recording?(a.dark?0xFFAD615B:0xFFD99792):stroke,8));
  a.content.addView(btn,new LinearLayout.LayoutParams(-1,-2));
  if(!recording&&!armed){
   Button go=a.button("원터치 출발 · GPS + 기본 내비",true,()->{
    a.navigate("라이딩");
    new RideUi(a).requestStart(true);
   });
   go.setMinHeight(a.dp(54));
   go.setContentDescription("GPS 기록에 동의한 뒤 시작하고, 기록이 시작되면 기본 내비 실행");
   LinearLayout.LayoutParams gp=new LinearLayout.LayoutParams(-1,-2);gp.topMargin=a.dp(8);
   a.content.addView(go,gp);
  }
 }
 void quickNavigation(){
  NavigationChoice preferred=NavigationApps.preferred(a);
  LinearLayout row=new LinearLayout(a);
  row.setGravity(Gravity.CENTER_VERTICAL);
  boolean narrow=a.getResources().getConfiguration().fontScale>=1.35f||
   a.getResources().getConfiguration().screenWidthDp<360;
  if(narrow)row.setOrientation(LinearLayout.VERTICAL);
  Button open=a.button("내비 실행 · "+preferred.label,false,()->NavigationApps.openPreferred(a));
  open.setMinHeight(a.dp(52));
  open.setContentDescription("기본 내비 "+preferred.label+" 실행, 라이딩 기록 유지");
  row.addView(open,narrow?new LinearLayout.LayoutParams(-1,-2):new LinearLayout.LayoutParams(0,-2,1));
  Button settings=a.button("내비 변경",false,()->NavigationApps.choose(a));
  settings.setMinHeight(a.dp(52));
  LinearLayout.LayoutParams lp=narrow?new LinearLayout.LayoutParams(-1,-2):new LinearLayout.LayoutParams(0,-2,1);
  if(narrow)lp.topMargin=a.dp(8);else lp.leftMargin=a.dp(8);
  row.addView(settings,lp);
  a.content.addView(row);
 }
 void quickActions(){
  sectionTitle("빠른 기록",null,null);
  LinearLayout entries=new LinearLayout(a);entries.setGravity(Gravity.CENTER_VERTICAL);boolean narrow=a.getResources().getConfiguration().fontScale>=1.35f||a.getResources().getConfiguration().screenWidthDp<360;if(narrow)entries.setOrientation(LinearLayout.VERTICAL);
  Button fuel=a.button("주유 기록",true,()->new QuickEntryUi(a).fuel());
  fuel.setMinHeight(a.dp(49));fuel.setBackground(a.shape(orange,0,8));
  Button maintenance=a.button("정비주기",false,()->new QuickEntryUi(a).maintenance());
  maintenance.setMinHeight(a.dp(49));maintenance.setBackground(a.shape(surface,stroke,8));
  LinearLayout.LayoutParams first=narrow?new LinearLayout.LayoutParams(-1,-2):new LinearLayout.LayoutParams(0,-2,1);
  if(narrow)first.bottomMargin=a.dp(8);else first.rightMargin=a.dp(8);entries.addView(fuel,first);
  entries.addView(maintenance,narrow?new LinearLayout.LayoutParams(-1,-2):new LinearLayout.LayoutParams(0,-2,1));
  a.content.addView(entries);
  LinearLayout links=new LinearLayout(a);links.setGravity(Gravity.CENTER_VERTICAL);if(narrow)links.setOrientation(LinearLayout.VERTICAL);
  TextView history=a.text("주행 내역  ›",13,soft,true);
  history.setGravity(Gravity.CENTER_VERTICAL);history.setMinHeight(a.dp(46));
  history.setClickable(true);history.setFocusable(true);
  history.setContentDescription("라이딩 기록 보기");
  history.setOnClickListener(v->a.navigate("라이딩"));
  TextView edit=a.text("바이크 정보  ›",13,soft,true);
  edit.setGravity(Gravity.CENTER_VERTICAL|Gravity.RIGHT);edit.setMinHeight(a.dp(46));
  edit.setClickable(true);edit.setFocusable(true);
  edit.setContentDescription("바이크 정보 수정");
  edit.setOnClickListener(v->new HubScreens(a).manageBike());
  links.addView(history,narrow?new LinearLayout.LayoutParams(-1,-2):new LinearLayout.LayoutParams(0,-2,1));
  links.addView(edit,narrow?new LinearLayout.LayoutParams(-1,-2):new LinearLayout.LayoutParams(0,-2,1));
  a.content.addView(links);
 }
 void sectionTitle(String title,String action,Runnable click){LinearLayout row=new LinearLayout(a);row.setGravity(Gravity.CENTER_VERTICAL);
  TextView t=a.text(title,17,light,true);row.addView(t,new LinearLayout.LayoutParams(0,-2,1));if(action!=null){TextView more=a.text(action,12,soft,true);more.setPadding(a.dp(8),a.dp(9),a.dp(3),a.dp(9));more.setOnClickListener(v->click.run());row.addView(more);}a.content.addView(row);a.gap(a.content,10);
 }
 void insights(JSONObject b){
  sectionTitle("유지비와 연비","기록 관리  ›",()->a.navigate("차고"));
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
  if(a.getResources().getConfiguration().fontScale>=1.35f)cells.setOrientation(LinearLayout.VERTICAL);
  String[] title={"이번 달 유지비","실측 연비"};
  String[] values={a.won(spent),segments>0?String.format(Locale.KOREA,"%.1f km/L",efficiency):"기록 부족"};
  for(int i=0;i<2;i++){
   LinearLayout tile=a.col();tile.addView(a.text(title[i],12,soft,false));
   TextView value=MotoDesign.figure(a,values[i],i==0?19:18,light);value.setMaxLines(2);value.setAutoSizeTextTypeUniformWithConfiguration(14,i==0?19:18,1,android.util.TypedValue.COMPLEX_UNIT_SP);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=a.dp(7);tile.addView(value,lp);
   LinearLayout.LayoutParams size=cells.getOrientation()==LinearLayout.VERTICAL?new LinearLayout.LayoutParams(-1,-2):new LinearLayout.LayoutParams(0,-2,1);if(i==1){if(cells.getOrientation()==LinearLayout.VERTICAL)size.topMargin=a.dp(12);else size.leftMargin=a.dp(11);}cells.addView(tile,size);
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
   LinearLayout row=new LinearLayout(a);row.setPadding(a.dp(12),a.dp(12),a.dp(12),a.dp(12));row.setGravity(Gravity.CENTER_VERTICAL);row.setMinimumHeight(a.dp(58));LinearLayout details=a.col();details.addView(a.text(x.optString("type","기록"),15,light,true));TextView date=a.text(x.optString("date"),12,soft,false);LinearLayout.LayoutParams dp=new LinearLayout.LayoutParams(-1,-2);dp.topMargin=a.dp(3);details.addView(date,dp);row.addView(details,new LinearLayout.LayoutParams(0,-2,1));TextView money=MotoDesign.figure(a,a.won(x.optDouble(x.has("amount")?"amount":"cost")),16,light);money.setMaxLines(2);money.setGravity(Gravity.END|Gravity.CENTER_VERTICAL);money.setPadding(a.dp(10),0,0,0);row.addView(money);row.setOnClickListener(v->a.edit(x.has("amount")?"expenses":"services",x));row.setClickable(true);a.content.addView(row);View rule=new View(a);rule.setBackgroundColor(stroke);a.content.addView(rule,new LinearLayout.LayoutParams(-1,a.dp(1)));
  }
 }
}
