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
 // Hero images must not wait behind a large interrupted-GPS scan.
 private static final java.util.concurrent.ExecutorService HERO_PHOTO_IO=java.util.concurrent.Executors.newSingleThreadExecutor(r->{
  Thread t=new Thread(r,"moto-home-photo");t.setDaemon(true);return t;
 });
 private static final java.util.concurrent.atomic.AtomicLong HOME_SCAN=new java.util.concurrent.atomic.AtomicLong();
 // At most 8 MiB of decoded, sized-down photos; never decode full camera images on UI.
 private static final android.util.LruCache<String,android.graphics.Bitmap> HERO_CACHE=
  new android.util.LruCache<String,android.graphics.Bitmap>(8*1024*1024){
   @Override protected int sizeOf(String key,android.graphics.Bitmap value){return value.getByteCount();}
  };
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
   .setNegativeButton("닫기",null).setNeutralButton("정비 완료",(d,w)->chooseCompletion(bike))
   .setPositiveButton("정비 관리",(d,w)->a.navigate("차고")).show();
 }

 /** Home reminder flows into the existing validated service completion editor. */
 void chooseCompletion(JSONObject bike){
  if(a.readOnly||RideService.running){a.toast("GPS 기록이 끝나고 저장할 수 있을 때 정비 완료를 입력해 주세요.");return;}
  List<JSONObject> options=latestServices(bike);
  options.removeIf(item->item.optDouble("nextKm")<=0&&item.optString("nextDate").isEmpty());
  if(options.isEmpty()){a.toast("완료할 정비주기가 없어요. 먼저 주기를 등록해 주세요.");return;}
  String[] names=new String[options.size()];
  for(int i=0;i<options.size();i++)
   names[i]=options.get(i).optString("type")+" · "+
    MaintenancePlanner.caption(MaintenanceAlertWindow.status(options.get(i),a.today(),bike.optDouble("odo")));
  new AlertDialog.Builder(a).setTitle("완료한 정비 항목 선택")
   .setItems(names,(d,i)->new ServiceStatusUi(a).complete(options.get(i)))
   .setNegativeButton("취소",null).show();
 }

 void render(){
  JSONObject b=a.bike();if(b==null){emptyGarage();return;}
  addHero(b);a.gap(a.content,12);stats(b);a.gap(a.content,18);
  TextView shortcutHeading=a.text("빠른 실행",16,light,true);
  LinearLayout.LayoutParams headingLp=new LinearLayout.LayoutParams(-1,-2);
  headingLp.bottomMargin=a.dp(10);a.content.addView(shortcutHeading,headingLp);
  // Keep user-selected order and visibility; combine only adjacent fuel/service
  // shortcuts when text size and screen width permit a safe two-column layout.
  String[] ordered=HomeShortcutOrder.parse(a.getSharedPreferences("home_shortcuts",0).getString("order",""));
  boolean narrow=a.getResources().getConfiguration().fontScale>=1.35f||
    a.getResources().getConfiguration().screenWidthDp<360;
  for(int i=0;i<ordered.length;i++){
   String key=ordered[i];
   if(!shortcut(key))continue;
   if(!narrow&&i+1<ordered.length&&shortcut(ordered[i+1])&&
      (("fuel".equals(key)&&"service".equals(ordered[i+1]))||
       ("service".equals(key)&&"fuel".equals(ordered[i+1])))){
    quickActionPair(key,ordered[i+1]);i++;
   }else switch(key){
    case "ride":rideAction();break;
    case "nav":quickNavigation();break;
    case "fuel":quickAction("fuel");break;
    case "service":quickAction("service");break;
   }
   a.gap(a.content,8);
  }
  // Home shortcut editing lives under Advanced Settings to keep the riding screen focused.
  String condition=condition(b);
  if("점검 필요".equals(condition)||"곧 점검".equals(condition)){
   TextView alert=a.text(condition+" · 정비 일정 확인",13,a.fg,true);
   alert.setPadding(a.dp(15),a.dp(14),a.dp(15),a.dp(14));alert.setBackground(a.shape(a.highlight,a.line,12));
   alert.setClickable(true);alert.setFocusable(true);alert.setContentDescription(condition+" 정비 알림 열기");
   alert.setOnClickListener(v->showNotices());a.content.addView(alert);a.gap(a.content,10);
  }
  if(RideService.autoArmed&&!RideService.running){TextView arm=a.text("OBD2 자동 감지 대기 · 출발하면 기록 시작",13,orange,true);arm.setPadding(a.dp(12),a.dp(10),a.dp(12),a.dp(10));arm.setBackground(a.shape(surface,stroke,13));a.content.addView(arm);a.gap(a.content,10);}
  if(RideService.running){TextView live=a.text("GPS 기록 중 · "+RideService.status,13,orange,true);live.setPadding(a.dp(12),a.dp(10),a.dp(12),a.dp(10));live.setBackground(a.shape(surface,stroke,13));a.content.addView(live);a.gap(a.content,10);}
  interruptedRideShortcut();a.gap(a.content,8);
  if(UndoLastSave.available(a)){
   Button undo=a.button("↶ 마지막 저장 실행 취소 · 2분",false,()->UndoLastSave.undo(a));
   undo.setContentDescription("방금 저장한 기록 실행 취소");
   a.content.addView(undo);a.gap(a.content,5);
  }
  a.action(a.content,UndoLastSave.available(a)?"빠른 도구 · 저장 취소 가능":"빠른 도구 · 주차·기록·소모품",false,()->showQuickTools(b));
  a.gap(a.content,6);
  // Recent activity is optional rather than always visible on the first screen.
  // Keep daily home focused on navigation and maintenance; secondary cards opt-in.
  boolean extra=a.getSharedPreferences("garage_home",0).getBoolean("extra_cards",false);
  Button more=a.button(extra?"활동·유지비 접기":"활동·유지비 더 보기",false,()->{
   a.getSharedPreferences("garage_home",0).edit().putBoolean("extra_cards",!extra).apply();
   a.renderPreservingPosition();
  });
  more.setMinHeight(a.dp(44));a.content.addView(more);
  // Manual access survives suppressed popups and repeated rides.
  if(!RideCompletionInbox.entries(a).isEmpty()){
   a.action(a.content,"최근 라이딩 저장 요약",false,a::showRideCompletionHistory);
  }
  if(extra){a.gap(a.content,16);recent(b);a.gap(a.content,16);insights(b);

  }
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
  LinearLayout blank=a.col();blank.setGravity(Gravity.CENTER);blank.setPadding(a.dp(24),a.dp(45),a.dp(24),a.dp(44));blank.setBackground(a.shape(surface,stroke,16));
  TextView intro=a.text("내 차고",12,orange,true);intro.setLetterSpacing(.16f);blank.addView(intro);
  TextView big=a.text("첫 바이크 등록",24,light,true);LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-1,-2);bp.topMargin=a.dp(15);big.setGravity(Gravity.CENTER);blank.addView(big,bp);
  TextView hint=a.text("바이크를 등록하면 주행거리와 정비 기록을 볼 수 있어.",14,soft,false);hint.setGravity(Gravity.CENTER);LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(-1,-2);hp.topMargin=a.dp(12);blank.addView(hint,hp);
  Button add=a.button("바이크 등록",true,()->a.edit("vehicles",null));add.setTextColor(a.dark?0xFF18191B:Color.WHITE);LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(-1,a.dp(54));ap.topMargin=a.dp(24);blank.addView(add,ap);a.content.addView(blank);
 }

 void addHero(JSONObject bike){
  LinearLayout box=a.col();box.setBackground(a.shape(surface,stroke,18));box.setClipToOutline(true);
  FrameLayout imageArea=new FrameLayout(a);imageArea.setBackgroundColor(a.dark?0xFF141619:0xFFEDECEA);int displayWidth=a.getResources().getDisplayMetrics().widthPixels;
  int photoHeight=displayWidth<a.dp(390)?190:211;
  imageArea.setLayoutParams(new LinearLayout.LayoutParams(-1,a.dp(photoHeight)));
  ImageView visual=new ImageView(a);visual.setScaleType(ImageView.ScaleType.FIT_CENTER);
  // Credit text exists before the asynchronous load so a failed personal image can fall back safely.
  TextView example=a.text("사진 확인 중…",11,0xFFF6F7F7,false);
  boolean own=loadBikePhoto(visual,bike,example);
  imageArea.addView(visual,new FrameLayout.LayoutParams(-1,-1));
  View shade=new View(a);shade.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
   new int[]{0x00000000,0x00000000,0x58000000}));imageArea.addView(shade,new FrameLayout.LayoutParams(-1,-1));
  int bikeCount=a.items("vehicles").size();
  boolean canSwitch=HomeBikeSwitch.visible(bikeCount);
  if(canSwitch){
   for(int direction:new int[]{-1,1}){
    TextView arrow=a.text(direction<0?"‹":"›",30,light,true);
    arrow.setGravity(Gravity.CENTER);arrow.setBackground(a.shape(0xAA15212B,0,12));
    arrow.setContentDescription(direction<0?"이전 바이크":"다음 바이크");
    arrow.setFocusable(true);arrow.setClickable(true);arrow.setOnClickListener(v->switchBike(direction));
    FrameLayout.LayoutParams ap=new FrameLayout.LayoutParams(a.dp(48),a.dp(54),Gravity.CENTER_VERTICAL|(direction<0?Gravity.LEFT:Gravity.RIGHT));
    ap.leftMargin=a.dp(5);ap.rightMargin=a.dp(5);imageArea.addView(arrow,ap);
   }
  }
  TextView photo=a.text("사진 변경",12,Color.WHITE,true);photo.setGravity(Gravity.CENTER);photo.setPadding(a.dp(12),a.dp(9),a.dp(12),a.dp(9));photo.setBackground(a.shape(0xB51B1C1F,0,12));photo.setMinHeight(a.dp(44));photo.setContentDescription("내 바이크 사진 선택");FrameLayout.LayoutParams pp=new FrameLayout.LayoutParams(-2,-2,Gravity.TOP|Gravity.RIGHT);pp.setMargins(0,a.dp(15),a.dp(15),0);imageArea.addView(photo,pp);photo.setClickable(true);photo.setFocusable(true);photo.setOnClickListener(v->changeHeroPhoto(bike,own));
  example.setText(BikeVisual.caption(bike.optString("name"),own));example.setPadding(a.dp(5),a.dp(3),a.dp(5),a.dp(3));example.setBackground(a.shape(0xB21B1E22,0,4));example.setMaxLines(2);FrameLayout.LayoutParams ep=new FrameLayout.LayoutParams(-2,-2,Gravity.BOTTOM|Gravity.RIGHT);ep.setMargins(0,0,a.dp(15),a.dp(9));imageArea.addView(example,ep);
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
 boolean loadBikePhoto(ImageView iv,JSONObject bike,TextView credit){
  File custom=a.garagePhotoFile(bike.optString("id"));
  boolean cub=BikeVisual.superCub(bike.optString("name"));
  iv.setImageResource(cub?R.drawable.garage_super_cub:R.drawable.garage_model_placeholder);
  if(custom==null||!custom.isFile())return false;
  // A user photo takes priority even while it is decoding asynchronously.
  // Binding this request to one image and bike stops rapid switching from showing a stale photo.
  final String bikeId=bike.optString("id");
  final String key=custom.getAbsolutePath()+":"+custom.length()+":"+custom.lastModified();
  iv.setTag(key);
  android.graphics.Bitmap cached=HERO_CACHE.get(key);
  if(cached!=null&&!cached.isRecycled()){iv.setImageBitmap(cached);return true;}
  HERO_PHOTO_IO.execute(()->{
   android.graphics.Bitmap loaded=MotorcyclePhoto.decodeLocalSampled(custom,1024);
   if(loaded!=null)HERO_CACHE.put(key,loaded);
   a.runOnUiThread(()->{
    if(a.isFinishing()||a.isDestroyed()||
       !key.equals(iv.getTag())||a.bike()==null||
       !bikeId.equals(a.bike().optString("id")))return;
    if(loaded!=null){iv.setImageBitmap(loaded);return;}
    // Keep the user's unreadable image untouched; show a matched model-photo candidate instead.
    JSONObject known=MotorcycleCatalog.search(a,bike.optString("name"));
    String brand=bike.optString("catalogBrand",known==null?"":known.optString("brand"));
    String model=bike.optString("catalogModel",known==null?"":known.optString("model"));
    if(!brand.isEmpty()&&!model.isEmpty()){
     MotorcyclePhoto.showFallback(a,iv,credit,brand,model,bike.optInt("year"),bikeId);
    }else{
     iv.setImageResource(cub?R.drawable.garage_super_cub:R.drawable.garage_model_placeholder);
     credit.setText("내 사진 손상 · 사진 변경에서 다시 선택");
    }
   });
  });
  return true;
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
 /** Clear hierarchy: verified dashboard ODO, next service, separate GPS estimate. */
 void stats(JSONObject b){
  String[] values={a.num(b.optDouble("odo"))+" km",nextService(b)};
  String[] names={"계기판 누적거리","다음 정비"};
  LinearLayout surfaceCard=a.col();
  surfaceCard.setPadding(a.dp(16),a.dp(15),a.dp(16),a.dp(13));
  surfaceCard.setBackground(a.shape(surface,stroke,16));
  LinearLayout row=new LinearLayout(a);row.setGravity(Gravity.CENTER_VERTICAL);
  boolean narrow=a.getResources().getConfiguration().fontScale>=1.35f||
    a.getResources().getConfiguration().screenWidthDp<360;
  if(narrow)row.setOrientation(LinearLayout.VERTICAL);
  for(int i=0;i<2;i++){
   final int index=i;
   LinearLayout group=a.col();group.setGravity(Gravity.CENTER_VERTICAL);
   group.setPadding(a.dp(7),a.dp(8),a.dp(7),a.dp(8));group.setMinimumHeight(a.dp(73));
   group.addView(a.text(names[i],12,soft,false));
   TextView value=i==0?MotoDesign.figure(a,values[i],24,light):a.text(values[i],18,light,true);
   value.setMaxLines(2);
   value.setAutoSizeTextTypeUniformWithConfiguration(14,i==0?24:18,1,android.util.TypedValue.COMPLEX_UNIT_SP);
   LinearLayout.LayoutParams vp=new LinearLayout.LayoutParams(-1,-2);
   vp.topMargin=a.dp(7);group.addView(value,vp);
   group.setClickable(true);group.setFocusable(true);
   if(index==0){
    group.setContentDescription("계기판 누적거리 "+values[i]+" · 직접 갱신");
    group.setOnClickListener(v->new ServiceStatusUi(a).updateOdo(b));
   }else{
    group.setContentDescription("다음 정비 "+values[i]+" · 정비 관리");
    group.setOnClickListener(v->a.navigate("차고"));
   }
   MotoDesign.ripple(a,group,surface,0,12);
   if(narrow){
    row.addView(group,new LinearLayout.LayoutParams(-1,-2));
    if(i==0){
     View separator=new View(a);separator.setBackgroundColor(stroke);
     LinearLayout.LayoutParams sep=new LinearLayout.LayoutParams(-1,a.dp(1));
     sep.setMargins(0,a.dp(6),0,a.dp(6));row.addView(separator,sep);
    }
   }else{
    row.addView(group,new LinearLayout.LayoutParams(0,-2,1));
    if(i==0){
     View separator=new View(a);separator.setBackgroundColor(stroke);
     LinearLayout.LayoutParams sep=new LinearLayout.LayoutParams(a.dp(1),a.dp(48));
     sep.setMargins(a.dp(8),0,a.dp(8),0);row.addView(separator,sep);
    }
   }
  }
  surfaceCard.addView(row);
  View separator=new View(a);separator.setBackgroundColor(stroke);
  LinearLayout.LayoutParams sep=new LinearLayout.LayoutParams(-1,a.dp(1));
  sep.topMargin=a.dp(9);sep.bottomMargin=a.dp(12);surfaceCard.addView(separator,sep);
  double gps=RideMileageEstimate.total(a,b.optString("id"));
  String estimate=Double.isFinite(gps)?String.format(java.util.Locale.KOREA,"%,.1f km",gps):"확인 불가";
  TextView gpsNote=a.text("GPS 누적 추정 "+estimate+"  ·  계기판 km와 별도",12,soft,false);
  gpsNote.setMaxLines(3);surfaceCard.addView(gpsNote);
  if(OdometerAutoSyncInbox.enabled(a,b.optString("id"))){
   TextView odoNote=a.text(OdometerAutoSyncInbox.display(a,b.optString("id")),12,soft,false);
   LinearLayout.LayoutParams op=new LinearLayout.LayoutParams(-1,-2);op.topMargin=a.dp(8);
   surfaceCard.addView(odoNote,op);
  }
  a.content.addView(surfaceCard);
 }
 void rideAction(){
  // Keep consent and stop confirmation; idle GPS action is deliberately secondary.
  boolean recording=RideService.running, armed=RideService.autoArmed;
  String title=armed?"OBD 자동 감지 관리":recording?"GPS 기록 종료":"GPS 기록 시작";
  if(recording||armed){
  Button btn=a.button(title,false,()->{
   if(RideService.autoArmed){
    a.navigate("라이딩");a.toast("자동 감지는 라이딩 탭에서 중지할 수 있어요.");
   }else if(RideService.running){
    new AlertDialog.Builder(a).setTitle("주행 기록을 종료할까요?")
     .setMessage("기록을 종료하면 현재 GPS 위치 수집을 중단합니다.")
     .setNegativeButton("취소",null)
     .setPositiveButton("종료",(d,w)->{
      new RideUi(a).finishWithSummary();
      a.root.postDelayed(()->{if(!a.isFinishing())a.render();},750);
     }).show();
   }else{a.navigate("라이딩");new RideUi(a).requestStart();}
  });
  btn.setTextColor(recording?(a.dark?0xFFFFC5BE:0xFF8F352E):light);btn.setTextSize(14);btn.setMinHeight(a.dp(48));
  btn.setBackground(a.shape(recording?(a.dark?0xFF372422:0xFFFCE8E6):surface,
   recording?(a.dark?0xFFAD615B:0xFFD99792):stroke,8));
  a.content.addView(btn,new LinearLayout.LayoutParams(-1,-2));
  }
  if(!recording&&!armed){
   Button go=a.button("GPS 기록 + 기본 내비 시작",true,()->{
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
  Button settings=a.button("변경",false,()->NavigationApps.choose(a));
  settings.setMinHeight(a.dp(52));
  LinearLayout.LayoutParams lp=narrow?new LinearLayout.LayoutParams(-1,-2):new LinearLayout.LayoutParams(0,-2,1);
  if(narrow)lp.topMargin=a.dp(8);else lp.leftMargin=a.dp(8);
  row.addView(settings,lp);
  a.content.addView(row);
 }
 boolean shortcut(String key){return a.getSharedPreferences("home_shortcuts",0).getBoolean(key,true);}
 void editHomeShortcuts(){
  final String[] keys={"ride","nav","fuel","service"};
  final String[] labels={"GPS 출발·종료","선택한 내비 실행","주유 기록","정비주기"};
  final boolean[] visible=new boolean[keys.length];
  for(int i=0;i<keys.length;i++)visible[i]=shortcut(keys[i]);
  AlertDialog dialog=new AlertDialog.Builder(a).setTitle("홈에서 보여줄 빠른 버튼")
   .setMultiChoiceItems(labels,visible,(d,i,checked)->visible[i]=checked)
   .setNegativeButton("취소",null)
   .setNeutralButton("버튼 순서 변경",(d,w)->changeHomeShortcutOrder())
   .setPositiveButton("저장",null).create();
  // Standard AlertDialog positive callbacks dismiss even if validation fails.
  // Keep the choices and unsaved edits visible until at least one shortcut is selected.
  dialog.setOnShowListener(ignored->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
   int shown=0;for(boolean selected:visible)if(selected)shown++;
   if(shown==0){a.toast("버튼은 하나 이상 선택해 주세요.");return;}
   android.content.SharedPreferences.Editor edit=a.getSharedPreferences("home_shortcuts",0).edit();
   for(int i=0;i<keys.length;i++)edit.putBoolean(keys[i],visible[i]);
   if(edit.commit()){dialog.dismiss();a.renderPreservingPosition();}
   else a.toast("버튼 설정 저장 실패 · 다시 시도해 주세요.");
  }));
  dialog.show();
 }
 void changeHomeShortcutOrder(){
  android.content.SharedPreferences prefs=a.getSharedPreferences("home_shortcuts",0);
  String order=prefs.getString("order","");
  String[] keys=HomeShortcutOrder.parse(order);
  String[] labels=new String[keys.length];
  for(int i=0;i<keys.length;i++)labels[i]=HomeShortcutOrder.label(keys[i]);
  new AlertDialog.Builder(a).setTitle("홈 버튼 순서 · 옮길 버튼 선택")
   .setItems(labels,(d,index)->new AlertDialog.Builder(a)
    .setTitle(labels[index]+" 이동")
    .setItems(new String[]{"한 칸 위로","한 칸 아래로"},(choice,step)->{
     String updated=HomeShortcutOrder.move(order,keys[index],step==0?-1:1);
     if(prefs.edit().putString("order",updated).commit()){
      a.renderPreservingPosition();changeHomeShortcutOrder();
     }else a.toast("순서를 저장하지 못했어요.");
    }).show())
   .setNegativeButton("닫기",null).show();
 }
 /** Two tap targets preserve all existing quick-entry validation and records. */
 void quickActionPair(String left,String right){
  LinearLayout row=new LinearLayout(a);row.setGravity(Gravity.CENTER_VERTICAL);
  String[] keys={left,right};
  for(int i=0;i<keys.length;i++){
   final boolean fuel="fuel".equals(keys[i]);
   Button button=a.button(fuel?"＋ 주유 기록":"＋ 정비 등록",false,()->{
    if(fuel)new QuickEntryUi(a).fuel();else new QuickEntryUi(a).maintenance();
   });
   button.setMinHeight(a.dp(56));
   button.setContentDescription(fuel?"현재 바이크 주유 기록 추가":"현재 바이크 정비 일정 등록");
   LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(0,-2,1);
   if(i==1)params.leftMargin=a.dp(9);
   row.addView(button,params);
  }
  a.content.addView(row);
 }
 void quickAction(String key){
  boolean fuel="fuel".equals(key);
  Button button=a.button(fuel?"＋ 주유 기록":"＋ 정비 등록",false,()->{
   if(fuel)new QuickEntryUi(a).fuel();else new QuickEntryUi(a).maintenance();
  });
  button.setMinHeight(a.dp(54));
  a.content.addView(button,new LinearLayout.LayoutParams(-1,-2));
 }
 void showQuickTools(JSONObject bike){
  java.util.List<String> labels=new java.util.ArrayList<>();
  java.util.List<Runnable> actions=new java.util.ArrayList<>();
  labels.add(ParkingSpotUi.stored(bike)?"주차 위치 보기·갱신":"주차 위치 저장");
  actions.add(()->new ParkingSpotUi(a).show(bike));
  labels.add("최근 주유·정비 수정");actions.add(()->quickRecentEdit(bike));
  labels.add("소모품 정비 현황");actions.add(()->showConsumables(bike));
  if(UndoLastSave.available(a)){labels.add("방금 저장한 기록 취소");actions.add(()->UndoLastSave.undo(a));}
  new AlertDialog.Builder(a).setTitle("빠른 도구")
   .setItems(labels.toArray(new String[0]),(d,index)->actions.get(index).run())
   .setNegativeButton("닫기",null).show();
 }
 /** Edit the latest existing ledger entry without creating a duplicate record. */
 void quickRecentEdit(JSONObject bike){
  java.util.List<JSONObject> recent=new java.util.ArrayList<>();
  java.util.List<String> kinds=new java.util.ArrayList<>();
  for(String key:new String[]{"services","expenses"})for(JSONObject item:a.items(key))
   if(bike.optString("id").equals(item.optString("vehicleId"))){
    recent.add(item);kinds.add(key);
   }
  java.util.List<Integer> order=new java.util.ArrayList<>();
  for(int i=0;i<recent.size();i++)order.add(i);
  order.sort((x,y)->{
   JSONObject left=recent.get(x),right=recent.get(y);
   int day=right.optString("date").compareTo(left.optString("date"));
   return day!=0?day:Double.compare(right.optDouble("mileage"),left.optDouble("mileage"));
  });
  if(order.isEmpty()){a.toast("수정할 기록이 없어요.");return;}
  int count=Math.min(order.size(),5);String[] choices=new String[count];
  for(int i=0;i<count;i++){
   JSONObject item=recent.get(order.get(i));String kind=kinds.get(order.get(i));
   boolean service="services".equals(kind);
   String category=service?(item.optBoolean("scheduleOnly")?"정비 예정":"정비 완료"):
    ("주유".equals(item.optString("type"))?"주유":"기타 비용");
   choices[i]=category+" · "+item.optString("type",service?"정비":"비용")
     +" · "+item.optString("date")+" · "+a.won(item.optDouble(service?"cost":"amount"));
  }
  new AlertDialog.Builder(a).setTitle("최근 기록 바로 수정")
   .setItems(choices,(d,i)->{
    int row=order.get(i);
    if(a.readOnly){a.toast("원본 보호 상태라 수정할 수 없어요.");return;}
    a.edit(kinds.get(row),recent.get(row));
   }).setNegativeButton("닫기",null).show();
 }
 /** Four useful consumables only, from latest maintenance records. No guessed wear. */
 void showConsumables(JSONObject bike){
  java.util.Map<String,JSONObject> latest=LatestServiceByType.latest(a.items("services"),bike.optString("id"));
  java.util.List<JSONObject> list=new java.util.ArrayList<>(latest.values());
  list.sort((x,y)->Integer.compare(
    MaintenancePlanner.order(MaintenanceAlertWindow.status(x,a.today(),bike.optDouble("odo"))),
    MaintenancePlanner.order(MaintenanceAlertWindow.status(y,a.today(),bike.optDouble("odo")))));
  if(list.isEmpty()){a.toast("정비 기록이 아직 없어요.");return;}
  StringBuilder text=new StringBuilder();int shown=0;
  for(JSONObject item:list){
   if(shown++>=8)break;
   text.append(item.optString("type")).append(" · ");
   if(item.optDouble("nextKm")>0||!item.optString("nextDate").isEmpty()){
    text.append(MaintenancePlanner.caption(MaintenanceAlertWindow.status(item,a.today(),bike.optDouble("odo"))));
    text.append(" · ").append(MaintenanceNoticeText.detail(item.optString("nextDate"),item.optDouble("nextKm"),a.today(),bike.optDouble("odo")));
   }else text.append("최근 기록 ").append(item.optString("date","날짜 없음"))
    .append(" · ").append(a.num(item.optDouble("mileage"))).append(" km");
   text.append("\n");
  }
  new AlertDialog.Builder(a).setTitle("소모품 현황 · "+bike.optString("name"))
   .setMessage(text.toString().trim()+"\n\n수명 추정이 아닌 실제 입력 기록 기준입니다.")
   .setNegativeButton("닫기",null).setPositiveButton("정비 관리",(d,w)->a.navigate("차고")).show();
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
