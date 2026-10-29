package com.motohub.next;

import android.app.*;
import android.os.*;
import android.content.*;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.util.AtomicFile;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;

/** Native recovery of the prior P0 implementation. No WebView or network SDK. */
public class MainActivity extends Activity {
 JSONObject db;AtomicFile disk;LinearLayout root,content,formFooter;Dialog dialog;LinearLayout formBody;
 String tab="홈",selected="",query="",filter="전체",sort="최신순",pendingExport="";
 String pendingTrack="";String pendingBundleJson="";char[] pendingSecurePassword;
 final RideStartConsent rideStartConsent=new RideStartConsent();
  boolean launchNavigationAfterRide=false;
 DeveloperObdScreen developerObd;
 String draftKey="",draftId="",draftVehicle="";Map<String,EditText> draftFields;Map<String,Spinner> draftSelectors;CheckBox fullTankBox;boolean formDirty=false;
 LinearLayout formOptionalSection;Button formOptionalToggle;Set<String> formOptionalKeys=new HashSet<>();
 String activeForm="";JSONObject activeOld=new JSONObject();
 String garagePhotoTarget="",receiptPhotoTarget="",receiptPhotoKey="";int receiptPhotoSlot=1;
 Map<String,EditText> ocrFields;String ocrKind="";LinearLayout ocrDateMileageFields;TextView ocrMetaSummary;String cameraCaptureName="";
 EditText quickOdoField;
 String catalogBrand="", catalogModel="";
 boolean dark,readOnly;int bg,fg,muted,line,accent,panel,highlight;
 final Set<String> compare=new LinkedHashSet<>();
 int recordPageOffset=0, listingPageOffset=0;
 volatile boolean interruptedRecoveryPromptVisible=false;
 final java.util.concurrent.atomic.AtomicBoolean recoveryReminderChecking=new java.util.concurrent.atomic.AtomicBoolean();
 volatile long suppressUpdatePromptUntilElapsed=0;
 boolean suppressUpdatePrompt(){return android.os.SystemClock.elapsedRealtime()<suppressUpdatePromptUntilElapsed||interruptedRecoveryPromptVisible||recoveryReminderChecking.get()||rideStartConsent.pending();}
 String garageMode="전체", previousTab="홈", rideHistoryFilter=RideHistoryFilter.ALL, rideHistorySort=RideHistorySort.NEWEST, rideHistoryPeriod=RideHistoryPeriod.ALL, rideHistoryBike=RideHistoryBikeFilter.ALL, rideHistoryQuery="";
 final String[] tabs={"홈","라이딩","차고","더보기"};
 int lastRenderedRideState=-1;
 int rideStateSignature(){
  // Back from phone navigation: skip expensive full layout+ride-history reload if
  // GPS/OBD and permission state are unchanged. Existing live label updater remains.
  return (RideService.running?1:0)|(RideService.autoArmed?2:0)|
   (new RideUi(this).readiness().ordinal()<<2);
 }
 interface Work{void run()throws Exception;}
 interface Choice{void pick(String value);}
 @Override public void onCreate(Bundle state){super.onCreate(state);PrivateCacheCleanup.atProcessStart(getCacheDir(),getFilesDir(),System.currentTimeMillis());ReceiptCameraFiles.cleanOld(getCacheDir(),System.currentTimeMillis());new Thread(()->{synchronized(MotorcyclePhoto.class){AutoPhotoCache.trim(new File(getFilesDir(),"moto-year-photos"),System.currentTimeMillis());}},"moto-photo-cache-maintenance").start();if(!RideService.running)Sharing.stop();disk=new AtomicFile(new File(getFilesDir(),"motohub-v3.json"));
  boolean crashedRestoreRecovered=false,crashedRestoreNeedsHelp=false;
  try{
   crashedRestoreRecovered=RestoreArchive.CrashJournal.recoverAll(getFilesDir(),bytes->atomic(disk,bytes))>0;
   // Recover any interrupted GPS trash move before a new ride or backup sees partial files.
   if(RideTrash.recoverInterrupted(getFilesDir())>0)crashedRestoreRecovered=true;
  }catch(Exception e){readOnly=true;crashedRestoreNeedsHelp=true;}
  try{db=disk.getBaseFile().exists()||new File(disk.getBaseFile()+".bak").exists()?Store.parse(new String(disk.readFully(),StandardCharsets.UTF_8)):Store.empty();}
  catch(Exception e){try{db=Store.empty();}catch(Exception ignored){}readOnly=true;}
  // Restore local list preference across process death; rotation state wins when available.
  rideHistorySort=RideHistorySort.restore(getSharedPreferences("ride_history_ui",MODE_PRIVATE).getString("sort",RideHistorySort.NEWEST),state==null?null:state.getString("rideHistorySort"));
  rideHistoryFilter=RideHistoryFilter.restore(getSharedPreferences("ride_history_ui",MODE_PRIVATE).getString("filter",RideHistoryFilter.ALL),state==null?null:state.getString("rideHistoryFilter"));
  rideHistoryPeriod=RideHistoryPeriod.restore(getSharedPreferences("ride_history_ui",MODE_PRIVATE).getString("period",RideHistoryPeriod.ALL),state==null?null:state.getString("rideHistoryPeriod"));
  rideHistoryBike=RideHistoryBikeFilter.restore(getSharedPreferences("ride_history_ui",MODE_PRIVATE).getString("bike",RideHistoryBikeFilter.ALL),state==null?null:state.getString("rideHistoryBike"));
  if(state==null)selected=getSharedPreferences("garage_ui",MODE_PRIVATE).getString("selected_bike","");
  if(state!=null){rideHistoryQuery=RideHistorySearch.limit(state.getString("rideHistoryQuery",""));pendingTrack=state.getString("pendingTrack","");tab=state.getString("tab","홈");selected=state.getString("selected","");garagePhotoTarget=state.getString("garagePhotoTarget","");receiptPhotoTarget=state.getString("receiptPhotoTarget","");receiptPhotoKey=state.getString("receiptPhotoKey","");receiptPhotoSlot=state.getInt("receiptPhotoSlot",1);cameraCaptureName=state.getString("cameraCaptureName","");if(state.getBoolean("rideStartConsent",false))rideStartConsent.approve();launchNavigationAfterRide=state.getBoolean("launchNavigationAfterRide",false);}
  if(Build.VERSION.SDK_INT>=33)getOnBackInvokedDispatcher().registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,this::back);
  applyWidgetTab(getIntent());
  render();
  applyWidgetForm(getIntent());
  boolean widgetActionTaken=applyWidgetQuickActions(getIntent());
  boolean notificationActionTaken=confirmStopFromNotification(getIntent());
  DailyMaintenanceJob.schedule(this);
  MotoWidgetProvider.refresh(this);
  if(state==null)receiveSharedListing(getIntent());
  if(crashedRestoreNeedsHelp)error("미완료 백업 복원의 원본 사본을 안전하게 되돌리지 못했어요. 추가 저장을 차단하고 복구용 파일을 보존했습니다. 앱을 삭제하지 말고 백업 상태를 확인해 주세요.");
  else if(readOnly)error("저장 파일을 읽지 못해 원본을 보존하고 새 저장을 차단했습니다. 더보기에서 백업을 복원해 주세요.");
  else if(crashedRestoreRecovered)toast("중단됐던 백업 복원을 이전 기록으로 안전하게 되돌렸어요.");
  if(state==null){
   // Main Store has already been parsed; audit the old restore point off the UI thread.
   final boolean loaded=!readOnly;
   new Thread(()->{
    String audit=UpdateInstallSnapshot.afterStartup(this,BuildConfig.VERSION_CODE,loaded);
    if(audit!=null)runOnUiThread(()->{if(!isFinishing()&&!isDestroyed())toast(audit);});
   },"moto-post-update-data-audit").start();
  }
  if(state==null && !RideService.running && !RideService.autoArmed && !widgetActionTaken&&!notificationActionTaken)showInterruptedRidePrompt();
 }
 /** Process death/reboot leaves a local .track without .done. Only offers a manual review. */
 void showInterruptedRidePrompt(){
  if(RideService.running||RideService.autoArmed||readOnly||
     interruptedRecoveryPromptVisible||!recoveryReminderChecking.compareAndSet(false,true))return;
  new Thread(()->{
   try{
    File file=InterruptedRideNotice.latest(getFilesDir(),RideService.active,RideService.running);
    if(file==null)return;
    String filename=file.getName();
    android.content.SharedPreferences prefs=getSharedPreferences("ride_recovery_notice",MODE_PRIVATE);
    if(!InterruptedRideReminderPolicy.shouldShow(filename,
      prefs.getString("acknowledged",""),prefs.getString("snoozed_file",""),
      prefs.getLong("snoozed_at",0),System.currentTimeMillis()))return;
    runOnUiThread(()->{
     if(isFinishing()||isDestroyed()||RideService.running||RideService.autoArmed||
        interruptedRecoveryPromptVisible||dialog!=null)return;
     // Never reuse the old seen flag: it hid missed rides permanently on Later.
     interruptedRecoveryPromptVisible=true;
     AlertDialog prompt=new AlertDialog.Builder(this).setTitle("중단된 라이딩 기록 발견")
      .setMessage("정상 종료되지 않은 GPS 경로가 있어요. 저장된 좌표를 확인하고 복구할 수 있습니다. 위치 기록이나 공유를 자동 시작하지 않습니다.")
      .setNegativeButton("내일 다시 알림",(d,w)->prefs.edit()
        .putString("snoozed_file",filename)
        .putLong("snoozed_at",System.currentTimeMillis()).apply())
      .setPositiveButton("기록 확인",(d,w)->{
       prefs.edit().putString("acknowledged",filename)
        .remove("snoozed_file").remove("snoozed_at").apply();
       tab="라이딩";rideHistoryFilter=RideHistoryFilter.INTERRUPTED;render();
      }).create();
     prompt.setOnCancelListener(d->prefs.edit().putString("snoozed_file",filename)
      .putLong("snoozed_at",System.currentTimeMillis()).apply());
     prompt.setOnDismissListener(d->interruptedRecoveryPromptVisible=false);
     prompt.show();
    });
   }catch(Exception ignored){ /* Never block app opening or modify GPS originals. */ }
   finally{recoveryReminderChecking.set(false);}
  },"moto-interrupted-ride-check").start();
 }
 /** Show the durable summary once when the rider comes back from navigation. */
 void showPendingRideSummary(){
  if(isFinishing()||isDestroyed()||!hasWindowFocus()||RideService.running||
     RideService.saving()||dialog!=null)return;
  JSONObject pending=RideCompletionInbox.unread(this);
  if(pending==null)return;
  String id=pending.optString("id"),summary=pending.optString("summary");
  if(summary.isEmpty())return;
  new AlertDialog.Builder(this).setTitle("라이딩 저장 완료").setMessage(summary)
   .setNegativeButton("닫기",null)
   .setPositiveButton("주행 내역 보기",(d,w)->{tab="라이딩";render();})
   .setOnDismissListener(d->RideCompletionInbox.seen(this,id)).show();
 }
 /** A summary can always be reopened, even if another dialog blocked its popup. */
 void showRideCompletionHistory(){
  java.util.List<JSONObject> entries=RideCompletionInbox.entries(this);
  if(entries.isEmpty()){toast("저장된 라이딩 요약이 없어요.");return;}
  String[] options=new String[entries.size()];
  for(int i=0;i<options.length;i++){
   JSONObject item=entries.get(i);
   String label=RideHistoryLabel.title(
    new File(item.optString("id","")),java.time.ZoneId.systemDefault());
   options[i]=(item.optBoolean("seen")?"":"● ")+label;
  }
  new AlertDialog.Builder(this).setTitle("최근 완료 라이딩 · 최대 10건")
   .setItems(options,(d,which)->{
    JSONObject item=entries.get(which);
    String id=item.optString("id");
    RideCompletionInbox.seen(this,id);
    new AlertDialog.Builder(this).setTitle("저장된 GPS 요약")
     .setMessage(item.optString("summary","")).setNegativeButton("닫기",null)
     .setPositiveButton("라이딩 내역",(dd,w)->{tab="라이딩";render();}).show();
   }).setNegativeButton("닫기",null).show();
 }
 @Override public void onWindowFocusChanged(boolean focused){
  super.onWindowFocusChanged(focused);
  if(focused&&root!=null)root.postDelayed(this::showPendingRideSummary,200);
 }
 @Override protected void onResume(){super.onResume();
  if(root!=null)root.postDelayed(this::showPendingRideSummary,350);
  // Device location/notification settings may have changed while the settings app was open.
  if(root!=null&&"라이딩".equals(tab)&&dialog==null&&rideStateSignature()!=lastRenderedRideState)renderPreservingPosition();
  if(!readOnly)DailyMaintenanceJob.schedule(this);
  MotoWidgetProvider.refresh(this);
  if(!RideService.running&&!RideService.autoArmed&&!readOnly&&!"라이딩".equals(tab))showInterruptedRidePrompt();
  AppUpdateNotice.checkOnResume(this);
 }
 @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);if(applyWidgetTab(intent))render();applyWidgetForm(intent);applyWidgetQuickActions(intent);confirmStopFromNotification(intent);receiveSharedListing(intent);}
 boolean applyWidgetTab(Intent intent){
  if(intent==null)return false;
  String requested=intent.getStringExtra(MotoWidgetProvider.EXTRA_TAB);
  intent.removeExtra(MotoWidgetProvider.EXTRA_TAB);
  if("홈".equals(requested)||"차고".equals(requested)||"라이딩".equals(requested)){tab=requested;return true;}
  return false;
 }
 /** A widget opens an explicit consent dialog; it never starts GPS or remote sharing by itself. */
 boolean applyWidgetQuickActions(Intent intent){
  if(intent==null)return false;
  boolean ride=intent.getBooleanExtra(MotoWidgetProvider.EXTRA_ONE_TAP_RIDE,false);
  boolean nav=intent.getBooleanExtra(MotoWidgetProvider.EXTRA_NAVIGATION,false);
  boolean chooseNav=intent.getBooleanExtra(MotoWidgetProvider.EXTRA_SELECT_NAV,false);
  intent.removeExtra(MotoWidgetProvider.EXTRA_ONE_TAP_RIDE);
  intent.removeExtra(MotoWidgetProvider.EXTRA_NAVIGATION);
  intent.removeExtra(MotoWidgetProvider.EXTRA_SELECT_NAV);
  if(!ride&&!nav&&!chooseNav)return false;
  suppressUpdatePromptUntilElapsed=android.os.SystemClock.elapsedRealtime()+120_000L;
  if(dialog!=null&&dialog.isShowing()){toast("입력 중인 화면을 먼저 닫아 주세요.");return true;}
  if(chooseNav)NavigationApps.choose(this);
  else if(ride){
   if(!"라이딩".equals(tab)){tab="라이딩";render();}
   if(RideService.running||RideService.autoArmed)NavigationApps.openPreferred(this);
   else new RideUi(this).requestStart(true); // Still requires explicit consent before GPS starts.
  }else if(nav)NavigationApps.openPreferred(this);
  return true;
 }
 /** Android notification is intentionally confirmation-only: never stop a track on a single accidental touch. */
 boolean confirmStopFromNotification(Intent intent){
  if(intent==null||!"com.motohub.next.CONFIRM_RIDE_STOP".equals(intent.getAction()))return false;
  intent.setAction("com.motohub.next.RIDE_REVIEW_OPENED");
  if(!RideService.running){toast("현재 진행 중인 GPS 기록은 없습니다.");return true;}
  if(dialog!=null&&dialog.isShowing()){toast("작성 중인 화면을 닫고 라이딩 종료를 눌러 주세요.");return true;}
  new AlertDialog.Builder(this).setTitle("라이딩 기록을 종료할까요?")
   .setMessage("잘못 눌렀다면 계속 기록을 선택하세요. GPS 경로는 사용자가 확인해야만 종료합니다.")
   .setNegativeButton("계속 기록",null)
   .setPositiveButton("저장하고 종료",(d,w)->{
    if(RideService.running)startService(new Intent(this,RideService.class).setAction(RideService.STOP));
   }).show();
  return true;
 }
 void applyWidgetForm(Intent intent){
  if(intent==null)return;
  String form=intent.getStringExtra(MotoWidgetProvider.EXTRA_FORM);
  intent.removeExtra(MotoWidgetProvider.EXTRA_FORM);
  // Widgets may only open a local form. They must never auto-save records or start GPS/sharing.
  if("expenses".equals(form)||"services".equals(form)){
   if(dialog!=null){toast("작성 중인 입력을 먼저 저장하거나 닫아 주세요.");return;}
   tab="차고";render();if(readOnly){toast("저장 파일 보호 중이에요.");return;}edit(form,null);
  }
 }
 /** Legacy text-share entry point: listing discovery was retired, never save new listings. */
 void receiveSharedListing(Intent intent){
  if(intent==null||!Intent.ACTION_SEND.equals(intent.getAction()))return;
  intent.removeExtra(Intent.EXTRA_TEXT);
  toast("매물 관리 기능을 숨겼습니다. 기존 기록은 전체 백업에 보존돼요.");
 }
 @Override public void onSaveInstanceState(Bundle out){super.onSaveInstanceState(out);out.putString("pendingTrack",pendingTrack);out.putString("tab",tab);out.putString("selected",selected);out.putString("garagePhotoTarget",garagePhotoTarget);out.putString("receiptPhotoTarget",receiptPhotoTarget);out.putString("receiptPhotoKey",receiptPhotoKey);out.putInt("receiptPhotoSlot",receiptPhotoSlot);out.putString("cameraCaptureName",cameraCaptureName);out.putString("rideHistoryFilter",rideHistoryFilter);out.putString("rideHistorySort",rideHistorySort);out.putString("rideHistoryPeriod",rideHistoryPeriod);out.putString("rideHistoryBike",rideHistoryBike);out.putString("rideHistoryQuery",rideHistoryQuery);out.putBoolean("rideStartConsent",rideStartConsent.pending());out.putBoolean("launchNavigationAfterRide",launchNavigationAfterRide);}
 void attempt(Work w){try{w.run();}catch(Exception e){error(e instanceof NumberFormatException?"숫자를 확인해 주세요.":e instanceof java.time.format.DateTimeParseException?"날짜는 YYYY-MM-DD 형식으로 입력해 주세요.":e.getMessage()==null?"작업을 완료하지 못했습니다.":e.getMessage());}}
 void error(String s){new AlertDialog.Builder(this).setTitle("확인해 주세요").setMessage(s).setPositiveButton("확인",null).show();}
 void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
 int dp(float v){return (int)(v*getResources().getDisplayMetrics().density+.5f);}
 String today(){return LocalDate.now(ZoneId.of("Asia/Seoul")).toString();}
 String num(double n){return String.format(Locale.KOREA,"%,.0f",n);}String won(double n){return num(n)+"원";}
 JSONArray arr(String k){return db.optJSONArray(k);}JSONObject settings(){return db.optJSONObject("settings");}
 JSONObject bike(){String id=selected.isEmpty()?settings().optString("primary"):selected;
  for(JSONObject b:items("vehicles"))if(b.optString("id").equals(id)&&(!selected.isEmpty()||!GarageBikeStatus.sold(b.optString("soldDate"))))return b;
  for(JSONObject b:items("vehicles"))if(!GarageBikeStatus.sold(b.optString("soldDate")))return b;
  return arr("vehicles").length()>0?arr("vehicles").optJSONObject(0):null;}
 String bikeName(String id){for(JSONObject b:items("vehicles"))if(b.optString("id").equals(id))return b.optString("name");return "차량 없음";}
 List<JSONObject> items(String key){List<JSONObject> list=new ArrayList<>();JSONArray a=arr(key);for(int i=a.length()-1;i>=0;i--)list.add(a.optJSONObject(i));if(key.equals("services")||key.equals("expenses"))list.sort((x,y)->y.optString("date").compareTo(x.optString("date")));return list;}
 void write(JSONObject next)throws Exception{if(readOnly)throw new Exception("원본 보호 중입니다. 백업 복원을 먼저 해 주세요.");Store.parse(next.toString());
  // The first snapshot of the day must preserve the state before the edit, not the new state.
  LocalSafetyBackup.snapshot(this,db);
  atomic(disk,next.toString(2).getBytes(StandardCharsets.UTF_8));db=next;LocalSafetyBackup.checkpoint(this,next);MotoWidgetProvider.refresh(this);}
 void atomic(AtomicFile file,byte[] bytes)throws Exception{FileOutputStream out=null;try{out=file.startWrite();out.write(bytes);file.finishWrite(out);}catch(Exception e){if(out!=null)file.failWrite(out);throw e;}}
 void saveItem(String key,JSONObject item)throws Exception{JSONObject next=new JSONObject(db.toString());Store.upsert(next,key,item);write(next);}
 GradientDrawable shape(int color,int stroke,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));if(stroke!=0)d.setStroke(dp(1),stroke);return d;}
 LinearLayout col(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
 TextView text(String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setTypeface(bold?MotoDesign.medium():Typeface.create("sans-serif",Typeface.NORMAL));t.setIncludeFontPadding(false);t.setLineSpacing(dp(size>=14?2:1),1f);return t;}
 void label(LinearLayout p,String s,int size,int color,boolean bold){TextView t=text(s,size,color,bold);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(8);p.addView(t,lp);}
 void gap(LinearLayout p,int n){p.addView(new Space(this),new LinearLayout.LayoutParams(1,dp(n)));}
 void divider(LinearLayout p){View v=new View(this);v.setBackgroundColor(line);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(1));lp.topMargin=dp(12);lp.bottomMargin=dp(12);p.addView(v,lp);}
 Button button(String s,boolean primary,Runnable r){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextSize(14);b.setTypeface(MotoDesign.medium());b.setMinHeight(dp(48));b.setMinimumWidth(0);b.setElevation(0);b.setStateListAnimator(null);b.setTextColor(primary?(dark?0xFF18191B:Color.WHITE):fg);MotoDesign.ripple(this,b,primary?accent:panel,primary?0:line,8);b.setPadding(dp(14),dp(8),dp(14),dp(8));b.setOnClickListener(v->r.run());return b;}
 void action(LinearLayout p,String s,boolean primary,Runnable r){if(p==formBody&&s.equals("저장")&&formFooter!=null)p=formFooter;LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(8);p.addView(button(s,primary,r),lp);}
 void pair(LinearLayout p,String a,Runnable ra,String b,Runnable rb){LinearLayout row=new LinearLayout(this);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);lp.setMargins(0,0,dp(4),dp(8));row.addView(button(a,true,ra),lp);LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(0,-2,1);rp.setMargins(dp(4),0,0,dp(8));row.addView(button(b,false,rb),rp);p.addView(row);}
 void heading(String t,String sub){gap(content,8);if(!t.equals(MotoDesign.heading(tab))){TextView title=text(t,22,fg,true);title.setTypeface(MotoDesign.display());content.addView(title);gap(content,6);}if(sub!=null&&!sub.isEmpty())label(content,sub,13,muted,false);gap(content,13);}
 void section(String s){gap(content,18);label(content,s,17,fg,true);}
 void note(String s){TextView t=text(s,13,muted,false);t.setLineSpacing(dp(3),1f);t.setPadding(dp(15),dp(15),dp(15),dp(15));t.setBackground(shape(panel,0,12));content.addView(t);gap(content,8);}
 void row(String title,String sub,String tail,Runnable r){new HubScreens(this).line(title,sub,tail,r);}
 File garagePhotoFile(String id){
  if(id==null||!id.matches("[a-fA-F0-9-]{1,64}"))return null;
  return new File(getFilesDir(),"garage-photo-"+id+".jpg");
 }
 void chooseGaragePhoto(){JSONObject b=bike();if(b==null){toast("바이크를 먼저 등록해 주세요.");return;}
  garagePhotoTarget=b.optString("id");if(garagePhotoFile(garagePhotoTarget)==null){toast("사진을 등록할 수 없는 바이크 ID입니다.");return;}
  Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT);intent.setType("image/*");intent.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(intent,52);
 }
 // Serialize photo writes so an older picker result cannot finish after a newer one.
 static final java.util.concurrent.ExecutorService GARAGE_PHOTO_SAVER=java.util.concurrent.Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"moto-garage-photo-save");t.setDaemon(true);return t;});
 void saveGaragePhoto(Uri uri,String bikeId)throws Exception{
  // Bind the photo to the bike selected when the document picker returned, not a mutable UI field.
  File file=garagePhotoFile(bikeId);if(file==null)throw new IOException("바이크 사진 저장 경로 오류");
  // Decode through the content resolver, bound pixels to avoid loading full camera images into memory.
  android.graphics.BitmapFactory.Options opts=new android.graphics.BitmapFactory.Options();opts.inJustDecodeBounds=true;
  try(InputStream in=getContentResolver().openInputStream(uri)){if(in==null)throw new IOException("선택한 사진을 열 수 없습니다.");android.graphics.BitmapFactory.decodeStream(in,null,opts);}
  if(opts.outWidth<1||opts.outHeight<1||opts.outWidth>25000||opts.outHeight>25000)throw new IOException("지원하지 않는 이미지 크기입니다.");
  opts.inJustDecodeBounds=false;opts.inSampleSize=1;while(opts.outWidth/opts.inSampleSize>1400||opts.outHeight/opts.inSampleSize>1400)opts.inSampleSize*=2;
  android.graphics.Bitmap bitmap;
  try(InputStream in=getContentResolver().openInputStream(uri)){if(in==null)throw new IOException("사진을 열 수 없습니다.");bitmap=android.graphics.BitmapFactory.decodeStream(in,null,opts);}
  if(bitmap==null)throw new IOException("지원하지 않는 사진입니다.");
  // The picker can return portrait JPEG pixels with EXIF rotation; preserve their appearance.
  bitmap=ReceiptImageOrientation.rotate(bitmap,ReceiptImageOrientation.readDegrees(this,uri));
  // Keep the previous photo intact if encoding, disk writes or the final commit fail.
  // AtomicFile writes to a staging file and only replaces the old image after success.
  AtomicFile photoDisk=new AtomicFile(file);
  FileOutputStream photoOut=null;
  try{
   photoOut=photoDisk.startWrite();
   if(!bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG,85,photoOut))throw new IOException("사진 변환 실패");
   photoDisk.finishWrite(photoOut);
   photoOut=null;
  }catch(Exception failure){
   if(photoOut!=null)photoDisk.failWrite(photoOut);
   throw failure;
  }finally{bitmap.recycle();}
  runOnUiThread(()->{if(!isFinishing()&&!isDestroyed()){
   toast("바이크 사진을 변경했어요.");render();
  }});
 }
 /** Every receipt stays in app-private storage and is included in full ZIP backups. */
 File receiptPhotoFile(String id){return receiptPhotoFile(id,1);}
 File receiptPhotoFile(String id,int slot){
  if(!ReceiptSlots.validId(id)||!ReceiptSlots.validSlot(slot))return null;
  return new File(getFilesDir(),ReceiptSlots.filename(id,slot));
 }
 int receiptCount(String id){int count=0;for(int slot=1;slot<=ReceiptSlots.MAX_SLOTS;slot++){
  File f=receiptPhotoFile(id,slot);if(f!=null&&f.isFile())count++;
 }return count;}
 void clearReceiptFiles(String id){for(int slot=1;slot<=ReceiptSlots.MAX_SLOTS;slot++){
  File f=receiptPhotoFile(id,slot);if(f!=null&&f.isFile()&&!f.delete())toast("영수증 사진 정리에 실패했습니다. 백업 전에 확인해 주세요.");
 }}
 void chooseReceiptPhoto(String key,JSONObject record){chooseReceiptPhoto(key,record,1);}
 void chooseReceiptPhoto(String key,JSONObject record,int slot){
  if(readOnly){error("원본 보호 상태에서는 사진을 변경할 수 없습니다.");return;}
  if(!key.equals("services")&&!key.equals("expenses"))return;
  File f=receiptPhotoFile(record.optString("id"),slot);if(f==null){error("영수증 연결 정보를 확인해 주세요.");return;}
  receiptPhotoTarget=record.optString("id");receiptPhotoKey=key;receiptPhotoSlot=slot;
  Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT);intent.setType("image/*");intent.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(intent,53);
 }
 /** Camera output is a private cache file. No camera permission or shared media-library write. */
 void startReceiptCamera(boolean forOcr){
  if(readOnly&&!forOcr){error("원본 보호 중에는 사진을 바꿀 수 없습니다.");return;}
  if(!cameraCaptureName.isEmpty()){toast("이미 촬영 화면을 열고 있어요.");return;}
  File file=null;
  try{
   file=ReceiptCameraFiles.create(getCacheDir());
   Uri destination=androidx.core.content.FileProvider.getUriForFile(this,getPackageName()+".receiptcamera",file);
   Intent capture=new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
   capture.putExtra(android.provider.MediaStore.EXTRA_OUTPUT,destination);
   capture.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_READ_URI_PERMISSION);
   capture.setClipData(ClipData.newUri(getContentResolver(),"촬영 임시 사진",destination));
   cameraCaptureName=file.getName();
   startActivityForResult(capture,forOcr?55:56);
  }catch(Exception e){
   if(file!=null)ReceiptCameraFiles.delete(getCacheDir(),file.getName());
   cameraCaptureName="";if(forOcr){ocrFields=null;ocrKind="";}
   error("카메라를 열 수 없어요. 갤러리에서 영수증을 선택해 주세요.");
  }
 }
 void captureReceiptPhoto(String key,JSONObject record,int slot){
  if(readOnly){error("원본 보호 중에는 사진을 변경할 수 없습니다.");return;}
  if(!"services".equals(key)&&!"expenses".equals(key))return;
  if(receiptPhotoFile(record.optString("id"),slot)==null){error("사진 연결 정보를 확인해 주세요.");return;}
  receiptPhotoTarget=record.optString("id");receiptPhotoKey=key;receiptPhotoSlot=slot;
  startReceiptCamera(false);
 }
 void finishReceiptCapture(int request,int result){
  String name=cameraCaptureName;cameraCaptureName="";
  File file=ReceiptCameraFiles.recover(getCacheDir(),name);
  if(result!=RESULT_OK||!ReceiptCameraFiles.valid(getCacheDir(),file)||file.length()<128||file.length()>30_000_000){
   ReceiptCameraFiles.delete(getCacheDir(),name);
   if(request==55){ocrFields=null;ocrKind="";}
   if(request==56){receiptPhotoTarget="";receiptPhotoKey="";receiptPhotoSlot=1;}
   if(result==RESULT_OK)error("사진이 비어 있거나 너무 커요. 다시 촬영해 주세요.");
   return;
  }
  Uri local=Uri.fromFile(file); // Never exposed to another application.
  if(request==55){scanReceiptOcr(local,()->ReceiptCameraFiles.delete(getCacheDir(),name));return;}
  if(request==58){scanQuickOdometerPhoto(local,()->ReceiptCameraFiles.delete(getCacheDir(),name));return;}
  new Thread(()->{try{saveReceiptPhoto(local);}catch(Exception e){runOnUiThread(()->error("사진 첨부 실패: "+e.getMessage()));}
   finally{ReceiptCameraFiles.delete(getCacheDir(),name);}},"moto-receipt-camera-save").start();
 }
 void saveReceiptPhoto(Uri uri)throws Exception{
  String id=receiptPhotoTarget,key=receiptPhotoKey;int slot=receiptPhotoSlot;receiptPhotoTarget="";receiptPhotoKey="";receiptPhotoSlot=1;
  if(readOnly||!"services".equals(key)&&!"expenses".equals(key))throw new IOException("영수증 연결 상태가 올바르지 않습니다.");
  File output=receiptPhotoFile(id,slot);if(output==null||Store.find(db,key,id)==null)throw new IOException("삭제된 기록에는 영수증을 첨부할 수 없습니다.");
  android.graphics.BitmapFactory.Options opts=new android.graphics.BitmapFactory.Options();opts.inJustDecodeBounds=true;
  try(InputStream in=getContentResolver().openInputStream(uri)){if(in==null)throw new IOException("사진을 열 수 없습니다.");android.graphics.BitmapFactory.decodeStream(in,null,opts);}
  if(opts.outWidth<1||opts.outHeight<1||opts.outWidth>25000||opts.outHeight>25000)throw new IOException("지원하지 않는 이미지 크기입니다.");
  int max=Math.max(opts.outWidth,opts.outHeight);opts.inJustDecodeBounds=false;opts.inSampleSize=1;
  while(max/opts.inSampleSize>1400)opts.inSampleSize*=2;
  android.graphics.Bitmap image;
  try(InputStream in=getContentResolver().openInputStream(uri)){if(in==null)throw new IOException("사진을 읽을 수 없습니다.");image=android.graphics.BitmapFactory.decodeStream(in,null,opts);}
  if(image==null)throw new IOException("이미지를 처리할 수 없습니다.");
  // Cameras commonly store portrait photos as sideways pixels with an EXIF rotation flag.
  // JPEG re-encoding drops that flag; rotate pixels before saving the private copy.
  try{image=ReceiptImageOrientation.rotate(image,ReceiptImageOrientation.readDegrees(this,uri));}
  catch(Exception e){image.recycle();throw new IOException("영수증 방향을 조정하지 못했습니다.",e);}
  AtomicFile atomic=new AtomicFile(output);FileOutputStream stream=null;
  try{stream=atomic.startWrite();if(!image.compress(android.graphics.Bitmap.CompressFormat.JPEG,82,stream))throw new IOException("사진 저장에 실패했습니다.");atomic.finishWrite(stream);stream=null;}
  catch(Exception e){if(stream!=null)atomic.failWrite(stream);throw e;}finally{image.recycle();}
  runOnUiThread(()->toast("영수증을 저장했어요. 전체 ZIP 백업에도 포함됩니다."));
 }
 void receiptOptions(String key,JSONObject item){
  String id=item.optString("id");if(receiptPhotoFile(id)==null)return;
  String[] menu=new String[ReceiptSlots.MAX_SLOTS];
  for(int slot=1;slot<=ReceiptSlots.MAX_SLOTS;slot++){
   File photo=receiptPhotoFile(id,slot);menu[slot-1]="사진 "+slot+(photo.isFile()?"  ·  보기 / 교체 / 삭제":"  ·  추가");
  }
  new AlertDialog.Builder(this).setTitle("영수증 · 작업 사진 (최대 "+ReceiptSlots.MAX_SLOTS+"장)")
   .setItems(menu,(d,which)->{
    int slot=which+1;File photo=receiptPhotoFile(id,slot);
    if(!photo.isFile()){
     new AlertDialog.Builder(this).setTitle("사진 "+slot+" 추가")
      .setItems(new String[]{"카메라로 촬영","갤러리에서 선택"},(sub,choice)->{
       if(choice==0)captureReceiptPhoto(key,item,slot);else chooseReceiptPhoto(key,item,slot);
      }).show();return;
    }
    new AlertDialog.Builder(this).setTitle("사진 "+slot)
     .setItems(new String[]{"보기","갤러리 사진으로 교체","카메라로 교체","이 사진만 삭제"},(sub,index)->{
      if(index==0){android.graphics.BitmapFactory.Options opts=new android.graphics.BitmapFactory.Options();opts.inJustDecodeBounds=true;
       android.graphics.BitmapFactory.decodeFile(photo.getAbsolutePath(),opts);
       if(opts.outWidth<1||opts.outHeight<1){error("사진을 읽을 수 없습니다.");return;}
       opts.inJustDecodeBounds=false;opts.inSampleSize=1;
       while(opts.outWidth/opts.inSampleSize>1280||opts.outHeight/opts.inSampleSize>1280)opts.inSampleSize*=2;
       android.graphics.Bitmap bitmap=android.graphics.BitmapFactory.decodeFile(photo.getAbsolutePath(),opts);
       if(bitmap==null){error("사진을 읽을 수 없습니다.");return;}
       ImageView preview=new ImageView(this);preview.setImageBitmap(bitmap);preview.setAdjustViewBounds(true);
       new AlertDialog.Builder(this).setTitle("영수증 사진 "+slot).setView(preview).setPositiveButton("닫기",null).show();
      }else if(index==1)chooseReceiptPhoto(key,item,slot);
      else if(index==2)captureReceiptPhoto(key,item,slot);
      else if(!readOnly)new AlertDialog.Builder(this).setTitle("사진 "+slot+"만 삭제할까요?")
       .setMessage("정비·주유 기록과 다른 사진은 그대로 남습니다.")
       .setNegativeButton("취소",null).setPositiveButton("삭제",(ok,w)->{
        if(photo.delete())toast("사진을 삭제했어요.");else error("사진 삭제에 실패했습니다.");
       }).show();
     }).show();
   }).show();
 }
 void hideKeyboard(){android.view.inputmethod.InputMethodManager imm=getSystemService(android.view.inputmethod.InputMethodManager.class);View focus=getCurrentFocus();if(focus!=null){imm.hideSoftInputFromWindow(focus.getWindowToken(),0);focus.clearFocus();}}
 void navigate(String destination){if("매물".equals(destination))destination="홈";hideKeyboard();if(destination.equals("더보기")&&!tab.equals("더보기"))previousTab=tab;tab=destination;render();}
 void selectRideHistoryFilter(String value){
  String normalized=RideHistoryFilter.normalize(value);
  // Only a local display preference: never part of backup, GPS or cloud sharing.
  getSharedPreferences("ride_history_ui",MODE_PRIVATE).edit().putString("filter",normalized).apply();
  updateRideHistoryControls(normalized,rideHistorySort);
 }
 void selectRideHistorySort(String value){
  String normalized=RideHistorySort.normalize(value);
  getSharedPreferences("ride_history_ui",MODE_PRIVATE).edit().putString("sort",normalized).apply();
  updateRideHistoryControls(rideHistoryFilter,normalized);
 }
 void selectRideHistoryPeriod(String value){
  String normalized=RideHistoryPeriod.normalize(value);
  getSharedPreferences("ride_history_ui",MODE_PRIVATE).edit().putString("period",normalized).apply();
  rideHistoryPeriod=normalized;
  updateRideHistoryControls(rideHistoryFilter,rideHistorySort);
 }
 void selectRideHistoryBike(String value){
  rideHistoryBike=RideHistoryBikeFilter.normalize(value);
  getSharedPreferences("ride_history_ui",MODE_PRIVATE).edit().putString("bike",rideHistoryBike).apply();
  updateRideHistoryControls(rideHistoryFilter,rideHistorySort);
 }
 void resetRideHistoryControls(){
  rideHistoryQuery="";
  rideHistoryPeriod=RideHistoryPeriod.ALL;
  rideHistoryBike=RideHistoryBikeFilter.ALL;
  getSharedPreferences("ride_history_ui",MODE_PRIVATE).edit()
   .putString("filter",RideHistoryFilter.ALL).putString("period",RideHistoryPeriod.ALL)
   .putString("bike",RideHistoryBikeFilter.ALL).apply();
  updateRideHistoryControls(RideHistoryFilter.ALL,rideHistorySort);
 }
 void updateRideHistoryControls(String selectedFilter,String selectedSort){
  int previousScroll=0;
  if(root!=null)for(int i=0;i<root.getChildCount();i++)if(root.getChildAt(i) instanceof ScrollView){previousScroll=((ScrollView)root.getChildAt(i)).getScrollY();break;}
  rideHistoryFilter=selectedFilter;
  rideHistorySort=selectedSort;
  render();
  final int restoreY=previousScroll;
  if(root!=null)for(int i=0;i<root.getChildCount();i++)if(root.getChildAt(i) instanceof ScrollView){
   ScrollView view=(ScrollView)root.getChildAt(i);
   view.post(()->{if(!isFinishing()&&!isDestroyed())view.scrollTo(0,restoreY);});
   break;
  }
 }
 void renderPreservingPosition(){
  if(root==null){render();return;}
  int savedY=0;
  for(int i=0;i<root.getChildCount();i++)if(root.getChildAt(i) instanceof ScrollView){
   savedY=((ScrollView)root.getChildAt(i)).getScrollY();break;
  }
  final String sourceTab=tab;final int restoreY=savedY;
  render();
  if(root==null)return;
  for(int i=0;i<root.getChildCount();i++)if(root.getChildAt(i) instanceof ScrollView){
   final ScrollView target=(ScrollView)root.getChildAt(i);
   target.post(()->{
    if(!isFinishing()&&!isDestroyed()&&root!=null&&target.getParent()==root&&sourceTab.equals(tab))
      target.scrollTo(0,restoreY);
   });break;
  }
 }
 void render(){String theme=settings().optString("theme","dark");dark=theme.equals("dark")||(theme.equals("system")&&(getResources().getConfiguration().uiMode&Configuration.UI_MODE_NIGHT_MASK)==Configuration.UI_MODE_NIGHT_YES);
  int[] palette=MotoDesign.colors(dark);bg=palette[0];fg=palette[1];muted=palette[2];line=palette[3];accent=palette[4];panel=palette[5];highlight=palette[6];
getWindow().setStatusBarColor(dark?bg:panel);getWindow().setNavigationBarColor(panel);
 getWindow().getDecorView().setSystemUiVisibility(dark?0:View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
  root=col();root.setBackgroundColor(bg);setContentView(root);root.setOnApplyWindowInsetsListener((v,i)->{if(Build.VERSION.SDK_INT>=30){android.graphics.Insets x=i.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.ime());v.setPadding(x.left,x.top,x.right,x.bottom);}else v.setPadding(i.getSystemWindowInsetLeft(),i.getSystemWindowInsetTop(),i.getSystemWindowInsetRight(),i.getSystemWindowInsetBottom());return i;});root.requestApplyInsets();
  MotoDesign.addHeader(this,root);
  if(Sharing.enabled){
   LinearLayout sharingBar=new LinearLayout(this);sharingBar.setGravity(Gravity.CENTER_VERTICAL);sharingBar.setPadding(dp(20),dp(5),dp(20),dp(7));
   TextView live=text("●  내 위치 공유 중",14,accent,true);sharingBar.addView(live,new LinearLayout.LayoutParams(0,-2,1));
   Button stop=button("공유 중지",false,()->{Sharing.stop();toast("위치 공유를 중지했어요.");render();});stop.setTextSize(13);stop.setMinHeight(dp(40));stop.setBackground(shape(highlight,0,12));sharingBar.addView(stop);root.addView(sharingBar);
  }
  if(!OfflineStatus.hasInternet(this)){
   TextView offline=text("오프라인 · 주유·정비·GPS 기록은 기기에 저장됩니다",12,muted,false);
   offline.setPadding(dp(20),dp(7),dp(20),dp(7));root.addView(offline);
  }
  ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);content=col();content.setPadding(dp(18),dp(8),dp(18),dp(22));scroll.addView(content);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
  LinearLayout dock=col();dock.setPadding(dp(18),dp(7),dp(18),0);
  // Home contains contextual quick actions; keep the bottom dock uncluttered.
  if(tab.equals("차고")&&bike()!=null)pair(dock,"간편 주유",()->new QuickEntryUi(this).fuel(),"정비주기",()->new QuickEntryUi(this).maintenance());
  // Home quick actions are inside its content, not repeated above the navigation.
  root.addView(dock);
  MotoDesign.addNavigation(this,root);
  // Render immediately. Avoid fading the full screen on every tab selection or live GPS refresh.
  if(readOnly)note("저장 오류 · 원본을 보호하고 있습니다");
  // Keep abandoned drafts visible, but give users a deliberate, reversible way to dismiss the reminder.
  if(getSharedPreferences("draft",MODE_PRIVATE).contains("active")&&(tab.equals("홈")||tab.equals("차고"))){
   String draftType="";
   try{draftType=new JSONObject(getSharedPreferences("draft",MODE_PRIVATE).getString("active","{}")).optString("key");}catch(Exception ignored){}
   LinearLayout draftCard=col();draftCard.setPadding(dp(14),dp(12),dp(14),dp(12));draftCard.setBackground(shape(highlight,line,8));
   TextView draftTitle=text("작성 중인 "+DraftBanner.label(draftType)+" 기록",15,fg,true);draftCard.addView(draftTitle);
   TextView draftHelp=text("저장되지 않은 입력이 있어요. 이어서 작성하거나 초안만 삭제할 수 있어요.",12,muted,false);
   LinearLayout.LayoutParams helpLp=new LinearLayout.LayoutParams(-1,-2);helpLp.topMargin=dp(5);draftCard.addView(draftHelp,helpLp);
   LinearLayout draftActions=new LinearLayout(this);draftActions.setGravity(Gravity.CENTER_VERTICAL);
   Button resume=button("이어서 작성",true,this::resumeDraft);resume.setContentDescription("작성 중인 "+DraftBanner.label(draftType)+" 기록 이어서 작성");
   Button discard=button("초안 삭제",false,this::confirmDiscardDraft);discard.setContentDescription("저장되지 않은 초안 삭제");
   LinearLayout.LayoutParams resumeLp=new LinearLayout.LayoutParams(0,-2,1);resumeLp.rightMargin=dp(8);draftActions.addView(resume,resumeLp);
   draftActions.addView(discard,new LinearLayout.LayoutParams(0,-2,1));
   LinearLayout.LayoutParams actionsLp=new LinearLayout.LayoutParams(-1,-2);actionsLp.topMargin=dp(10);draftCard.addView(draftActions,actionsLp);
   content.addView(draftCard);gap(content,10);
  }
  switch(tab){case"홈":home();break;case"라이딩":ride();break;case"차고":garage();break;case"지도":more();break;default:more();}
  if("라이딩".equals(tab))lastRenderedRideState=rideStateSignature();
 }
 String dueText(JSONObject s){return(s.optDouble("nextKm")>0?num(s.optDouble("nextKm"))+" km":"")+(!s.optString("nextDate").isEmpty()?" · "+s.optString("nextDate"):"");}
 void renderMaintenance(JSONObject b){
  Map<String,JSONObject> latest=LatestServiceByType.latest(items("services"),b.optString("id"));
  List<JSONObject> reminders=new ArrayList<>();
  for(JSONObject service:latest.values())if(service.optDouble("nextKm")>0||!service.optString("nextDate").isEmpty())reminders.add(service);
  reminders.sort((x,y)->{
   MaintenancePlanner.Status a=MaintenancePlanner.status(x.optString("nextDate"),x.optDouble("nextKm"),today(),b.optDouble("odo"),30,500);
   MaintenancePlanner.Status c=MaintenancePlanner.status(y.optString("nextDate"),y.optDouble("nextKm"),today(),b.optDouble("odo"),30,500);
   return Integer.compare(MaintenancePlanner.order(a),MaintenancePlanner.order(c));
  });
  int shown=0;
  for(JSONObject service:reminders){
   if(shown++>=3)break;
   MaintenancePlanner.Status status=MaintenancePlanner.status(service.optString("nextDate"),service.optDouble("nextKm"),today(),b.optDouble("odo"),30,500);
   row(service.optString("type"),dueText(service),MaintenancePlanner.caption(status),()->edit("services",service));
  }
  String expiry=b.optString("insuranceDate");
  if(!expiry.isEmpty()){
   MaintenancePlanner.Status status=MaintenancePlanner.status(expiry,0,today(),b.optDouble("odo"),30,500);
   row("보험 만료",expiry,MaintenancePlanner.caption(status),()->edit("vehicles",b));
  }
  if(shown==0&&expiry.isEmpty())label(content,"정비 기록에 다음 점검일이나 주행거리를 설정해 주세요.",15,muted,false);
 }
 void home(){new HubScreens(this).home();}
 void detail(JSONObject x){LinearLayout p=page(x.optString("model"));label(p,won(x.optDouble("price")),30,fg,true);label(p,x.optInt("year")+"년 · "+num(x.optDouble("mileage"))+" km",16,muted,false);label(p,"상태: "+x.optString("status","확인필요")+" (사용자 기록)",16,fg,false);label(p,x.optString("notes"),16,fg,false);if(!x.optString("url").isEmpty())action(p,"판매 페이지 열기 ↗",true,()->open(x.optString("url")));action(p,"매물 수정",false,()->edit("listings",x));label(p,"가격 변화",18,fg,true);JSONArray h=x.optJSONArray("priceHistory");if(h!=null)for(int i=h.length()-1;i>=0;i--){JSONObject a=h.optJSONObject(i);label(p,a.optString("date").split("T")[0]+" · "+won(a.optDouble("price")),16,muted,false);}
  label(p,"구매 전 확인",18,fg,true);for(String c:new String[]{"등록 서류·차대번호 일치","냉간 시동·누유·소모품","사고·전도·튜닝 이력","구조변경·보험·총인수비용"}){CheckBox check=new CheckBox(this);check.setText(c);check.setTextSize(16);check.setTextColor(fg);check.setMinHeight(dp(48));check.setChecked(x.optJSONObject("checks")!=null&&x.optJSONObject("checks").optBoolean(c));check.setOnCheckedChangeListener((v,value)->attempt(()->{JSONObject next=new JSONObject(x.toString());JSONObject checks=next.optJSONObject("checks");if(checks==null)checks=new JSONObject();checks.put(c,value);next.put("checks",checks);saveItem("listings",next);x.put("checks",checks);}));p.addView(check);}action(p,"매물 삭제",false,()->{close();remove("listings",x);});show();
 }
 void garage(){new HubScreens(this).records();}
 void friendsMap(){more();}
 void ride(){
  heading("라이딩","기록 · 내비 · 주행 이력");
  new RideUi(this).render();
 }
 // Saved manual routes are preserved under advanced tools; data is never deleted.
 void savedRoutes(){
  LinearLayout p=page("저장한 코스");
  action(p,"＋ 코스 저장",true,()->edit("routes",null));
  for(JSONObject x:items("routes")){
   action(p,x.optString("name","이름 없는 코스")+" · "+x.optString("end","도착지 미지정"),false,()->edit("routes",x));
   if(!x.optString("end").isEmpty())action(p,"지도에서 목적지 검색 ↗",false,()->open("https://map.naver.com/p/search/"+Uri.encode(x.optString("end"))));
  }
  label(p,"저장된 수동 코스는 유지됩니다. GPX 주행 기록과는 별개입니다.",12,muted,false);
  show();
 }

 void pickGpx(){
  Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);
  i.setType("*/*");i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"application/gpx+xml","application/xml","text/xml","application/octet-stream"});
  startActivityForResult(i,44);
 }
 void openRideInsights(File track){
  if(RideService.running&&track.getName().equals(RideService.active)){toast("기록 종료 후 확인해 주세요.");return;}
  toast("주행 기록을 분석하고 있어요.");
  new Thread(()->{
   try{
    long beforeBytes=track.length(),beforeModified=track.lastModified();
    RideInsights.Result info=RideInsights.analyze(track);
    final String sourceHash=RideMileageIntegrity.validCompleted(track)?RideMileageIntegrity.fingerprint(track):"";
    if(track.length()!=beforeBytes||track.lastModified()!=beforeModified)
     throw new IOException("분석 도중 GPS 원본이 바뀌었어요. 기록을 다시 열어 주세요.");
    boolean undoAvailable=RideTrackMerge.canUndo(getFilesDir(),track);
    runOnUiThread(()->{if(isFinishing()||isDestroyed())return;
     LinearLayout p=page("라이딩 분석");
     label(p,info.distanceLabel(),33,accent,true);
     String elapsed=String.format(Locale.KOREA,"%d시간 %d분",info.durationMillis/3600000,(info.durationMillis/60000)%60);
     label(p,"총 소요 "+elapsed+" · 좌표 "+info.points+"개",16,muted,false);
     LinearLayout row=col();row.setPadding(dp(15),dp(15),dp(15),dp(15));row.setBackground(shape(panel,line,16));
     label(row,String.format(Locale.KOREA,"평균 %.1f km/h · 최고 %.0f km/h",info.averageKmh,info.peakKmh),18,fg,true);
     label(row,String.format(Locale.KOREA,"이동 중 평균 %.1f km/h · GPS 추정 이동 시간 %d분",info.movingAverageKmh,info.movingMillis/60000),13,muted,false);
     label(row,"GPS 관측 구간 "+(info.gpsObservedMillis/60000)+"분 · 시간 관측률 "+info.gpsCoveragePercent()+"% (정확도 40m 이하 기준)",13,muted,false);
     label(row,"GPS 관측 정차 추정 "+(Math.max(0,info.gpsObservedMillis-info.movingMillis)/60000)+"분 · 미관측 "+(Math.max(0,info.durationMillis-info.gpsObservedMillis)/60000)+"분 (터널·신호 소실 포함)",13,muted,false);
     label(row,"평균속도는 총 경과시간 기준 · 오차 좌표 "+info.discarded+"개 제외",12,muted,false);
      label(row,"GPS 신호 공백 "+info.gpsGaps+"구간 · 2분 이상 정차 추정 "+info.restStops+"곳 · 신호대기·휴식 구분 불가",13,muted,false);
      label(row,"GPS 기록 품질: "+info.gpsQuality()+"\n"+info.stationaryConfidence(),13,muted,false);p.addView(row);
     if(new File(track.getPath()+".imported").exists())label(p,"GPX에서 가져온 기록 · 실제 주행 여부는 직접 확인해 주세요.",13,muted,false);
     if(new File(track.getPath()+".recovered").exists())label(p,"중단 후 복구한 GPS 기록 · 마지막 좌표 일부가 누락됐을 수 있어요.",13,muted,false);
     String associatedBike=RideBikeBinding.read(track);
     label(p,"연결된 바이크: "+(associatedBike.isEmpty()?"미지정 (기존 기록)":bikeName(associatedBike)),15,fg,true);
     if(!undoAvailable&&!RideTrackMerge.mergedSource(track)&&!RideTrackMerge.archived(track))action(p,"이 기록의 바이크 지정·변경",false,()->{
      JSONObject appliedRides=settings().optJSONObject("appliedRides");
      if(appliedRides!=null&&appliedRides.has(track.getName())){
       error("이미 주행거리에 반영된 기록이에요. 바이크 연결을 변경하면 계기판 이력이 어긋날 수 있어 변경을 막았어요.");return;
      }
      List<JSONObject> vehicles=items("vehicles");
      if(vehicles.isEmpty()){toast("차고에 바이크를 먼저 등록해 주세요.");return;}
      String[] names=new String[vehicles.size()];
      for(int i=0;i<vehicles.size();i++)names[i]=vehicles.get(i).optString("name")+" · "+vehicles.get(i).optInt("year")+"년식";
      new AlertDialog.Builder(this).setTitle("이 라이딩을 기록한 바이크")
       .setMessage("로컬 라이딩 기록에만 연결하며 계기판 주행거리는 변경하지 않습니다.")
       .setItems(names,(dialog,which)->attempt(()->{
        if(RideService.running&&track.getName().equals(RideService.active))throw new IOException("주행이 끝난 뒤 변경해 주세요.");
        RideBikeBinding.write(track,vehicles.get(which).optString("id"));
        close();openRideInsights(track);
       })).setNegativeButton("취소",null).show();
     });
     label(p,"라이딩 유형: "+RideCategory.label(track,info)+(RideCategory.AUTO.equals(RideCategory.read(track))?" · 자동 추천":" · 직접 선택"),15,accent,true);
     action(p,"라이딩 유형 변경 (출퇴근·와인딩·장거리)",false,()->{
      final String[] categories=RideCategory.OPTIONS;
      int checked=java.util.Arrays.asList(categories).indexOf(RideCategory.read(track));
      new AlertDialog.Builder(this).setTitle("라이딩 유형 지정 · 내 기기에만 저장")
       .setSingleChoiceItems(categories,Math.max(0,checked),(dialog,which)->{
        dialog.dismiss();
        try{RideCategory.write(track,categories[which]);close();openRideInsights(track);}
        catch(IOException ex){error("분류 저장 실패: "+ex.getMessage());}
       }).setNegativeButton("취소",null).show();
     });
     if(RideTrackMerge.archived(track))label(p,"통합 취소로 보관된 경로 · 월간 통계에서 제외",13,muted,false);
     if(RideTrackMerge.mergedSource(track))label(p,"통합에 포함된 원본 · 통합 취소 후 통계에 복원됩니다.",13,muted,false);
     JSONObject appliedForUndo=settings().optJSONObject("appliedRides");
     boolean appliedMerged=appliedForUndo!=null&&appliedForUndo.has(track.getName());
     if(undoAvailable&&appliedMerged)label(p,"통합 라이딩 거리를 이미 차고에 반영했어요. 원본을 중복 반영하지 않도록 통합 취소는 제한됩니다.",13,muted,false);
     if(undoAvailable&&!appliedMerged)action(p,"통합 취소 · 원본 2건 복원",false,()->new AlertDialog.Builder(this)
      .setTitle("라이딩 통합을 취소할까요?")
      .setMessage("원본 2건을 월간 통계에 다시 포함하고 통합본은 보관합니다. GPS 좌표는 어느 쪽도 삭제하지 않습니다.")
      .setNegativeButton("유지",null).setPositiveButton("통합 취소",(d,w)->{
       JSONObject liveLedger=settings().optJSONObject("appliedRides");
       if(liveLedger!=null&&liveLedger.has(track.getName())){error("누적거리에 반영한 통합본은 취소할 수 없어요. 중복 계산을 방지합니다.");return;}
       new Thread(()->{
       try{RideTrackMerge.undo(getFilesDir(),track);
        runOnUiThread(()->{if(!isFinishing()&&!isDestroyed()){close();render();toast("통합 취소 · 원본 2건 복원, 통합본 보관");}});
       }catch(Exception e){runOnUiThread(()->{if(!isFinishing()&&!isDestroyed())error("통합 취소 실패: "+e.getMessage());});}
      },"moto-unmerge").start();}).show());
     gap(p,16);
     action(p,"지도에서 경로 보기",true,()->{Intent i=new Intent(this,RideMapActivity.class);i.putExtra("track",track.getName());startActivity(i);});
     action(p,"GPX 파일로 내보내기",false,()->attempt(()->{
      if(TrackFile.read(track,(x,y,z,t)->{})==0)throw new IOException("저장된 좌표가 없습니다.");
      pendingTrack=track.getName();Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("application/gpx+xml").putExtra(Intent.EXTRA_TITLE,track.getName().replace(".track",".gpx"));
      startActivityForResult(i,43);
     }));
     JSONObject b=bike();JSONObject ledger=settings().optJSONObject("appliedRides");
     boolean applied=ledger!=null&&ledger.has(track.getName());
     if(b!=null){
      if(applied)label(p,"✓ 이미 누적 주행거리에 반영한 기록",14,accent,true);
      else if(RideMileageIntegrity.validCompleted(track))
       action(p,"주행거리 확인 후 정비주기 반영",false,()->confirmRideMileage(track,info,sourceHash));
      else label(p,"통합 원본·보관본 또는 미완료 기록은 누적 주행거리에 반영할 수 없어요.",13,muted,false);
     }
     action(p,"기록 삭제",false,()->{
      if(!RideTrackMerge.canDelete(getFilesDir(),track)){
       error("통합된 기록입니다. 먼저 통합을 취소한 뒤 삭제해 주세요.");return;
      }
      new AlertDialog.Builder(this).setTitle("이 라이딩을 삭제할까요?")
      .setMessage("GPX 파일을 백업했다면 보관해 주세요. 이미 반영한 계기판 주행거리는 자동으로 줄어들지 않습니다.")
      .setNegativeButton("취소",null).setPositiveButton("삭제",(d,w)->{
       if(!track.delete()){toast("기록 파일 삭제 실패");return;}
       new File(track.getPath()+".done").delete();new File(track.getPath()+".imported").delete();new File(track.getPath()+".recovered").delete();
       new File(track.getPath()+".category").delete();new File(track.getPath()+".bike").delete();new File(track.getPath()+".unmerged").delete();
       close();render();toast("주행 기록을 삭제했어요.");
      }).show();});
     show();
    });
   }catch(Exception e){runOnUiThread(()->{if(!isFinishing()&&!isDestroyed())error("분석에 실패했어요: "+e.getMessage());});}
  },"MOTO-ride-insights").start();
 }
 void confirmRideMileage(File track,RideInsights.Result info,String sourceHash){
  JSONObject b=bike();if(b==null){toast("바이크를 먼저 등록해 주세요.");return;}
  if(info.points<2||info.distanceKm<0.01||info.distanceKm>1000){error("이 기록의 GPS 거리가 정상 범위가 아니어서 적용할 수 없어요.");return;}
  if(!RideMileageIntegrity.validCompleted(track)){error("미완료·통합 원본·보관된 라이딩은 반영할 수 없어요.");return;}
  final long sourceBytes=track.length(),sourceModified=track.lastModified();
  String bikeId=b.optString("id");
  String rideBikeId;
  try{rideBikeId=RideBikeBinding.readStrict(track);}catch(IOException e){error(e.getMessage());return;}
  if(!rideBikeId.isEmpty()&&!rideBikeId.equals(bikeId)){error("이 기록은 "+bikeName(rideBikeId)+"에 연결돼 있어요. 차고에서 해당 바이크를 선택한 뒤 반영하세요.");return;}
  double old=b.optDouble("odo");
  new AlertDialog.Builder(this).setTitle("주행거리 반영")
   .setMessage(b.optString("name")+"의 누적 주행거리에 GPS 계산 거리 "+String.format(Locale.KOREA,"%.1f km",info.distanceKm)+"를 추가할까요?\n\n"
    +num(old)+" km → "+num(old+info.distanceKm)+" km\n\nGPS 오차가 있으므로 실제 계기판을 먼저 확인하세요. 가져온 GPX도 포함해서 중복 반영하지 마세요.")
   .setNegativeButton("취소",null).setPositiveButton("반영",(d,w)->{
    toast("GPS 기록의 내용과 저장 상태를 확인하고 있어요.");
    new Thread(()->{
     String errorText=null;
     try{
      RideMileageIntegrity.requireApplyable(track,false,TrackFile.directory(getFilesDir()),
       sourceBytes,sourceModified,sourceHash);
     }catch(Exception ex){errorText=ex.getMessage()==null?"GPS 기록 검증에 실패했어요.":ex.getMessage();}
     final String failure=errorText;
     runOnUiThread(()->{
      if(isFinishing()||isDestroyed())return;
      if(failure!=null){error(failure);return;}
      attempt(()->{
    JSONObject next=new JSONObject(db.toString());JSONObject settings=next.getJSONObject("settings");
    JSONObject ledger=settings.optJSONObject("appliedRides");if(ledger==null){ledger=new JSONObject();settings.put("appliedRides",ledger);}
    RideMileageIntegrity.requireApplyable(track,ledger.has(track.getName()),
     TrackFile.directory(getFilesDir()),sourceBytes,sourceModified);
    String latestRideBike=RideBikeBinding.readStrict(track);
    if(!latestRideBike.isEmpty()&&!latestRideBike.equals(bikeId))throw new Exception("라이딩 바이크가 변경됐어요. 다시 확인해 주세요.");
    JSONObject target=Store.find(next,"vehicles",bikeId);if(target==null)throw new Exception("바이크가 삭제됐어요.");
    if(Double.compare(target.optDouble("odo"),old)!=0)throw new IOException("계기판 주행거리가 변경됐어요. 다시 확인한 뒤 반영해 주세요.");
    target.put("odo",Math.round((old+info.distanceKm)*10)/10.0);
    ledger.put(track.getName(),new JSONObject().put("vehicleId",bikeId).put("km",info.distanceKm));
    write(next);DailyMaintenanceJob.checkSoon(this);close();render();toast("주행거리와 정비 예정 정보를 갱신했어요.");
      });
     });
    },"moto-ride-mileage-sha256").start();
   }).show();
 }
 EditText costField(LinearLayout parent,String title,String starting){
  label(parent,title,15,fg,true);
  EditText input=new EditText(this);input.setText(starting);input.setTextSize(18);input.setTextColor(fg);input.setHintTextColor(muted);
  input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
  input.setSingleLine(true);input.setMinimumHeight(dp(52));input.setPadding(dp(12),dp(8),dp(12),dp(8));input.setBackground(shape(panel,line,10));
  input.setContentDescription(title);
  LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(16);parent.addView(input,lp);return input;
 }
 long money(EditText e){String value=e.getText().toString().trim();return value.isEmpty()?0:Long.parseLong(value);}
 void tradeIn(){
  LinearLayout p=page("바이크 기변 계산기");
  label(p,"금액은 모두 원 단위야. 등록 비용과 초기 정비비는 직접 확인한 예상값을 넣어줘.",15,muted,false);
  EditText price=costField(p,"새 바이크 구매가", "0");
  EditText sale=costField(p,"기존 바이크 예상 판매가", "0");
  EditText register=costField(p,"취득·등록 및 명의이전 예상비", "0");
  EditText delivery=costField(p,"운송·탁송 비용", "0");
  EditText maintenance=costField(p,"구매 직후 정비 예산", "0");
  EditText equipment=costField(p,"장비·기타 비용", "0");
  TextView answer=text("금액을 입력한 뒤 계산을 눌러줘.",20,fg,true);
  LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(14);lp.bottomMargin=dp(16);p.addView(answer,lp);
  action(p,"총 기변 비용 계산",true,()->attempt(()->{
   TradeInCalculator.Result result=TradeInCalculator.calculate(money(price),money(register),money(delivery),money(maintenance),money(equipment),money(sale));
   answer.setText("총 취득비: "+won(result.totalPurchaseCost)+"\n기존 바이크 처분 예상액: "+won(result.expectedSale)+"\n추가로 필요한 현금: "+won(result.additionalCashNeeded)+(result.cashRemaining>0?"\n남는 현금: "+won(result.cashRemaining):""));
  }));
  label(p,"※ 실제 세금·보험료·이전 비용·시세가 자동 조회되는 기능은 아니야.",14,muted,false);
  show();
 }
 void more(){new HubScreens(this).settings();}
 void choose(String title,String[] values,Choice c){new AlertDialog.Builder(this).setTitle(title).setItems(values,(d,i)->c.pick(values[i])).show();}
 void open(String url){if(!Store.safeUrl(url)){error("올바른 웹 주소가 아닙니다.");return;}attempt(()->startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url))));}
 void close(){hideKeyboard();boolean dirty=formDirty;if(dialog!=null){persistDraft();dialog.dismiss();dialog=null;}draftFields=null;draftSelectors=null;fullTankBox=null;ocrFields=null;ocrKind="";quickOdoField=null;ocrDateMileageFields=null;ocrMetaSummary=null;formOptionalSection=null;formOptionalToggle=null;formOptionalKeys.clear();formDirty=false;if(dirty)render();}
 LinearLayout page(String title){close();dialog=new Dialog(this);LinearLayout outer=col();outer.setBackgroundColor(bg);outer.setPadding(dp(20),dp(14),dp(20),dp(14));LinearLayout bar=new LinearLayout(this);bar.setGravity(Gravity.CENTER_VERTICAL);TextView caption=text(title,24,fg,true);bar.addView(caption,new LinearLayout.LayoutParams(0,-2,1));Button done=button("닫기",false,this::close);done.setMinHeight(dp(42));done.setTextSize(13);done.setBackground(shape(panel,0,12));bar.addView(done);outer.addView(bar);View separator=new View(this);separator.setBackgroundColor(line);LinearLayout.LayoutParams sep=new LinearLayout.LayoutParams(-1,dp(1));sep.topMargin=dp(15);outer.addView(separator,sep);ScrollView scroll=new ScrollView(this);formBody=col();formBody.setPadding(0,dp(20),0,dp(20));scroll.addView(formBody);outer.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));formFooter=col();outer.addView(formFooter);dialog.setContentView(outer);outer.setOnApplyWindowInsetsListener((v,i)->{if(Build.VERSION.SDK_INT>=30){android.graphics.Insets in=i.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.ime());v.setPadding(dp(20)+in.left,dp(12)+in.top,dp(20)+in.right,dp(12)+in.bottom);}return i;});return formBody;}
 void show(){dialog.setOnDismissListener(d->{persistDraft();if(formDirty)render();});dialog.show();Window w=dialog.getWindow();w.setLayout(-1,-1);w.setStatusBarColor(bg);w.setNavigationBarColor(bg);w.getDecorView().setSystemUiVisibility(dark?0:View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE|WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);w.getDecorView().requestApplyInsets();}
 final Set<String> numbers=new HashSet<>(Arrays.asList("odo","year","purchase","price","mileage","cost","amount","liters","nextKm","distance"));
 // Low-friction entry: common values are explicitly selected; no silent guesses or remote lookups.
 void chips(LinearLayout parent,String[] labels,String[] values,EditText target){
  HorizontalScrollView scroll=new HorizontalScrollView(this);scroll.setHorizontalScrollBarEnabled(false);scroll.setFillViewport(false);
  LinearLayout bar=new LinearLayout(this);bar.setGravity(Gravity.CENTER_VERTICAL);List<TextView> choices=new ArrayList<>();
  for(int i=0;i<labels.length;i++){
   final String value=values[i];TextView chip=text(labels[i],14,fg,true);chip.setGravity(Gravity.CENTER);chip.setMinHeight(dp(42));chip.setPadding(dp(15),dp(9),dp(15),dp(9));
   chip.setBackground(shape(panel,line,12));chip.setClickable(true);chip.setFocusable(true);
   chip.setOnClickListener(v->{if(value.isEmpty()){pickDate(target);return;}if("@clear".equals(value)){target.setText("");return;}target.setText(value);target.setSelection(target.length());});
   LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,-2);lp.rightMargin=dp(8);bar.addView(chip,lp);choices.add(chip);
  }
  target.addTextChangedListener(new android.text.TextWatcher(){
   public void beforeTextChanged(CharSequence a,int b,int c,int d){}
   public void onTextChanged(CharSequence a,int b,int c,int d){String selected=a.toString().trim();for(int i=0;i<choices.size();i++){boolean on=(!values[i].isEmpty() && selected.equals(values[i])) || ("@clear".equals(values[i]) && selected.isEmpty());TextView t=choices.get(i);t.setTextColor(on?accent:fg);t.setBackground(shape(on?highlight:panel,on?accent:line,12));}}
   public void afterTextChanged(android.text.Editable e){}
  });
  scroll.addView(bar);LinearLayout.LayoutParams row=new LinearLayout.LayoutParams(-1,-2);row.bottomMargin=dp(10);parent.addView(scroll,row);
 }
 void pickDate(EditText e){
  LocalDate value;try{value=LocalDate.parse(e.getText().toString().trim());}catch(Exception ignore){value=LocalDate.now(ZoneId.of("Asia/Seoul"));}
  DatePickerDialog picker=new DatePickerDialog(this,(view,year,month,day)->e.setText(LocalDate.of(year,month+1,day).toString()),value.getYear(),value.getMonthValue()-1,value.getDayOfMonth());
  picker.show();
 }
 String[] modelSuggestions(){
  LinkedHashSet<String> suggestions=new LinkedHashSet<>();
  for(JSONObject b:items("vehicles"))if(!b.optString("name").isEmpty())suggestions.add(b.optString("name"));
  for(JSONObject m:items("listings"))if(!m.optString("model").isEmpty())suggestions.add(m.optString("model"));
  suggestions.addAll(Arrays.asList("슈퍼커브 110","390 DUKE","YZF-R3","Ninja 400","MT-03","CBR500R","ZX-4RR","PCX 125"));
  ArrayList<String> list=new ArrayList<>(suggestions);return list.subList(0,Math.min(14,list.size())).toArray(new String[0]);
 }
 void helpers(LinearLayout parent,String form,String key,EditText e,JSONObject previous,JSONObject currentBike){
  if(DateFieldChoices.isDateField(key)){
   e.setFocusable(false);e.setClickable(true);e.setOnClickListener(v->pickDate(e));
   // The text field itself opens the calendar. Optional dates also need a way to unset them.
   boolean optional=DateFieldChoices.canClear(form,key);
   if(optional){
    chips(parent,new String[]{"오늘","어제","날짜 선택","선택 안 함"},
     new String[]{today(),LocalDate.now(ZoneId.of("Asia/Seoul")).minusDays(1).toString(),"","@clear"},e);
   }else{
    chips(parent,new String[]{"오늘","어제","날짜 선택"},
     new String[]{today(),LocalDate.now(ZoneId.of("Asia/Seoul")).minusDays(1).toString(),""},e);
   }
  }
  if(key.equals("type")&&form.equals("services"))chips(parent,
   new String[]{"엔진오일","오일필터","타이어","브레이크 패드","체인","냉각수","배터리"},
   new String[]{"엔진오일","오일필터","타이어","브레이크 패드","체인","냉각수","배터리"},e);
  if(key.equals("name")&&form.equals("vehicles")||key.equals("model")&&form.equals("listings")){
   String[] names=modelSuggestions();chips(parent,names,names,e);
  }
  if(key.equals("year")){
   String y=String.valueOf(LocalDate.now().getYear());String[] values={y,String.valueOf(LocalDate.now().getYear()-1),String.valueOf(LocalDate.now().getYear()-2),"2024","2023","2022"};
   chips(parent,values,values,e);
  }
  if(key.equals("mileage")&&currentBike!=null&&(form.equals("services")||form.equals("expenses"))){
   String km=String.valueOf((long)currentBike.optDouble("odo"));chips(parent,new String[]{"현재 계기판 "+km+" km"},new String[]{km},e);
  }
  if(key.equals("amount")&&form.equals("expenses"))chips(parent,
   new String[]{"1만원","2만원","3만원","5만원","10만원"},new String[]{"10000","20000","30000","50000","100000"},e);
  if(key.equals("cost")&&form.equals("services"))chips(parent,
   new String[]{"1만원","3만원","5만원","10만원","20만원"},new String[]{"10000","30000","50000","100000","200000"},e);
  if(key.equals("purchase")&&form.equals("vehicles"))chips(parent,
   new String[]{"100만","300만","500만","700만","1000만"},new String[]{"1000000","3000000","5000000","7000000","10000000"},e);
  if(key.equals("price")&&form.equals("listings"))chips(parent,
   new String[]{"100만","300만","500만","700만","1000만"},new String[]{"1000000","3000000","5000000","7000000","10000000"},e);
  if(key.equals("date")&&(form.equals("expenses")||form.equals("services")))label(parent,"저장 전 날짜와 계기판을 확인하세요. 이전 기록 불러오기는 이 두 항목을 바꾸지 않아요.",12,muted,false);
  if(key.equals("liters")&&form.equals("expenses"))chips(parent,
   new String[]{"2L","3L","4L","5L","6L","10L"},new String[]{"2","3","4","5","6","10"},e);
  if(key.equals("type")&&form.equals("services")){
   String[] common={"엔진오일","오일필터","체인","브레이크 패드","타이어","냉각수","기타"};
   chips(parent,common,common,e);
  }
  if(key.equals("nextDate")&&form.equals("services"))label(parent,"다음 정비일과 거리 중 하나만 설정해도 알림을 받을 수 있어요.",12,muted,false);
  if(key.equals("nextKm")&&form.equals("services")&&currentBike!=null){
   Button interval=button("정비 간격 선택 · 확인 후 적용",false,()->{
    new AlertDialog.Builder(this).setTitle("다음 정비 거리 예시")
     .setMessage("제조사별 공식 정비주기가 아닙니다. 서비스 매뉴얼을 확인하고 직접 선택하세요.")
     .setItems(MaintenancePreset.LABELS,(d,choice)->attempt(()->{
      double current=Double.parseDouble(e.getText().toString().isEmpty()?"0":e.getText().toString());
      EditText km=draftFields==null?null:draftFields.get("mileage");
      if(km!=null&&!km.getText().toString().trim().isEmpty())current=Double.parseDouble(km.getText().toString().trim());
      e.setText(String.valueOf((long)MaintenancePreset.next(current,choice)));
     })).setNegativeButton("취소",null).show();
   });parent.addView(interval);

   long km=(long)currentBike.optDouble("odo");
   chips(parent,new String[]{"설정 안 함","+1,000km","+3,000km","+5,000km"},new String[]{"0",String.valueOf(km+1000),String.valueOf(km+3000),String.valueOf(km+5000)},e);
   label(parent,"정비주기 예시는 선택 편의를 위한 값이며 제조사 권장 주기가 아닙니다.",12,muted,false);
  }
  if(key.equals("region")&&form.equals("listings")){
   String[] names={"서울","경기","인천","충북","충남","강원","대전","대구","부산","광주","경북","경남"};chips(parent,names,names,e);
  }
 }
 void recentRecordShortcut(LinearLayout p,String key,Map<String,EditText> f,Map<String,Spinner> sels,JSONObject b){
  if(!key.equals("expenses")&&!key.equals("services")||b==null)return;
  JSONObject prev=null;
  for(JSONObject x:items(key))if(x.optString("vehicleId").equals(b.optString("id"))){
   if(prev==null||RecentRecordReuse.isNewer(x.optString("date"),x.optDouble("mileage"),prev.optString("date"),prev.optDouble("mileage")))prev=x;
  }
  if(prev==null)return;
  // Always expose a dedicated validated fuel template, regardless of the latest expense category.
  // Copy values only on an explicit tap: odometer and date stay as entered today.
  if(key.equals("expenses")){
   JSONObject fuel=null;
   for(JSONObject x:items("expenses"))if(b.optString("id").equals(x.optString("vehicleId"))&&"주유".equals(x.optString("type"))){
    try{FuelRecordValidation.requireFuel(x.optDouble("amount",Double.NaN),x.optDouble("liters",Double.NaN));}
    catch(Exception ignored){continue;}
    if(fuel==null||RecentRecordReuse.isNewer(x.optString("date"),x.optDouble("mileage"),fuel.optString("date"),fuel.optDouble("mileage")))fuel=x;
   }
   if(fuel!=null){final JSONObject recentFuel=fuel;
    Button fill=button("⛽  최근 주유값으로 채우기",true,()->{
     EditText amount=f.get("amount"),liters=f.get("liters");
     if(amount!=null)amount.setText(recentFuel.optString("amount"));
     if(liters!=null)liters.setText(recentFuel.optString("liters"));
     Spinner type=sels.get("type");if(type!=null)for(int i=0;i<type.getCount();i++)if("주유".equals(type.getItemAtPosition(i).toString())){type.setSelection(i);break;}
     if(fullTankBox!=null)fullTankBox.setChecked(recentFuel.optBoolean("fullTank"));
     toast("주유량·금액만 복사했어요. 오늘 날짜와 계기판은 유지합니다.");
    });
    fill.setContentDescription("이전 주유 기록의 금액과 리터만 불러오기. 날짜와 주행거리는 유지");
    LinearLayout.LayoutParams fp=new LinearLayout.LayoutParams(-1,-2);fp.bottomMargin=dp(8);p.addView(fill,fp);
   }
  }
  final JSONObject last=prev;
  String lastDate=last.optString("date");
  String buttonLabel="↻  최근 "+(key.equals("expenses")?"비용":"정비")+" 기록 불러오기";
  Button reuse=button(buttonLabel,false,()->{
   // Preserve the new transaction date and odometer; never reuse an expired due date.
   for(String field:new String[]{"type","amount","liters","cost","note"}){
    EditText e=f.get(field);if(e!=null&&last.has(field))e.setText(last.optString(field));
    Spinner spin=sels.get(field);if(spin!=null){String selected=last.optString(field);for(int i=0;i<spin.getCount();i++)if(selected.equals(spin.getItemAtPosition(i).toString()))spin.setSelection(i);}
   }
   if(key.equals("services")){
    EditText next=f.get("nextKm");
    if(next!=null){
     EditText current=f.get("mileage");
     double km=b.optDouble("odo");
     if(current!=null)try{km=Double.parseDouble(current.getText().toString().trim());}catch(Exception ignored){}
     long due=RecentRecordReuse.nextServiceKm(last.optDouble("mileage",Double.NaN),last.optDouble("nextKm",Double.NaN),km);
     next.setText(String.valueOf(due));
    }
    EditText dueDate=f.get("nextDate");if(dueDate!=null)dueDate.setText("");
   }
   if(fullTankBox!=null&&key.equals("expenses"))fullTankBox.setChecked("주유".equals(last.optString("type"))&&last.optBoolean("fullTank"));
   if(formOptionalSection!=null&&(key.equals("services")||hasPopulatedOptionalFields(f))){
    formOptionalSection.setVisibility(View.VISIBLE);updateOptionalButton();
   }
   toast(key.equals("services")?"날짜·주행거리 유지 · 다음 정비 거리는 이전 간격으로 재계산했어요.":"날짜·주행거리는 오늘 기준으로 유지했어요.");
  });
  reuse.setContentDescription("최근 "+(key.equals("expenses")?"비용":"정비")+" 기록 "+lastDate+"의 입력값 재사용");
  LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(12);p.addView(reuse,lp);
  label(p,"최근 기록: "+lastDate+" · "+num(last.optDouble("mileage"))+" km"+(key.equals("services")?"  |  다음 정비 간격 자동 계산":""),12,muted,false);
 }

 /** Small, explicit buttons rather than storing guesses or reusing another motorcycle's values. */
 void quickRecordTools(LinearLayout target,String key,Map<String,EditText> inputs,Map<String,Spinner> selectors,JSONObject currentBike){
  if(currentBike==null||!key.equals("expenses")&&!key.equals("services"))return;
  String bikeId=currentBike.optString("id");
  if(key.equals("expenses")){
   EditText money=inputs.get("amount"),liters=inputs.get("liters");
   if(money==null||liters==null)return;
   JSONObject lastFuel=null;
   for(JSONObject entry:items("expenses")){
    if(!bikeId.equals(entry.optString("vehicleId"))||!"주유".equals(entry.optString("type")))continue;
    try{FuelRecordValidation.requireFuel(entry.optDouble("amount",Double.NaN),entry.optDouble("liters",Double.NaN));}
    catch(Exception ignored){continue;}
    if(lastFuel==null||RecentRecordReuse.isNewer(entry.optString("date"),entry.optDouble("mileage"),lastFuel.optString("date"),lastFuel.optDouble("mileage")))lastFuel=entry;
   }
   long lastUnit=lastFuel==null?0:QuickLogMath.unitPrice(lastFuel.optDouble("amount"),lastFuel.optDouble("liters"));
   label(target,"주유 빠른 계산 (선택)",15,fg,true);
   label(target,"총금액 또는 주유량과 L당 가격을 입력하고 계산하세요. 추정값이므로 영수증과 대조 후 저장하세요.",12,muted,false);
   EditText price=new EditText(this);price.setSingleLine(true);price.setInputType(android.text.InputType.TYPE_CLASS_NUMBER|android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
   price.setText(lastUnit>0?String.valueOf(lastUnit):"");price.setHint("L당 가격 (원) · 직접 수정 가능");price.setContentDescription("리터당 가격, 영수증 기준으로 수정");
   price.setTextColor(fg);price.setHintTextColor(muted);price.setBackground(shape(panel,line,13));price.setPadding(dp(12),dp(10),dp(12),dp(10));
   target.addView(price,new LinearLayout.LayoutParams(-1,dp(53)));
   pair(target,"금액 → 주유량",()->{
    String result=QuickLogMath.litersFromAmount(money.getText().toString(),price.getText().toString());
    if(result.isEmpty()){toast("총금액과 리터당 가격을 확인해 주세요.");return;}
    Spinner type=selectors.get("type");if(type!=null)type.setSelection(0);
    liters.setText(result);toast("약 "+result+" L · 실제 주유량과 확인해 주세요.");
   },"주유량 → 금액",()->{
    String result=QuickLogMath.amountFromLiters(liters.getText().toString(),price.getText().toString());
    if(result.isEmpty()){toast("주유량과 리터당 가격을 확인해 주세요.");return;}
    Spinner type=selectors.get("type");if(type!=null)type.setSelection(0);
    money.setText(result);toast("약 "+result+"원 · 실제 결제액을 확인해 주세요.");
   });
   label(target,lastUnit>0?"최근 이 바이크의 L당 가격 약 "+lastUnit+"원 · 자동 적용하지 않습니다.":"이 바이크의 최근 정상 주유 내역이 없어요. L당 가격을 직접 입력하세요.",12,muted,false);
   Button fullQuick=button("가득 주유로 표시",false,()->{
    Spinner type=selectors.get("type");if(type!=null)type.setSelection(0);
    if(fullTankBox!=null){fullTankBox.setChecked(true);toast("가득 주유를 선택했어요. 주유량을 확인해 주세요.");}
   });target.addView(fullQuick);

  }else{
   EditText type=inputs.get("type"),cost=inputs.get("cost"),nextKm=inputs.get("nextKm");
   if(type==null||cost==null)return;
   label(target,"정비 항목 빠른 선택",15,fg,true);
   String[] types={"엔진오일","오일필터","체인","브레이크 패드","타이어","냉각수"};
   chips(target,types,types,type);
   Button copy=button("↻ 같은 바이크 · 같은 항목의 최근 기록 불러오기",false,()->{
    JSONObject previous=QuickServiceHistory.latest(items("services"),bikeId,type.getText().toString().trim());
    if(previous==null){toast("이 바이크의 해당 정비 기록이 없습니다.");return;}
    cost.setText(previous.optString("cost"));EditText note=inputs.get("note");
    if(note!=null)note.setText(previous.optString("note"));
    if(nextKm!=null){double km=currentBike.optDouble("odo");
     try{km=Double.parseDouble(inputs.get("mileage").getText().toString().trim());}catch(Exception ignored){}
     nextKm.setText(String.valueOf(RecentRecordReuse.nextServiceKm(previous.optDouble("mileage",Double.NaN),previous.optDouble("nextKm",Double.NaN),km)));
    }
    EditText nextDate=inputs.get("nextDate");if(nextDate!=null)nextDate.setText("");
    // A copied maintenance target is never hidden: show all optional fields for review.
    if(formOptionalSection!=null){formOptionalSection.setVisibility(View.VISIBLE);updateOptionalButton();}
    toast("이전 비용·메모만 복사했어요. 날짜·계기판은 현재값이고 정비 거리는 재계산했습니다.");
   });copy.setContentDescription("선택한 정비 항목의 이전 기록을 이 바이크에서만 찾기");target.addView(copy);
   Button diy=button("직접 작업 · 비용 0원",false,()->{cost.setText("0");toast("직접 정비 비용을 0원으로 입력했어요.");});target.addView(diy);
   label(target,"비용과 다음 정비 거리는 확인 후 저장하세요. 차량별 제조사 주기를 자동 가정하지 않습니다.",12,muted,false);
  }
  gap(target,12);
 }
 void field(LinearLayout p,Map<String,EditText> f,JSONObject old,String key,String label,String def){label(p,label,14,muted,true);EditText e=new EditText(this);e.setContentDescription(label);e.setTextSize(16);e.setTextColor(fg);e.setHintTextColor(muted);e.setPadding(dp(12),dp(10),dp(12),dp(10));e.setMinHeight(dp(52));e.setInputType(numbers.contains(key)?android.text.InputType.TYPE_CLASS_NUMBER|android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL:android.text.InputType.TYPE_CLASS_TEXT|(key.equals("url")?android.text.InputType.TYPE_TEXT_VARIATION_URI:android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE));e.setText(old.has(key)?old.optString(key):def);e.setBackground(shape(panel,line,13));e.setHint(key.equals("date")?"날짜를 선택하세요":key.equals("model")?"모델명 직접 입력":key.equals("name")&&activeForm.equals("vehicles")?"바이크 이름 직접 입력":key.equals("year")?"연식 4자리 입력 또는 모델 선택":key.equals("type")?"항목 직접 입력":"직접 입력");f.put(key,e);e.setSelectAllOnFocus(numbers.contains(key));helpers(p,activeForm,key,e,activeOld,bike());LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(20);p.addView(e,lp);}
 void selector(LinearLayout p,Map<String,Spinner> f,String key,String title,String[] values,String value){label(p,title,14,muted,true);Spinner s=new Spinner(this);ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,values){@Override public View getView(int position,View convert,ViewGroup parent){TextView t=(TextView)super.getView(position,convert,parent);t.setTextColor(fg);t.setTextSize(16);return t;}};s.setAdapter(adapter);for(int i=0;i<values.length;i++)if(values[i].equals(value))s.setSelection(i);p.addView(s,new LinearLayout.LayoutParams(-1,dp(56)));gap(p,12);f.put(key,s);if(key.equals("type")&&activeForm.equals("expenses")){
   String[] options={"주유","보험","부품","기타"};
   HorizontalScrollView scroll=new HorizontalScrollView(this);scroll.setHorizontalScrollBarEnabled(false);LinearLayout bar=new LinearLayout(this);
   for(int i=0;i<options.length;i++){final int index=i;Button b=button(options[i],false,()->s.setSelection(index));b.setTextSize(14);b.setMinHeight(dp(40));LinearLayout.LayoutParams l=new LinearLayout.LayoutParams(-2,-2);l.rightMargin=dp(4);bar.addView(b,l);}
   scroll.addView(bar);p.addView(scroll,new LinearLayout.LayoutParams(-1,-2));gap(p,8);
  }}
 // Catalog wizard: brand -> model -> year, with search; keyboard is optional.
 void searchChoices(String title,List<String> candidates,Choice picked,Runnable network){
  searchChoices(title,candidates,Collections.emptyMap(),picked,network);
 }
 void searchChoices(String title,List<String> candidates,Map<String,List<String>> aliases,Choice picked,Runnable network){
  LinearLayout wrap=col();wrap.setPadding(dp(16),dp(9),dp(16),dp(10));
  EditText search=new EditText(this);search.setSingleLine(true);search.setTextColor(fg);search.setHintTextColor(muted);search.setHint("검색 (한글·영문·별칭)");search.setBackground(shape(panel,line,12));search.setPadding(dp(14),dp(8),dp(14),dp(8));wrap.addView(search,new LinearLayout.LayoutParams(-1,dp(52)));
  ListView list=new ListView(this);ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_list_item_1,new ArrayList<>(candidates));list.setAdapter(adapter);list.setDividerHeight(dp(1));list.setBackgroundColor(bg);wrap.addView(list,new LinearLayout.LayoutParams(-1,dp(365)));
  TextView hint=text("검색 후 선택하거나, 목록에 없으면 직접 입력하세요.",12,muted,false);hint.setPadding(0,dp(8),0,dp(6));wrap.addView(hint);
  AlertDialog dialog=new AlertDialog.Builder(this).setTitle(title).setView(wrap).setNegativeButton("취소",null).setNeutralButton("직접 입력",null).create();
  dialog.setOnShowListener(ignored->dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v->{
   String value=search.getText().toString().trim();
   if(value.isEmpty()){search.setError("브랜드 또는 모델명을 입력해 주세요.");search.requestFocus();return;}
   dialog.dismiss();picked.pick(value);
  }));
  if(network!=null){Button online=button("온라인 모델 검색 · NHTSA",false,network);online.setMinHeight(dp(43));wrap.addView(online);}
  list.setOnItemClickListener((parent,v,pos,id)->{picked.pick(adapter.getItem(pos));dialog.dismiss();});
  search.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence t,int st,int count,int after){}public void afterTextChanged(android.text.Editable e){}public void onTextChanged(CharSequence t,int st,int before,int count){
   adapter.clear();String q=t.toString();int shown=0;
   for(String item:candidates){if(CatalogSearch.matches(item,aliases.get(item),q)){adapter.add(item);if(++shown>=250)break;}}
   adapter.notifyDataSetChanged();
   hint.setText(shown==0?"검색 결과 없음 · 직접 입력 버튼으로 계속할 수 있어요.":"검색 결과 "+shown+(shown>=250?"개 이상":"개")+" · 원하는 모델을 탭하세요.");
  }});
  dialog.show();
 }
 void applyCatalogYear(String brand,String model,int year,Map<String,EditText> fields,ImageView photo,TextView status){
  catalogBrand=brand;catalogModel=model;
  JSONObject entry=MotorcycleCatalog.lookup(this,brand,model);
  String show=MotorcycleCatalog.displayName(brand,entry==null?model:entry.optString("model"));
  EditText name=fields.get(activeForm.equals("vehicles")?"name":"model");EditText y=fields.get("year");
  if(name!=null)name.setText(show);if(y!=null)y.setText(String.valueOf(year));
  hideKeyboard();renderCatalogPhoto(photo,status,brand,model,year);
 }
 void pickCatalogYear(String brand,String model,Map<String,EditText> fields,ImageView photo,TextView status){
  int now=LocalDate.now().getYear();
  EditText yearField=fields.get("year");
  int selectedYear=CatalogYearChoices.preferred(yearField==null?null:yearField.getText().toString(),now);
  String[] years=CatalogYearChoices.labels(now);
  int checked=CatalogYearChoices.selectedIndex(selectedYear,now);
  AlertDialog dialog=new AlertDialog.Builder(this).setTitle("연식 선택 · "+model)
   .setSingleChoiceItems(years,checked,(dlg,pos)->{
    dlg.dismiss();applyCatalogYear(brand,model,CatalogYearChoices.yearAt(pos,now),fields,photo,status);
   }).setNeutralButton("연식 직접 입력",null).setNegativeButton("취소",null).create();
  dialog.setOnShowListener(ignored->{
   ListView list=dialog.getListView();if(list!=null)list.setSelection(Math.max(0,checked-2));
   dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v->{
    EditText input=new EditText(this);input.setSingleLine(true);
    input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
    input.setText(String.valueOf(selectedYear));input.selectAll();
    LinearLayout container=col();container.setPadding(dp(24),dp(4),dp(24),dp(2));container.addView(input);
    AlertDialog manual=new AlertDialog.Builder(this).setTitle("연식 직접 입력")
     .setMessage("차량 등록증 기준으로 입력해 주세요. (1900~"+CatalogYearChoices.lastYear(now)+")")
     .setView(container).setNegativeButton("취소",null).setPositiveButton("적용",null).create();
    manual.setOnShowListener(unused->manual.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(button->{
     String typed=input.getText().toString().trim();int year;
     try{year=Integer.parseInt(typed);}catch(NumberFormatException e){input.setError("연도 4자리를 입력해 주세요.");return;}
     if(!CatalogYearChoices.valid(year,now)){input.setError("1900~"+CatalogYearChoices.lastYear(now)+"년만 선택할 수 있어요.");return;}
     manual.dismiss();dialog.dismiss();applyCatalogYear(brand,model,year,fields,photo,status);
    }));
    manual.show();
   });
  });
  dialog.show();
 }
 void pickCatalogModel(String brand,Map<String,EditText> fields,ImageView photo,TextView status){
  List<String> models=MotorcycleCatalog.models(this,brand);
  // Do not offer a fake model literally named "직접 입력" when a brand has no offline entries.
  final List<String> local=models;
  searchChoices(brand+" · 모델 선택",local,MotorcycleCatalog.modelAliases(this,brand),model->pickCatalogYear(brand,model,fields,photo,status),()->{
   int year;try{year=Integer.parseInt(fields.get("year").getText().toString().trim());}catch(Exception e){year=LocalDate.now().getYear();}
   final int chosenYear=year;toast(chosenYear+"년 온라인 목록 확인 중…");
   new Thread(()->{try{List<String> remote=MotorcycleCatalog.lookupYearOnline(brand,chosenYear);runOnUiThread(()->{
     if(remote.isEmpty()){toast("해당 연식의 온라인 모델이 없어요. 오프라인 목록이나 직접 입력을 사용해 주세요.");return;}
     // Remote model names are from the US regulatory dataset, not domestic trims.
     searchChoices("온라인 모델 · "+chosenYear+"년 (미국 자료)",remote,model->pickCatalogYear(brand,model,fields,photo,status),null);
   });}catch(Exception e){runOnUiThread(()->toast("온라인 조회 실패 · 오프라인 목록을 사용해 주세요."));}},"moto-vpic-lookup").start();
  });
 }
 void showBikeCatalog(Map<String,EditText> fields,ImageView photo,TextView status){
  searchChoices("브랜드 선택",MotorcycleCatalog.brands(this),brand->pickCatalogModel(brand,fields,photo,status),()->{
   toast("온라인 브랜드 목록 불러오는 중…");
   new Thread(()->{try{List<String> brands=MotorcycleCatalog.brandsOnline();runOnUiThread(()->{
    if(brands.isEmpty()){toast("온라인 목록이 비어 있어요.");return;}
    searchChoices("온라인 브랜드 (미국 신고 자료)",brands,brand->pickCatalogModel(brand,fields,photo,status),null);
   });}catch(Exception e){runOnUiThread(()->toast("네트워크 오류 · 저장된 브랜드 목록을 사용해 주세요."));}},"moto-vpic-makes").start();
  });
 }
 void renderCatalogPhoto(ImageView photo,TextView label,String brand,String model,int year){
  photo.setImageResource(BikeVisual.superCub(model)?R.drawable.garage_super_cub:R.drawable.garage_model_placeholder);
  label.setOnClickListener(null);label.setText(BikeVisual.superCub(model)?"슈퍼커브 참고 일러스트 · 실제 연식 사진 검색 중":"실제 연식에 맞는 사진 검색 중…");
  MotorcyclePhoto.show(this,photo,label,brand,model,year,null);
 }
 /** Guided, atomic entry of several services performed during the same visit. */
 void batchServiceEntry(){
  if(readOnly){error("기록 원본 보호 중입니다. 백업을 복원한 뒤 저장하세요.");return;}
  JSONObject selectedBike=bike();if(selectedBike==null){error("먼저 바이크를 등록해 주세요.");return;}
  final String bikeId=selectedBike.optString("id");
  LinearLayout p=page("정비 묶음 기록");
  label(p,bikeName(bikeId)+" · 여러 정비를 한 번에 등록",18,fg,true);
  label(p,"항목마다 비용을 입력해도 저장은 한 번만 진행돼요. 날짜·주행거리는 선택한 모든 항목에 동일하게 적용됩니다.",13,muted,false);
  final EditText date=costField(p,"정비일 (YYYY-MM-DD)",today());
  date.setInputType(android.text.InputType.TYPE_CLASS_DATETIME|android.text.InputType.TYPE_DATETIME_VARIATION_DATE);
  final EditText mileage=costField(p,"당시 계기판 (km)",String.valueOf(selectedBike.optDouble("odo",0)));
  mileage.setInputType(android.text.InputType.TYPE_CLASS_NUMBER|android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
  label(p,"작업한 항목 선택 · 비용 직접 입력",16,fg,true);
  final List<CheckBox> choices=new ArrayList<>();final List<EditText> costs=new ArrayList<>();
  List<JSONObject> prior=items("services");
  for(String type:ServiceBatch.COMMON){
   LinearLayout row=col();row.setPadding(dp(12),dp(8),dp(12),dp(8));row.setBackground(shape(panel,line,13));
   CheckBox select=new CheckBox(this);select.setText(type);select.setTextColor(fg);select.setTextSize(16);select.setMinHeight(dp(48));row.addView(select);
   EditText input=new EditText(this);input.setSingleLine(true);input.setTextColor(fg);input.setTextSize(16);input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
   input.setContentDescription(type+" 정비 비용");input.setHint("비용 (원) · 직접 작업은 0");input.setText("0");input.setSelectAllOnFocus(true);input.setVisibility(View.GONE);row.addView(input);
   JSONObject last=QuickServiceHistory.latest(prior,bikeId,type);
   if(last!=null){Button previous=button("지난 "+type+" 비용 불러오기",false,()->{
    select.setChecked(true);input.setText(String.valueOf((long)last.optDouble("cost",0)));
   });previous.setVisibility(View.GONE);row.addView(previous);
    select.setOnCheckedChangeListener((v,on)->{input.setVisibility(on?View.VISIBLE:View.GONE);previous.setVisibility(on?View.VISIBLE:View.GONE);});
   }else select.setOnCheckedChangeListener((v,on)->input.setVisibility(on?View.VISIBLE:View.GONE));
   LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,-2);rp.bottomMargin=dp(9);p.addView(row,rp);
   choices.add(select);costs.add(input);
  }
  CheckBox intervals=new CheckBox(this);intervals.setText("이전 동일 항목의 정비 간격 재사용 (선택)");intervals.setTextColor(fg);intervals.setTextSize(14);intervals.setMinHeight(dp(52));p.addView(intervals);
  label(p,"정비 간격은 같은 바이크·같은 항목의 기록으로만 계산하며 제조사 공식 주기가 아닙니다.",12,muted,false);
  final TextView batchLive=text("선택한 정비 0개 · 합계 0원",17,fg,true);
  batchLive.setPadding(dp(12),dp(10),dp(12),dp(10));batchLive.setBackground(shape(panel,line,13));p.addView(batchLive);
  Runnable refreshBatchTotal=()->{
   int selectedCount=0;long total=0;boolean invalid=false;
   for(int i=0;i<choices.size();i++)if(choices.get(i).isChecked()){
    selectedCount++;
    String raw=costs.get(i).getText().toString().trim();
    try{long amount=Long.parseLong(raw);if(amount<0||amount>1_000_000_000||total>1_000_000_000L-amount)invalid=true;else total+=amount;}
    catch(Exception e){invalid=true;}
   }
   batchLive.setText("선택한 정비 "+selectedCount+"개 · "+(invalid?"금액 확인 필요":"예상 합계 "+won(total)));
  };
  // Batch draft is a separate, bike-scoped local preference. No partial writes to the real records.
  String batchKey="batch_"+bikeId;
  android.content.SharedPreferences batchPrefs=getSharedPreferences("service_batch_drafts",MODE_PRIVATE);
  final boolean[] loading={true};
  Runnable saveBatchDraft=()->{
   if(loading[0])return;
   try{
    JSONObject data=new JSONObject();data.put("vehicleId",bikeId);data.put("date",date.getText().toString());
    data.put("mileage",mileage.getText().toString());data.put("reuseIntervals",intervals.isChecked());
    JSONArray chosen=new JSONArray(),amounts=new JSONArray();
    for(int i=0;i<choices.size();i++){chosen.put(choices.get(i).isChecked());amounts.put(costs.get(i).getText().toString());}
    data.put("selected",chosen);data.put("costs",amounts);data.put("time",System.currentTimeMillis());
    batchPrefs.edit().putString(batchKey,data.toString()).apply();
    refreshBatchTotal.run();
   }catch(Exception ignored){toast("임시저장 실패 · 입력을 확인해 주세요.");}
  };
  for(int i=0;i<choices.size();i++){
   choices.get(i).setOnCheckedChangeListener((v,on)->{
    int pos=choices.indexOf(v);if(pos<0)return;
    costs.get(pos).setVisibility(on?View.VISIBLE:View.GONE);
    // Keep an existing per-item 'previous cost' shortcut visible when selected.
    LinearLayout parent=(LinearLayout)v.getParent();
    if(parent.getChildCount()>2)parent.getChildAt(2).setVisibility(on?View.VISIBLE:View.GONE);
    saveBatchDraft.run();
   });
   costs.get(i).addTextChangedListener(new android.text.TextWatcher(){
    public void beforeTextChanged(CharSequence s,int start,int count,int after){}
    public void onTextChanged(CharSequence s,int start,int before,int count){saveBatchDraft.run();}
    public void afterTextChanged(android.text.Editable e){}
   });
  }
  for(EditText input:new EditText[]{date,mileage})input.addTextChangedListener(new android.text.TextWatcher(){
   public void beforeTextChanged(CharSequence s,int start,int count,int after){}
   public void onTextChanged(CharSequence s,int start,int before,int count){saveBatchDraft.run();}
   public void afterTextChanged(android.text.Editable e){}
  });
  intervals.setOnCheckedChangeListener((v,on)->saveBatchDraft.run());
  loading[0]=false;refreshBatchTotal.run();
  action(p,"엔진오일 + 오일필터 빠른 선택",false,()->{
   choices.get(0).setChecked(true);choices.get(1).setChecked(true);
   saveBatchDraft.run();toast("두 항목을 선택했어요. 비용을 확인한 뒤 저장하세요.");
  });
  action(p,"정비 항목 선택 모두 해제",false,()->{
   for(CheckBox selectedItem:choices)selectedItem.setChecked(false);
   saveBatchDraft.run();
  });
  List<ServiceBatch.Item> recentBatch=ServiceBatchPreset.latest(prior,bikeId);
  if(!recentBatch.isEmpty())action(p,"지난 묶음 정비 "+recentBatch.size()+"항목 다시 선택",false,()->{
   loading[0]=true;
   try{
    for(int i=0;i<choices.size();i++){
     ServiceBatch.Item matched=null;
     for(ServiceBatch.Item item:recentBatch)if(ServiceBatch.COMMON[i].equals(item.type)){matched=item;break;}
     choices.get(i).setChecked(matched!=null);
     if(matched!=null)costs.get(i).setText(matched.cost);
    }
   }finally{loading[0]=false;}
   saveBatchDraft.run();toast("지난 묶음의 항목·비용만 불러왔어요. 오늘 날짜와 계기판은 유지합니다.");
  });
  String savedBatch=batchPrefs.getString(batchKey,"");
  if(!savedBatch.isEmpty()){
   action(p,"이전에 작성하던 묶음 정비 불러오기",false,()->{
    try{
     JSONObject draft=new JSONObject(savedBatch);
     if(!BatchDraftPolicy.recoverable(bikeId,draft.optString("vehicleId"),draft.optLong("time",0),System.currentTimeMillis()))
      throw new Exception("다른 바이크 또는 오래된 임시 기록입니다.");
     loading[0]=true;
     try{
      date.setText(draft.optString("date",today()));mileage.setText(draft.optString("mileage","0"));
      intervals.setChecked(draft.optBoolean("reuseIntervals"));
      JSONArray chosen=draft.optJSONArray("selected"),values=draft.optJSONArray("costs");
      for(int i=0;i<choices.size();i++){
       choices.get(i).setChecked(chosen!=null&&chosen.optBoolean(i));
       costs.get(i).setText(values==null?"0":values.optString(i,"0"));
      }
     }finally{loading[0]=false;}
     saveBatchDraft.run();toast("작성 중이던 정비를 불러왔어요. 저장 전 금액을 확인해 주세요.");
    }catch(Exception e){error("임시 기록을 불러오지 못했어요: "+e.getMessage());}
   });
   action(p,"묶음 정비 임시 기록 삭제",false,()->new AlertDialog.Builder(this).setTitle("임시 기록을 삭제할까요?")
    .setMessage("저장된 정비 내역은 그대로 유지됩니다.")
    .setNegativeButton("취소",null).setPositiveButton("임시 기록 삭제",(d,w)->{
     batchPrefs.edit().remove(batchKey).apply();toast("임시 기록만 지웠어요.");
    }).show());
  }
  action(p,"선택 항목 임시저장",false,()->{saveBatchDraft.run();toast("이 바이크의 묶음 정비를 임시저장했어요.");});
  action(p,"선택 항목 한 번에 저장",true,()->attempt(()->{
   List<ServiceBatch.Item> rows=new ArrayList<>();
   for(int i=0;i<choices.size();i++)if(choices.get(i).isChecked())rows.add(new ServiceBatch.Item(ServiceBatch.COMMON[i],costs.get(i).getText().toString(),""));
   ServiceBatch.Result result=ServiceBatch.build(bikeId,date.getText().toString().trim(),mileage.getText().toString().trim(),rows,items("services"),intervals.isChecked());
   // Always snapshot current storage again before confirming; one write commits all records.
   int duplicated=ServiceBatch.duplicates(items("services"),result.records);
   StringBuilder detail=new StringBuilder();for(JSONObject row:result.records){
    detail.append("\n · ").append(row.optString("type")).append("  ").append(won(row.optDouble("cost")));
   }
   String message=result.records.size()+"개 항목 · 총 "+won(result.totalCost)+detail+"\n"+
    date.getText().toString()+" · "+mileage.getText().toString()+" km"+
    (duplicated>0?"\n\n⚠ 기존 기록과 유사한 항목 "+duplicated+"개가 있어요.":"")+
    "\n\n모든 정비 기록을 한 번에 저장할까요?";
   new AlertDialog.Builder(this).setTitle("정비 내용 확인").setMessage(message)
    .setNegativeButton("계속 수정",null).setPositiveButton("모두 저장",(d,w)->attempt(()->{
     if(!bikeId.equals(bike()==null?"":bike().optString("id")))throw new IOException("선택 바이크가 변경됐습니다. 다시 확인하세요.");
     JSONObject next=new JSONObject(db.toString());
     if(Store.find(next,"vehicles",bikeId)==null)throw new IOException("바이크가 삭제됐습니다.");
     int nowDuplicates=ServiceBatch.duplicates(items("services"),result.records);
     if(nowDuplicates>duplicated)throw new IOException("저장 중 유사한 기록이 추가됐습니다. 다시 확인하세요.");
     for(JSONObject entry:result.records)Store.upsert(next,"services",entry);
     JSONObject updated=Store.find(next,"vehicles",bikeId);
     double km=Double.parseDouble(mileage.getText().toString().trim());
     if(km>updated.optDouble("odo",0))updated.put("odo",km);
     write(next);batchPrefs.edit().remove(batchKey).apply();DailyMaintenanceJob.checkSoon(this);close();render();toast("정비 "+result.records.size()+"건을 저장했어요.");
    })).show();
  }));
  show();
 }

 void edit(String key,JSONObject existing){if(readOnly){error("원본 보호 중입니다. 백업을 먼저 복원해 주세요.");return;}JSONObject b=bike();if((key.equals("services")||key.equals("expenses"))&&b==null){error("바이크를 먼저 등록해 주세요.");return;}JSONObject old=existing==null?new JSONObject():existing;String title=switch(key){case"vehicles"->"바이크";case"listings"->"매물";case"services"->"정비";case"expenses"->"주유·비용";case"routes"->"코스";default->"보호장구";};LinearLayout p=page(title+(existing==null?" 추가":" 수정"));activeForm=key;activeOld=old;catalogBrand=old.optString("catalogBrand","");catalogModel=old.optString("catalogModel","");Map<String,EditText> f=new LinkedHashMap<>();Map<String,Spinner> sel=new HashMap<>();
  String[][] fields;
  if(key.equals("vehicles"))fields=new String[][]{{"name","바이크 이름 *",""},{"odo","현재 주행거리 (km) *",bike()==null?"":"0"},{"year","연식 *",""},{"purchase","구매가 (원)","0"},{"purchaseDate","구매일 (YYYY-MM-DD)",""},{"soldDate","판매일 (선택)",""},{"insuranceDate","보험 만료일 (YYYY-MM-DD)",""},{"note","메모",""}};
  else if(key.equals("listings"))fields=new String[][]{{"model","모델명 *",""},{"price","가격 (원) *",""},{"url","매물 URL (선택)",""},{"year","연식 *",""},{"mileage","주행거리 (km)","0"},{"region","지역",""},{"notes","튜닝·매물 메모",""}};
  else if(key.equals("services"))fields=new String[][]{{"type","정비 항목 *","엔진오일"},{"date","날짜 (YYYY-MM-DD) *",today()},{"mileage","당시 주행거리 (km)",String.valueOf(b.optDouble("odo"))},{"cost","금액 (원) *",""},{"nextKm","다음 정비 거리 (km, 0=미설정)","0"},{"nextDate","다음 정비일 (선택)",""},{"note","메모",""}};
  else if(key.equals("expenses"))fields=new String[][]{{"date","날짜 (YYYY-MM-DD) *",today()},{"mileage","당시 주행거리 (km)",String.valueOf(b.optDouble("odo"))},{"amount","금액 (원) *",""},{"liters","주유량 (L, 주유 외에는 0)","0"},{"note","메모",""}};
  else if(key.equals("routes"))fields=new String[][]{{"name","코스 이름 *",""},{"start","출발지",""},{"end","목적지",""},{"distance","거리 (km, 수동)","0"},{"date","날짜",today()},{"notes","경유지·메모",""}};
  else fields=new String[][]{{"name","장구 이름 *",""},{"date","구매일",today()},{"note","인증·교체 예정·메모",""}};
  ImageView catalogPreview=null;TextView catalogStatus=null;
  if(key.equals("vehicles")||key.equals("listings")){
   label(p,"브랜드 → 모델 → 연식  ·  선택해서 자동 입력",16,fg,true);
   ImageView preview=new ImageView(this);preview.setBackground(shape(panel,line,16));preview.setScaleType(ImageView.ScaleType.CENTER_CROP);preview.setImageResource(R.drawable.garage_model_placeholder);
   LinearLayout.LayoutParams pl=new LinearLayout.LayoutParams(-1,dp(155));pl.bottomMargin=dp(6);p.addView(preview,pl);
   TextView status=text("연식·모델 사진은 확인된 후보만 표시 · 직접 사진은 차고에서 변경",12,muted,false);p.addView(status);
   catalogPreview=preview;catalogStatus=status;
   Button catalog=button("⌕  브랜드 / 모델 / 연식 선택",true,()->showBikeCatalog(f,preview,status));
   LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-1,dp(54));bp.topMargin=dp(10);bp.bottomMargin=dp(18);p.addView(catalog,bp);
  }
  if(key.equals("expenses")||key.equals("services"))label(p,"차량: "+bikeName(old.optString("vehicleId",b.optString("id"))),16,fg,true);
  // Put the one-tap shortcut above the long form, not below the optional fields.
  LinearLayout recentShortcut=col();
  boolean fastEntry=existing==null&&(key.equals("expenses")||key.equals("services"));
  LinearLayout dateMileageFields=col();
  TextView quickMetaSummary=null;Button metaToggle=null;
  if(fastEntry){
   p.addView(recentShortcut);
   label(p,"기본 기록값 · 필요할 때만 변경",14,fg,true);
   TextView summary=text("오늘 "+today()+" · 계기판 "+num(b.optDouble("odo"))+" km",13,muted,false);
   quickMetaSummary=summary;ocrDateMileageFields=dateMileageFields;ocrMetaSummary=summary;p.addView(summary);
   Button expand=button("날짜·계기판 수정  ▾",false,()->{
    boolean open=dateMileageFields.getVisibility()!=View.VISIBLE;
    dateMileageFields.setVisibility(open?View.VISIBLE:View.GONE);
   });metaToggle=expand;p.addView(expand);dateMileageFields.setVisibility(View.GONE);p.addView(dateMileageFields);
  }
  if(key.equals("expenses"))selector(p,sel,"type","비용 종류",new String[]{"주유","보험","부품","기타"},old.optString("type","주유"));
  LinearLayout optional=col();optional.setVisibility(View.GONE);formOptionalSection=optional;formOptionalToggle=null;formOptionalKeys.clear();
  Set<String> essentials=new HashSet<>(Arrays.asList("name","model","odo","year","price","url","type","date","mileage","amount","cost","liters","start","end"));
  LinearLayout litersContainer=null;
  for(String[] x:fields){
   if(key.equals("expenses")&&x[0].equals("liters")){litersContainer=col();p.addView(litersContainer);field(litersContainer,f,old,x[0],x[1],x[2]);}
   else if(fastEntry&&(x[0].equals("date")||x[0].equals("mileage"))){field(dateMileageFields,f,old,x[0],x[1],x[2]);}
   else {if(!essentials.contains(x[0]))formOptionalKeys.add(x[0]);field(essentials.contains(x[0])?p:optional,f,old,x[0],x[1],x[2]);}
  }
  if(catalogPreview!=null&&old.optInt("year")>0){
   JSONObject found=MotorcycleCatalog.search(this,old.optString(key.equals("vehicles")?"name":"model"));
   String brand=catalogBrand.isEmpty()&&found!=null?found.optString("brand"):catalogBrand;
   String model=catalogModel.isEmpty()&&found!=null?found.optString("model"):catalogModel;
   if(!brand.isEmpty()&&!model.isEmpty())renderCatalogPhoto(catalogPreview,catalogStatus,brand,model,old.optInt("year"));
  }
  if(key.equals("expenses")||key.equals("services")){
   action(p,"▣ 영수증 사진에서 금액·날짜 읽기 (확인 후 적용)",false,()->{
    ocrFields=f;ocrKind=key;
    Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT);intent.setType("image/*");
    intent.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(intent,54);
   });
   action(p,"카메라로 영수증 바로 촬영 · 자동입력 (확인 필수)",false,()->{
    ocrFields=f;ocrKind=key;startReceiptCamera(true);
   });
   label(p,"기기 안에서 글자를 인식합니다. 결과가 틀릴 수 있으므로 원본 영수증과 금액을 대조한 뒤 저장하세요.",12,muted,false);
  }
  final LinearLayout fuelInputs=litersContainer;
  if(fuelInputs!=null){Spinner kind=sel.get("type");fuelInputs.setVisibility(kind.getSelectedItem().toString().equals("주유")?View.VISIBLE:View.GONE);}

  if(optional.getChildCount()>0){
   boolean revealSaved=existing!=null&&hasPopulatedOptionalFields(f);
   optional.setVisibility(revealSaved?View.VISIBLE:View.GONE);
   Button expand=button(revealSaved?"선택 입력 접기  ▴":"선택 입력 보기  ▾",false,()->{
    boolean opening=optional.getVisibility()!=View.VISIBLE;
    optional.setVisibility(opening?View.VISIBLE:View.GONE);
    updateOptionalButton();
   });
   formOptionalToggle=expand;p.addView(expand);p.addView(optional);gap(p,14);
  }
  if(existing!=null&&(key.equals("services")||key.equals("expenses"))){
   File attached=receiptPhotoFile(old.optString("id"));
   action(p,receiptCount(old.optString("id"))>0?"영수증·작업 사진 "+receiptCount(old.optString("id"))+"장 관리":"＋ 영수증·작업 사진 첨부",false,()->receiptOptions(key,old));
   label(p,"사진은 기기 내부에 보관되고 전체 ZIP/암호화 백업에 포함돼요. 새 기록은 저장한 뒤 첨부할 수 있습니다.",12,muted,false);
  }
  if(key.equals("listings")){selector(p,sel,"status","판매 상태",new String[]{"확인필요","판매중","예약중","판매완료"},old.optString("status","확인필요"));selector(p,sel,"quick","퀵시프터",new String[]{"미확인","있음","없음"},old.optString("quick","미확인"));}
  CheckBox full=new CheckBox(this);LinearLayout fuelNote=col();
  if(key.equals("expenses")){full.setText("가득 주유  ·  연비 계산에 사용");full.setTextSize(16);full.setTextColor(fg);full.setMinHeight(dp(52));full.setChecked(old.optBoolean("fullTank"));fuelNote.addView(full);label(fuelNote,"가득 주유를 두 번 이상 기록하면 연비를 계산해요.",13,muted,false);p.addView(fuelNote);fuelNote.setVisibility("주유".equals(sel.get("type").getSelectedItem().toString())?View.VISIBLE:View.GONE);}
  TextView fuelUnitPrice=null;
  if(key.equals("expenses")&&f.containsKey("amount")&&f.containsKey("liters")){
   TextView unit=text(FuelEntryMath.unitPrice(f.get("amount").getText().toString(),f.get("liters").getText().toString()),13,muted,false);
   unit.setPadding(dp(5),dp(6),0,dp(8));p.addView(unit);
   fuelUnitPrice=unit;
   unit.setVisibility("주유".equals(sel.get("type").getSelectedItem().toString())?View.VISIBLE:View.GONE);
   android.text.TextWatcher watcher=new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int af){}
    public void onTextChanged(CharSequence s,int st,int before,int count){unit.setText(FuelEntryMath.unitPrice(f.get("amount").getText().toString(),f.get("liters").getText().toString()));}
    public void afterTextChanged(android.text.Editable e){}
   };
   f.get("amount").addTextChangedListener(watcher);f.get("liters").addTextChangedListener(watcher);
  }
  draftKey=key;draftId=old.optString("id");draftVehicle=b==null?"":old.optString("vehicleId",b.optString("id"));draftFields=f;draftSelectors=sel;fullTankBox=key.equals("expenses")?full:null;formDirty=false;
  for(EditText input:f.values())input.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence a,int b,int c,int d){}public void onTextChanged(CharSequence a,int b,int c,int d){formDirty=true;persistDraft();}public void afterTextChanged(android.text.Editable e){}});
  full.setOnCheckedChangeListener((v,value)->{formDirty=true;persistDraft();});
  if(existing==null)recentRecordShortcut(recentShortcut,key,f,sel,b);
  if(fastEntry){
   quickRecordTools(recentShortcut,key,f,sel,b);
   final TextView summary=quickMetaSummary;
   android.text.TextWatcher updateSummary=new android.text.TextWatcher(){
    public void beforeTextChanged(CharSequence t,int start,int count,int after){}
    public void onTextChanged(CharSequence t,int start,int before,int count){
     summary.setText("날짜 "+f.get("date").getText().toString()+" · 계기판 "+f.get("mileage").getText().toString()+" km");
    }
    public void afterTextChanged(android.text.Editable e){}
   };
   f.get("date").addTextChangedListener(updateSummary);f.get("mileage").addTextChangedListener(updateSummary);
  }
  final CheckBox attachAfterSave=new CheckBox(this);
  if(existing==null&&(key.equals("services")||key.equals("expenses"))){
   attachAfterSave.setText("저장한 다음 영수증·작업 사진 첨부");
   attachAfterSave.setTextColor(fg);attachAfterSave.setTextSize(15);attachAfterSave.setMinHeight(dp(48));
   attachAfterSave.setContentDescription("저장 후 사진 첨부 방법 선택");p.addView(attachAfterSave);
  }
  final TextView fuelUnitLabel=fuelUnitPrice;
  for(Spinner spinner:sel.values())spinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){boolean initial=true;public void onItemSelected(android.widget.AdapterView<?> parent,View view,int pos,long id){if(fuelInputs!=null&&spinner==sel.get("type")){boolean isFuel="주유".equals(spinner.getSelectedItem().toString());fuelInputs.setVisibility(isFuel?View.VISIBLE:View.GONE);fuelNote.setVisibility(isFuel?View.VISIBLE:View.GONE);if(fuelUnitLabel!=null)fuelUnitLabel.setVisibility(isFuel?View.VISIBLE:View.GONE);}if(initial){initial=false;return;}formDirty=true;persistDraft();}public void onNothingSelected(android.widget.AdapterView<?> p){}});
  action(p,"저장",true,()->attempt(()->{JSONObject item=new JSONObject(old.toString());item.put("id",old.optString("id",Store.id()));for(Map.Entry<String,EditText> entry:f.entrySet()){String k=entry.getKey(),s=entry.getValue().getText().toString().trim();if(numbers.contains(k)){if((k.equals("odo")||k.equals("price")||k.equals("cost")||k.equals("amount"))&&s.isEmpty())throw new Exception("필수 숫자 항목을 입력해 주세요.");double n=s.isEmpty()?0:Double.parseDouble(s);if(!Double.isFinite(n)||n<0||n>1e13)throw new Exception("숫자 범위를 확인해 주세요.");if(k.equals("year")&&(n<1900||n>LocalDate.now().getYear()+2||n%1!=0))throw new Exception("연식을 확인해 주세요.");item.put(k,n);}else item.put(k,s);}for(String k:sel.keySet())item.put(k,sel.get(k).getSelectedItem().toString());
   String required=switch(key){case"vehicles","routes","gear"->"name";case"listings"->"model";default->"type";};if(item.optString(required).isEmpty())throw new Exception("이름 또는 항목을 입력해 주세요.");for(String d:new String[]{"date","purchaseDate","soldDate","insuranceDate","nextDate"})Store.validateDate(item.optString(d));if((key.equals("expenses")||key.equals("services"))&&item.optString("date").isEmpty())throw new Exception("날짜를 입력해 주세요.");if(key.equals("vehicles"))item.put("isDemo",false);
   if(key.equals("vehicles")||key.equals("listings")){
    String typed=item.optString(key.equals("vehicles")?"name":"model");JSONObject catalog=MotorcycleCatalog.lookup(this,catalogBrand,catalogModel);
    String selectedName=MotorcycleCatalog.displayName(catalogBrand,catalog==null?catalogModel:catalog.optString("model"));
    if(!catalogBrand.isEmpty()&&(MotorcycleCatalog.norm(typed).equals(MotorcycleCatalog.norm(selectedName))||MotorcycleCatalog.norm(typed).equals(MotorcycleCatalog.norm(catalogModel)))){
     item.put("catalogBrand",catalogBrand);item.put("catalogModel",catalogModel);item.put("catalogPhotoStatus","year-search-candidate");
    }else{
     JSONObject inferred=MotorcycleCatalog.search(this,typed);
     if(inferred!=null){item.put("catalogBrand",inferred.optString("brand"));item.put("catalogModel",inferred.optString("model"));}
     else{item.remove("catalogBrand");item.remove("catalogModel");item.remove("catalogPhotoStatus");}
    }
   }
   if(key.equals("listings")){if(item.optDouble("price")<=0)throw new Exception("가격은 0원보다 커야 합니다.");String u=item.optString("url");if(!u.isEmpty()&&!Store.safeUrl(u))throw new Exception("http(s) 매물 주소를 입력해 주세요.");for(JSONObject x:items("listings"))if(!u.isEmpty()&&!x.optString("id").equals(item.optString("id"))&&Store.canonicalUrl(u).equals(Store.canonicalUrl(x.optString("url"))))throw new Exception("이미 저장한 매물 주소입니다.");JSONArray hist=item.optJSONArray("priceHistory");if(hist==null)hist=new JSONArray();if(hist.length()==0||old.optDouble("price")!=item.optDouble("price")){JSONObject h=new JSONObject();h.put("price",item.optDouble("price"));h.put("date",Instant.now().toString());hist.put(h);}item.put("priceHistory",hist);if(!item.has("created"))item.put("created",Instant.now().toString());}
   if(key.equals("expenses")){if(!"주유".equals(item.optString("type")))item.put("liters",0);
    else FuelRecordValidation.requireFuel(item.optDouble("amount"),item.optDouble("liters"));
    boolean isFull=full.isChecked()&&"주유".equals(item.optString("type"));if(isFull&&item.optDouble("liters")<=0)throw new Exception("가득 주유의 주유량을 입력해 주세요.");item.put("fullTank",isFull);}
   if(key.equals("services")||key.equals("expenses"))item.put("vehicleId",old.optString("vehicleId",b.optString("id")));JSONObject next=new JSONObject(db.toString());Store.upsert(next,key,item);if(key.equals("services")||key.equals("expenses")){JSONObject v=Store.find(next,"vehicles",item.getString("vehicleId"));if(v!=null&&item.optDouble("mileage")>v.optDouble("odo"))v.put("odo",item.optDouble("mileage"));}if(existing==null&&QuickLogDuplicate.exists(items(key),item,key)){
    new AlertDialog.Builder(this).setTitle("중복으로 보이는 기록")
     .setMessage("이 바이크에 같은 날짜·거리·항목·금액의 기록이 이미 있어요. 두 번 저장할까요?")
     .setNegativeButton("취소",null).setPositiveButton("그래도 저장",(d,which)->attempt(()->{
      write(next);if(key.equals("vehicles")||key.equals("services")||key.equals("expenses"))DailyMaintenanceJob.checkSoon(this);clearDraft();close();render();toast("중복 확인 후 저장했어요.");
      if(attachAfterSave.isChecked())offerReceiptForSavedRecord(key,item);
     })).show();return;
   }write(next);if(key.equals("vehicles")||key.equals("services")||key.equals("expenses"))DailyMaintenanceJob.checkSoon(this);clearDraft();close();render();toast("저장했어요.");
   if(attachAfterSave.isChecked())offerReceiptForSavedRecord(key,item);}));if(existing!=null&&!key.equals("vehicles"))action(p,"기록 삭제",false,()->{close();remove(key,old);});show();
 }
 void offerReceiptForSavedRecord(String key,JSONObject item){
  if(!"services".equals(key)&&!"expenses".equals(key))return;
  try{if(Store.find(db,key,item.optString("id"))==null)return;}
  catch(Exception e){return;} // Saved record was removed or data is unavailable.
  new AlertDialog.Builder(this).setTitle("영수증·작업 사진 추가")
   .setMessage("기록은 이미 저장했어요. 영수증 사진을 추가할까요? (최대 4장)")
   .setItems(new String[]{"카메라로 촬영","갤러리에서 선택"},(d,which)->{
    if(which==0)captureReceiptPhoto(key,item,1);else chooseReceiptPhoto(key,item,1);
   }).setNegativeButton("나중에",null).show();
 }
 void remove(String key,JSONObject item){new AlertDialog.Builder(this).setTitle("기록을 삭제할까요?").setMessage(key.equals("vehicles")?"이 바이크의 정비·비용 기록도 함께 삭제됩니다. 필요한 기록은 먼저 백업해 주세요.":"삭제 후 되돌릴 수 없습니다.").setNegativeButton("취소",null).setPositiveButton("삭제",(d,w)->attempt(()->{JSONObject next=new JSONObject(db.toString());String id=item.getString("id");List<String> childReceipts=new ArrayList<>();
  if(key.equals("vehicles"))for(String collection:new String[]{"services","expenses"})for(JSONObject oldEntry:items(collection))if(id.equals(oldEntry.optString("vehicleId")))childReceipts.add(oldEntry.optString("id"));
  Store.remove(next,key,id);if(key.equals("vehicles")){for(String k:new String[]{"services","expenses"}){JSONArray a=next.getJSONArray(k);for(int i=a.length()-1;i>=0;i--)if(a.getJSONObject(i).optString("vehicleId").equals(id))a.remove(i);}if(next.getJSONObject("settings").optString("primary").equals(id))next.getJSONObject("settings").remove("primary");selected="";getSharedPreferences("garage_ui",MODE_PRIVATE).edit().remove("selected_bike").apply();}write(next);if(key.equals("vehicles"))for(String childId:childReceipts)clearReceiptFiles(childId);if(key.equals("services")||key.equals("expenses")){clearReceiptFiles(id);}render();})).show();}
 boolean restoreIsBlocked(){return RestoreRideGate.blocked(RideService.running,RideService.autoArmed);}
 void explainRestoreBlocked(){error(RestoreRideGate.MESSAGE);}
 void importBundle(){
  if(restoreIsBlocked()){explainRestoreBlocked();return;}
  Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("application/zip");i.addCategory(Intent.CATEGORY_OPENABLE);
  i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"application/zip","application/x-zip-compressed","application/octet-stream"});
  startActivityForResult(i,46);
 }
 void prepareBundleRestore(Uri uri){prepareBundleRestore(uri,null);}
 void prepareBundleRestore(Uri uri,char[] password){
  if(restoreIsBlocked()){if(password!=null)Arrays.fill(password,'\0');explainRestoreBlocked();return;}
  new Thread(()->{
   try {
    RestoreArchive.Prepared ready;
    File authenticated=null;
    try{
     if(password==null){
      try(InputStream in=getContentResolver().openInputStream(uri)){
       if(in==null)throw new IOException("ZIP을 열지 못했어요.");ready=RestoreArchive.prepare(in,getFilesDir());
      }
     }else{
      // Verify the entire authenticated stream before parsing or offering any restore.
      try(InputStream encrypted=getContentResolver().openInputStream(uri)){
       if(encrypted==null)throw new IOException("암호화 백업 파일을 열지 못했어요.");
       authenticated=BackupCrypto.decryptToPrivateFile(encrypted,password,getCacheDir());
      }
      try(InputStream in=new BufferedInputStream(new FileInputStream(authenticated))){
       BackupIntegrity.verify(in);
      }
      try(InputStream in=new BufferedInputStream(new FileInputStream(authenticated))){
       ready=RestoreArchive.prepare(in,getFilesDir());
      }
     }
    }finally{
     if(authenticated!=null)java.nio.file.Files.deleteIfExists(authenticated.toPath());
     if(password!=null)Arrays.fill(password,'\0');
    }
    JSONObject verified;
    try{verified=Store.parse(ready.json);}catch(Exception e){ready.close();throw e;}
    runOnUiThread(()->{
     if(isFinishing()||isDestroyed()){ready.close();return;}
     String spaceNote;
     try {
      long need=ready.requiredSpace(getFilesDir()),free=RestoreCapacity.available(getFilesDir());
      spaceNote="\n\n복원 작업 여유 공간: 약 "+RestoreCapacity.mb(need)+"MB 필요 · 현재 약 "+RestoreCapacity.mb(free)+"MB";
      if(free<need)spaceNote+="\n공간 부족: 기존 파일 보호를 위해 실제 복원을 거부합니다.";
     }catch(IOException e){spaceNote="\n\n저장 공간 점검 불가: 복원 시 안전 검사를 다시 진행합니다.";}
     new AlertDialog.Builder(this).setTitle("전체 백업을 복원할까요?")
      .setMessage(RestorePreview.text(verified,db,ready.rides,ready.photos)+RestorePreflightDetail.text(verified,db,ready.rides,ready.photos,ready.receipts)+spaceNote+"\n\n현재 기록과 사진·GPS를 교체합니다. 중복 병합하지 않습니다. 기존 기록이 필요하면 먼저 전체 ZIP을 내보내세요.\n\n계정 없이 기기별 독립 저장됩니다.")
      .setNegativeButton("취소",(d,w)->ready.close())
      .setOnCancelListener(d->ready.close())
      .setPositiveButton("전체 복원",(d,w)->{
       if(restoreIsBlocked()){ready.close();explainRestoreBlocked();return;}
       new Thread(()->{
        boolean entered=false;
        try {
         if(!RestoreRideGate.begin(RideService.running,RideService.autoArmed))throw new IOException(RestoreRideGate.MESSAGE);
         entered=true;
         byte[] previous=(readOnly?disk.readFully():db.toString(2).getBytes(StandardCharsets.UTF_8));
         // Abort before overwriting even the previous JSON recovery point if
         // there isn't room for the old JSON and both sets of GPS/photos.
         long required=ready.requiredSpace(getFilesDir());
         required=Math.addExact(required,(long)previous.length);
         RestoreCapacity.ensure(RestoreCapacity.available(getFilesDir()),required);
         // Write old JSON rollback BEFORE replacing any user files.
         AtomicFile rollback=new AtomicFile(new File(getFilesDir(),"before-import.json"));atomic(rollback,previous);
         // Do not use write(next) here: it performs post-commit checkpoint work
         // that can throw after the JSON is already persisted. At this point the
         // asset rollback would otherwise undo files but leave NEW JSON on disk.
         ready.install(getFilesDir(),previous,json->{
          JSONObject next=Store.parse(json);
          if(RideService.running||RideService.autoArmed)throw new IOException(RestoreRideGate.MESSAGE);
          atomic(disk,next.toString(2).getBytes(StandardCharsets.UTF_8));
          db=next;readOnly=false; // no fallible work after the JSON commit
         });
         // Non-critical follow-up must never turn a successfully committed restore
         // into an asset-only rollback. The previous JSON remains in before-import.json.
         boolean checkpointOk=true;
         try{LocalSafetyBackup.checkpoint(this,db);}catch(Exception ignored){checkpointOk=false;}
         try{MotoWidgetProvider.refresh(this);}catch(Exception ignored){ /* Restore is already committed. */ }
         final boolean checkpointReady=checkpointOk;
         runOnUiThread(()->{selected="";compare.clear();render();
          toast("복원 완료 · GPS "+ready.rides+"개 · 사진 "+ready.photos+"개"+
           (checkpointReady?"":" · 자동 안전 백업 갱신 실패"));});
        }catch(Exception e){runOnUiThread(()->error("전체 백업 복원 실패: "+e.getMessage()));}
        finally{if(entered)RestoreRideGate.end();ready.close();}
       },"MOTO-ZIP-restore-apply").start();
      }).show();
    });
   }catch(Exception e){runOnUiThread(()->{if(!isFinishing()&&!isDestroyed())error("ZIP 검사 실패: "+e.getMessage());});}
  },"MOTO-ZIP-restore-check").start();
 }
 void exportBundle(){
  if(!BackupExportGate.canExport(RideService.running,RideService.autoArmed,RestoreRideGate.isRestoring())){error(BackupExportGate.MESSAGE);return;}
  new AlertDialog.Builder(this).setTitle("모든 기록 ZIP 내보내기")
   .setMessage("바이크·정비·매물·GPS 경로·직접 찍은 사진을 한 파일로 저장해요. 이 ZIP은 암호화되지 않습니다. 공유에 주의하세요.\n\n위치정보가 민감하면 '암호화 전체 백업'을 사용하세요.")
   .setNegativeButton("취소",null).setPositiveButton("내보내기",(d,w)->launchBackupDocument(null)).show();
 }
 void exportSplitBundle(String part){
  if(!BackupExportGate.canExport(RideService.running,RideService.autoArmed,RestoreRideGate.isRestoring())){
   error(BackupExportGate.MESSAGE);return;
  }
  if(!"GPS".equals(part)&&!"MEDIA".equals(part))return;
  attempt(()->{
   pendingBundleJson=readOnly?new String(disk.readFully(),StandardCharsets.UTF_8):db.toString(2);
   Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT);
   intent.setType("application/zip");intent.addCategory(Intent.CATEGORY_OPENABLE);
   intent.putExtra(Intent.EXTRA_TITLE,"MOTO-HUB-"+part+"-PART-"+today()+".zip");
   startActivityForResult(intent,"GPS".equals(part)?65:66);
  });
 }
 void importSplitBundle(){
  if(restoreIsBlocked()){explainRestoreBlocked();return;}
  Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT);
  intent.setType("application/zip");intent.addCategory(Intent.CATEGORY_OPENABLE);
  startActivityForResult(intent,67);
 }
 void prepareSplitRestore(Uri uri){
  if(restoreIsBlocked()){explainRestoreBlocked();return;}
  new Thread(()->{
   RestoreArchive.Prepared prepared=null;
   try{
    try(InputStream data=getContentResolver().openInputStream(uri)){
     if(data==null)throw new IOException("ZIP을 열 수 없어요.");
     prepared=RestoreArchive.prepare(data,getFilesDir());
    }
    if(!prepared.isSplit())throw new IOException("분할 ZIP 전용 메뉴입니다. 전체 ZIP은 일반 복원을 사용하세요.");
    final RestoreArchive.Prepared ready=prepared;
    prepared=null;
    runOnUiThread(()->{
     if(isFinishing()||isDestroyed()){ready.close();return;}
     new AlertDialog.Builder(this).setTitle(ready.part+" 분할 백업 추가 복원")
      .setMessage("GPS "+ready.rides+"건 · 사진 "+ready.photos+"장 · 영수증 "+ready.receipts+"장\n\n기존 파일은 덮어쓰지 않으며, 다른 파일은 지우지 않습니다. JSON 기록도 자동 교체하지 않습니다. 전체 복원 대신 필요한 부분만 추가합니다.")
      .setNegativeButton("취소",(d,w)->ready.close())
      .setOnCancelListener(d->ready.close())
      .setPositiveButton("원본 파일 추가 복원",(d,w)->{
       new Thread(()->{
        boolean entered=false;
        try{
         if(!RestoreRideGate.begin(RideService.running,RideService.autoArmed))
          throw new IOException(RestoreRideGate.MESSAGE);
         entered=true;
         int added=ready.installPart(getFilesDir());
         runOnUiThread(()->{
          if(isFinishing()||isDestroyed())return;
          new AlertDialog.Builder(this).setTitle("분할 복원 완료")
           .setMessage("새 GPS·사진 등 "+added+"개 파일을 추가했습니다. 기존 JSON 기록은 유지했습니다. 분할 백업에 들어 있던 JSON을 사용하려면 기존 기록을 백업한 다음 JSON 가져오기를 따로 실행하세요.")
           .setPositiveButton("확인",null).show();
         });
        }catch(Exception e){runOnUiThread(()->{if(!isFinishing()&&!isDestroyed())error("분할 복원 실패: "+e.getMessage());});}
        finally{ready.close();if(entered)RestoreRideGate.end();}
       },"moto-split-zip-restore").start();
      }).show();
    });
   }catch(Exception e){
    if(prepared!=null)prepared.close();
    runOnUiThread(()->{if(!isFinishing()&&!isDestroyed())error("분할 ZIP 검사 실패: "+e.getMessage());});
   }
  },"moto-split-zip-prepare").start();
 }
 void exportEncryptedBundle(){showBackupPassword(true,password->launchBackupDocument(password));}
 void importEncryptedBundle(){
  if(restoreIsBlocked()){explainRestoreBlocked();return;}
  showBackupPassword(false,password->{
   if(pendingSecurePassword!=null)Arrays.fill(pendingSecurePassword,'\0');
   pendingSecurePassword=password;
   Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("application/octet-stream");i.addCategory(Intent.CATEGORY_OPENABLE);
   i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"application/octet-stream","*/*"});startActivityForResult(i,48);
  });
 }
 interface PasswordChoice{void picked(char[] password);}
 void showBackupPassword(boolean confirm,PasswordChoice next){
  LinearLayout fields=new LinearLayout(this);fields.setOrientation(LinearLayout.VERTICAL);fields.setPadding(dp(20),dp(6),dp(20),0);
  EditText first=new EditText(this);first.setSingleLine(true);first.setHint("암호 (12자 이상)");first.setInputType(129);fields.addView(first);
  EditText second=null;
  if(confirm){second=new EditText(this);second.setSingleLine(true);second.setHint("암호 다시 입력");second.setInputType(129);fields.addView(second);}
  final EditText repeated=second;
  AlertDialog alert=new AlertDialog.Builder(this).setTitle(confirm?"암호화 전체 백업":"암호화 백업 복원")
   .setMessage(confirm?"AES-256-GCM으로 GPS·사진·기록을 보호합니다. 비밀번호는 저장하지 않습니다. 잊으면 복구할 수 없어요.":"백업을 만들 때 사용한 비밀번호가 필요합니다. 틀리면 기존 기록을 수정하지 않습니다.")
   .setView(fields).setNegativeButton("취소",null).setPositiveButton("계속",null).create();
  alert.setOnShowListener(d->alert.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
   char[] password=first.getText().toString().toCharArray();
   char[] confirmPassword=confirm?repeated.getText().toString().toCharArray():null;
   boolean valid=BackupCrypto.validPassword(password)&&(!confirm||Arrays.equals(password,confirmPassword));
   if(confirmPassword!=null)Arrays.fill(confirmPassword,'\0');
   if(!valid){Arrays.fill(password,'\0');first.setError(confirm?"12~256자이며 두 암호가 같아야 합니다.":"암호는 12~256자여야 합니다.");return;}
   first.setText("");if(repeated!=null)repeated.setText("");alert.dismiss();next.picked(password);
  }));alert.show();
 }
 void launchBackupDocument(char[] password){
  attempt(()->{
   if(!BackupExportGate.canExport(RideService.running,RideService.autoArmed,RestoreRideGate.isRestoring()))throw new IOException(BackupExportGate.MESSAGE);
   if(pendingSecurePassword!=null)Arrays.fill(pendingSecurePassword,'\0');
   pendingSecurePassword=password;
   pendingBundleJson=readOnly?new String(disk.readFully(),StandardCharsets.UTF_8):db.toString(2);
   boolean secure=password!=null;
   Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.setType(secure?"application/octet-stream":"application/zip");i.addCategory(Intent.CATEGORY_OPENABLE);
   i.putExtra(Intent.EXTRA_TITLE,"MOTO-HUB-FULL-"+today()+(secure?".mhub":".zip"));startActivityForResult(i,secure?47:45);
  });
 }
 void exportData(){attempt(()->{pendingExport=readOnly?new String(disk.readFully(),StandardCharsets.UTF_8):db.toString(2);Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.setType("application/json");i.addCategory(Intent.CATEGORY_OPENABLE);i.putExtra(Intent.EXTRA_TITLE,"MOTO-HUB-"+today()+".json");startActivityForResult(i,41);});}
 /** JSON-only additive import; ZIP restore remains a full replacement with GPS/photo rollback. */
 void importJsonMerge(){
  if(readOnly){error("원본 보호 중에는 기록을 합칠 수 없어요. 먼저 복구 상태를 확인해 주세요.");return;}
  if(restoreIsBlocked()){explainRestoreBlocked();return;}
  new AlertDialog.Builder(this).setTitle("JSON 기록 추가 · 중복 제외")
   .setMessage("현재 바이크·정비·주유·매물 기록은 절대 덮어쓰지 않고, 새 ID의 기록만 추가합니다. 동일 매물 주소도 제외합니다. GPS·사진·설정은 합치지 않습니다.\n\n진행 전 전체 ZIP 백업을 외부에 보관하는 걸 권장합니다.")
   .setNegativeButton("취소",null)
   .setPositiveButton("JSON 파일 선택",(d,w)->{
    Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);
    i.setType("application/json");
    i.addCategory(Intent.CATEGORY_OPENABLE);
    i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"application/json","text/plain","application/octet-stream"});
    startActivityForResult(i,61);
   }).show();
 }
 void prepareJsonMerge(Uri uri){
  // Snapshot on UI, then do both JSON parsing AND large collection diffing off-main.
  if(readOnly||restoreIsBlocked()){error("라이딩 중이거나 원본 보호 중에는 병합할 수 없어요.");return;}
  final String previewSnapshot=db.toString();
  toast("JSON 검사·중복 비교 중…");
  new Thread(()->{
   try{
    JSONObject source;
    try(InputStream in=getContentResolver().openInputStream(uri)){
     if(in==null)throw new IOException("JSON 파일을 열지 못했어요.");
     ByteArrayOutputStream bytes=new ByteArrayOutputStream();
     byte[] chunk=new byte[8192];int n;
     while((n=in.read(chunk))!=-1){
      if(bytes.size()+n>8_000_000)throw new IOException("JSON 파일 최대 크기는 8MB입니다.");
      bytes.write(chunk,0,n);
     }
     source=Store.parse(bytes.toString("UTF-8"));
    }
    final JsonDataMerger.Result preview=JsonDataMerger.merge(Store.parse(previewSnapshot),source);
    runOnUiThread(()->{
     if(isFinishing()||isDestroyed())return;
     if(readOnly||restoreIsBlocked()){error("라이딩 중이거나 원본 보호 중에는 병합할 수 없어요.");return;}
     if(!db.toString().equals(previewSnapshot)){
      error("검사 중 기록이 변경됐어요. 최신 기록을 기준으로 다시 파일을 선택해 주세요.");return;
     }
     new AlertDialog.Builder(this).setTitle("JSON 병합 미리보기")
      .setMessage("새 기록 "+preview.added+"개 · 중복 "+preview.skipped+"개 · 차량 보류 "+preview.blocked+"개.\n\n차량 ID가 겹친 정비·주유는 자동 연결하지 않습니다. GPS·사진·설정은 병합하지 않습니다.")
      .setNegativeButton("취소",null)
      .setNeutralButton("보류된 기록 직접 연결",(d,w)->{
       if(preview.blocked==0){toast("보류된 기록이 없어요.");return;}
       try{
        java.util.List<JSONObject> clashes=JsonDataMerger.conflicts(Store.parse(previewSnapshot),source);
        selectJsonMergeBikes(previewSnapshot,source,clashes,new java.util.HashMap<>(),0);
       }catch(Exception e){error("차량 확인 실패: "+e.getMessage());}
      })
      .setPositiveButton("안전한 기록만 추가",(d,w)->commitJsonMerge(source,new java.util.HashMap<>())).show();
    });
   }catch(Exception e){runOnUiThread(()->{if(!isFinishing()&&!isDestroyed())error("JSON 병합 불가: "+e.getMessage());});}
  },"moto-json-merge-preview").start();
 }

 /** A quick-log odometer is suggested from a locally chosen photo and applied only after approval. */
  void selectJsonMergeBikes(String snapshot,JSONObject source,java.util.List<JSONObject> collisions,
   java.util.Map<String,String> selections,int index){
  if(restoreIsBlocked()||!snapshot.equals(db.toString())){error("기록이 바뀌어 병합 선택을 취소했습니다.");return;}
  if(index>=collisions.size()){
   new AlertDialog.Builder(this).setTitle("바이크 직접 연결 · 최종 확인")
    .setMessage("사용자가 직접 연결한 원본 바이크 "+selections.size()+"대입니다. 이 차량에 속한 정비·주유 기록만 지정한 바이크에 추가됩니다. 기존 기록은 덮어쓰지 않습니다.")
    .setNegativeButton("취소",null)
    .setPositiveButton("확인하고 추가",(d,w)->commitJsonMerge(source,new java.util.HashMap<>(selections))).show();
   return;
  }
  JSONObject sourceBike=collisions.get(index);
  String id=sourceBike.optString("id"),name=sourceBike.optString("name");
  java.util.List<JSONObject> destinations=items("vehicles");
  String[] names=new String[destinations.size()+1];names[0]="이 원본 바이크 기록은 보류";
  for(int i=0;i<destinations.size();i++){
   JSONObject bike=destinations.get(i);
   names[i+1]=bike.optString("name")+" · "+(bike.optInt("year")>0?bike.optInt("year")+"년식 · ":"")+
    num(bike.optDouble("odo"))+" km";
  }
  new AlertDialog.Builder(this).setTitle("원본 바이크 "+(index+1)+"/"+collisions.size()+" · "+name)
   .setMessage("원본 JSON에서 보류된 정비·주유 기록을 연결할 실제 바이크를 확인하세요.\n\n"+JsonConflictPreview.text(source,id))
   .setItems(names,(d,choice)->{
    if(choice>0)selections.put(id,destinations.get(choice-1).optString("id"));else selections.remove(id);
    selectJsonMergeBikes(snapshot,source,collisions,selections,index+1);
   }).setNegativeButton("전체 취소",null).show();
 }

  void commitJsonMerge(JSONObject source,java.util.Map<String,String> mapping){
       if(readOnly||!RestoreRideGate.begin(RideService.running,RideService.autoArmed)){
        error("라이딩이 진행 중이거나 데이터를 보호하는 중이에요.");return;
       }
       // Block concurrent ride starts while the validated merge is computed.
       final String currentSnapshot=db.toString();
       toast("기록 병합·안전 백업 준비 중…");
       new Thread(()->{
        JsonDataMerger.Result actual=null;Exception problem=null;
        try{actual=JsonDataMerger.merge(Store.parse(currentSnapshot),source,mapping);}
        catch(Exception e){problem=e;}
        final JsonDataMerger.Result ready=actual;final Exception failure=problem;
        runOnUiThread(()->{
         try{
          if(isFinishing()||isDestroyed())return;
          if(failure!=null){error("병합 검증 실패: "+failure.getMessage());return;}
          if(readOnly||RideService.running||RideService.autoArmed
             ||!db.toString().equals(currentSnapshot)){
           error("작업 도중 기록이 변경됐어요. 다시 병합을 시작해 주세요.");return;
          }
          if(ready.added==0){toast("추가할 새 기록이 없어요.");return;}
          AtomicFile rollback=new AtomicFile(new File(getFilesDir(),"before-json-merge.json"));
          atomic(rollback,currentSnapshot.getBytes(StandardCharsets.UTF_8));
          write(ready.data);
          render();
          toast("새 기록 "+ready.added+"개 추가 · 중복 "+ready.skipped+"개 · 차량 연결 보류 "+ready.blocked+"개");
         }catch(Exception e){error("JSON 병합 저장 실패: "+e.getMessage());}
         finally{RestoreRideGate.end();}
        });
       },"moto-json-merge-commit").start();

 }

 void chooseQuickOdometerPhoto(EditText field){
  if(readOnly||dialog==null||field==null||quickOdoField!=field){toast("주유 기록 화면을 확인해 주세요.");return;}
  Intent pick=new Intent(Intent.ACTION_OPEN_DOCUMENT);
  pick.addCategory(Intent.CATEGORY_OPENABLE);pick.setType("image/*");
  startActivityForResult(pick,57);
 }
 void startQuickOdometerCamera(EditText target){
  if(readOnly||dialog==null||!dialog.isShowing()||quickOdoField!=target){
   toast("주유 기록 화면을 확인해 주세요.");return;
  }
  if(!cameraCaptureName.isEmpty()){toast("이미 촬영 중이에요.");return;}
  File file=null;
  try{
   file=ReceiptCameraFiles.create(getCacheDir());
   Uri photo=androidx.core.content.FileProvider.getUriForFile(this,getPackageName()+".receiptcamera",file);
   Intent capture=new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
   capture.putExtra(android.provider.MediaStore.EXTRA_OUTPUT,photo);
   capture.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_READ_URI_PERMISSION);
   capture.setClipData(ClipData.newUri(getContentResolver(),"계기판 임시 사진",photo));
   cameraCaptureName=file.getName();
   startActivityForResult(capture,58);
  }catch(Exception e){
   if(file!=null)ReceiptCameraFiles.delete(getCacheDir(),file.getName());
   cameraCaptureName="";
   error("카메라를 열지 못했어요. 갤러리에서 선택하거나 직접 입력해 주세요.");
  }
 }
 void scanQuickOdometerPhoto(Uri uri){scanQuickOdometerPhoto(uri,null);}
 void scanQuickOdometerPhoto(Uri uri,Runnable cleanup){
  final EditText target=quickOdoField;
  if(target==null||dialog==null||!dialog.isShowing()){
   if(cleanup!=null)cleanup.run();
   toast("주유 기록을 다시 열어 주세요.");return;
  }
  toast("계기판 사진을 기기 안에서 인식 중이에요.");
  ReceiptOcr.scan(this,uri,new ReceiptOcr.Callback(){
   public void failure(String message){
    if(cleanup!=null)cleanup.run();
    if(!isFinishing()&&!isDestroyed()&&quickOdoField==target)error(message);
   }
   public void success(String recognized){
    if(cleanup!=null)cleanup.run();
    if(isFinishing()||isDestroyed()||quickOdoField!=target||dialog==null||!dialog.isShowing())return;
    java.util.List<String> suggestions=OdometerOcrCandidates.fromText(recognized);
    if(suggestions.isEmpty()){error("주행거리 숫자를 찾지 못했어요. 숫자가 선명한 사진을 선택하거나 직접 입력해 주세요.");return;}
    String[] choices=new String[suggestions.size()];
    for(int i=0;i<choices.length;i++)choices[i]=suggestions.get(i)+" km";
    new AlertDialog.Builder(MainActivity.this)
      .setTitle("계기판 주행거리 확인")
      .setMessage("OCR 인식값이 실제 계기판과 같은지 확인하세요. 입력칸에만 적용하며 저장은 따로 해야 합니다.")
      .setSingleChoiceItems(choices,0,(dialog,index)->{
       if(quickOdoField==target&&MainActivity.this.dialog!=null&&MainActivity.this.dialog.isShowing()){
        String candidate=suggestions.get(index);
        target.setText(candidate);target.setSelection(target.getText().length());
        toast("입력칸에만 적용했어요. 확인 후 저장해 주세요.");
       }
       dialog.dismiss();
      }).setNegativeButton("직접 입력",null).show();
   }
  });
 }
 /** The OCR suggestion is never persisted or applied without explicit user approval. */
 void scanReceiptOcr(Uri uri,Runnable cleanup){
  final Map<String,EditText> fields=ocrFields;final String key=ocrKind;
  ocrFields=null;ocrKind="";
  if(fields==null||!key.equals(activeForm)){
   if(cleanup!=null)cleanup.run();toast("입력 화면을 다시 열어 주세요.");return;
  }
  toast("영수증을 기기 안에서 읽고 있어요. 완료 후 값을 확인해 주세요.");
  ReceiptOcr.scan(this,uri,new ReceiptOcr.Callback(){
   public void failure(String reason){if(cleanup!=null)cleanup.run();if(!isFinishing()&&!isDestroyed())error(reason);}
   public void success(String text){
    if(cleanup!=null)cleanup.run();
    if(isFinishing()||isDestroyed()||draftFields!=fields||!key.equals(activeForm))return;
    ReceiptTextParser.Result candidate=ReceiptTextParser.parse(text);
    if(!candidate.hasAny()){error("금액·주유량·날짜 후보가 없어요. 직접 입력해 주세요.");return;}
    if(candidate.amountCandidates.size()>1){
     String[] labels=new String[candidate.amountCandidates.size()];
     for(int i=0;i<labels.length;i++)labels[i]=candidate.amountCandidates.get(i)+"원"+(i==0?"  ·  추천":"");
     new AlertDialog.Builder(MainActivity.this).setTitle("OCR 결제금액 선택")
      .setItems(labels,(d,index)->showReceiptOcrProposal(fields,key,candidate,candidate.amountCandidates.get(index)))
      .setNegativeButton("직접 입력",null).show();
    }else showReceiptOcrProposal(fields,key,candidate,candidate.amount);
   }
  });
 }
 void showReceiptOcrProposal(Map<String,EditText> fields,String key,ReceiptTextParser.Result proposal,String selectedAmount){
  if(draftFields!=fields||!key.equals(activeForm)||isFinishing()||isDestroyed())return;
  EditText amount=fields.get("services".equals(key)?"cost":"amount");
  String current=amount==null?"":amount.getText().toString().trim();
  boolean replacing=!current.isEmpty()&&!current.equals(selectedAmount)&&!selectedAmount.isEmpty();
  String message=(selectedAmount.isEmpty()?"":"금액 "+selectedAmount+"원\n")+
   (proposal.liters.isEmpty()?"":"주유량 "+proposal.liters+"L\n")+
   (proposal.date.isEmpty()?"":"날짜 "+proposal.date+"\n")+
   (replacing?"\n이미 입력한 금액 "+current+"원을 덮어씁니다.\n":"\n")+
   "인식 오류가 있을 수 있습니다. 영수증과 대조하세요. 저장은 따로 해야 합니다.";
  new AlertDialog.Builder(this).setTitle("영수증 인식 후보 확인")
   .setMessage(message).setNegativeButton("수동 입력",null)
   .setPositiveButton("입력칸에 적용",(d,w)->{
    if(draftFields!=fields||!key.equals(activeForm))return;
    if(amount!=null&&!selectedAmount.isEmpty())amount.setText(selectedAmount);
    EditText liters=fields.get("liters");if(liters!=null&&!proposal.liters.isEmpty())liters.setText(proposal.liters);
    EditText date=fields.get("date");if(date!=null&&!proposal.date.isEmpty()){
     date.setText(proposal.date);
     if(ocrDateMileageFields!=null)ocrDateMileageFields.setVisibility(View.VISIBLE);
     if(ocrMetaSummary!=null)ocrMetaSummary.setText("영수증 날짜를 인식했어요. 아래 값을 확인하세요.");
    }
    if(formOptionalSection!=null&&hasPopulatedOptionalFields(fields)){
     formOptionalSection.setVisibility(View.VISIBLE);updateOptionalButton();
    }
    toast("입력칸에만 적용했어요. 확인 후 저장해 주세요.");
   }).show();
 }
 @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request==90){toast(result==RESULT_OK?"설치 결과를 확인했습니다.":"설치 취소 또는 실패 · 기존 기록과 검증된 다운로드는 유지됩니다. 더보기 → 앱 업데이트에서 다시 설치할 수 있어요.");return;}if(request==55||request==56||request==58){finishReceiptCapture(request,result);return;}if(result!=RESULT_OK||data==null||data.getData()==null){if(request==47||request==48){if(pendingSecurePassword!=null)Arrays.fill(pendingSecurePassword,'\0');pendingSecurePassword=null;}if(request==45||request==47||request==65||request==66)pendingBundleJson="";if(request==52)garagePhotoTarget="";if(request==53){receiptPhotoTarget="";receiptPhotoKey="";receiptPhotoSlot=1;}if(request==54){ocrFields=null;ocrKind="";}return;}Uri uri=data.getData();if(request==61){prepareJsonMerge(uri);return;}if(request==52){final String bikeId=garagePhotoTarget;garagePhotoTarget="";if(readOnly){error("원본 보호 중에는 사진을 변경할 수 없어요.");return;}
   GARAGE_PHOTO_SAVER.execute(()->{try{saveGaragePhoto(uri,bikeId);}catch(Exception error){runOnUiThread(()->{if(!isFinishing()&&!isDestroyed())this.error("바이크 사진 저장 실패: "+error.getMessage());});}});return;}if(request==53){new Thread(()->{try{saveReceiptPhoto(uri);}catch(Exception e){runOnUiThread(()->error("영수증 저장 실패: "+e.getMessage()));}},"moto-receipt-photo").start();return;}
  if(request==54){scanReceiptOcr(uri,null);return;}
  if(request==57){scanQuickOdometerPhoto(uri);return;}
  if(request==60){
   if(RideService.running||RideService.autoArmed||RestoreRideGate.isRestoring()){error("라이딩 종료 후 검사해 주세요.");return;}
   new Thread(()->{
    try{
     RestoreArchive.Prepared prepared;
     try(InputStream in=getContentResolver().openInputStream(uri)){
      if(in==null)throw new IOException("백업 파일을 열 수 없어요.");
      // Stage into temporary private storage; never mutate live data.
      prepared=RestoreArchive.prepare(in,getFilesDir());
     }
     try(prepared){
      Store.parse(prepared.json);
      final int rides=prepared.rides,photos=prepared.photos;
      runOnUiThread(()->{if(!isFinishing()&&!isDestroyed())
       new AlertDialog.Builder(this).setTitle("ZIP 복구 사전 검증 통과")
       .setMessage("백업 내용·데이터 JSON·GPS 및 사진 구조 검사 통과\\nGPS "+rides+"건 · 사진 "+photos+"장\\n검사 중 기존 기록은 변경하지 않았습니다. 실제 복원 시 여유공간과 기기 상태를 다시 확인합니다.")
       .setPositiveButton("확인",null).show();
      });
     }
    }catch(Exception failure){runOnUiThread(()->{if(!isFinishing()&&!isDestroyed())
      error("ZIP 복구 사전 검증 실패: "+(failure.getMessage()==null?"파일/용량을 확인해 주세요.":failure.getMessage()));});}
   },"moto-zip-dryrun").start();
   return;
  }
  if(request==46){prepareBundleRestore(uri);return;}
  if(request==67){prepareSplitRestore(uri);return;}
  if(request==48){char[] password=pendingSecurePassword;pendingSecurePassword=null;
   if(password==null){error("암호를 다시 입력해 주세요.");return;}
   prepareBundleRestore(uri,password);return;
  }
  if(request==45||request==47||request==65||request==66){
   final char[] password=(request==47?pendingSecurePassword:null);pendingSecurePassword=null;
   final String part=request==65?"GPS":request==66?"MEDIA":"FULL";
   final String snapshot=pendingBundleJson;pendingBundleJson="";
   if(snapshot.isEmpty()){error("백업 정보를 찾지 못했어요. 다시 시도하세요.");return;}
   new Thread(()->{
    boolean reserved=RestoreRideGate.beginExport(RideService.running,RideService.autoArmed);
    try{
    if(!reserved)throw new IOException(BackupExportGate.MESSAGE);
    int count;
    try(OutputStream out=getContentResolver().openOutputStream(uri,"wt")){
     if(out==null)throw new IOException("백업 파일을 열 수 없어요.");
     if(password==null)count=BackupBundle.export(snapshot,getFilesDir(),out,part);
     else try(OutputStream secure=BackupCrypto.encrypt(out,password)){
      count=BackupBundle.export(snapshot,getFilesDir(),secure);
     }
    }
    // Validate every authenticated encrypted chunk BEFORE trusting the ZIP.
    File authenticated=null;
    BackupIntegrity.Result checkedBackup;
    try{
     if(password!=null){
      try(InputStream check=getContentResolver().openInputStream(uri)){
       if(check==null)throw new IOException("암호화 백업 검증을 위해 파일을 다시 열 수 없어요.");
       authenticated=BackupCrypto.decryptToPrivateFile(check,password,getCacheDir());
      }
     }
     try(InputStream check=authenticated!=null?new FileInputStream(authenticated):getContentResolver().openInputStream(uri)){
      if(check==null)throw new IOException("백업 검증을 위해 파일을 다시 열 수 없어요.");
      checkedBackup=BackupIntegrity.verify(check);
     }
    }finally{if(authenticated!=null)java.nio.file.Files.deleteIfExists(authenticated.toPath());}
    // Allow safe trash only for exact GPS bytes actually found in the verified user-owned ZIP.
    if(!"MEDIA".equals(part))BackupGpsProof.record(this,checkedBackup.trackSha256);
    runOnUiThread(()->{if(!isFinishing()&&!isDestroyed())toast((password==null?("FULL".equals(part)?"전체 백업":part+" 분할 백업"):"암호화 백업")+" ZIP 검증 완료 · "+count+"개 항목");});
   }catch(Exception e){runOnUiThread(()->{if(!isFinishing()&&!isDestroyed())error("통합 백업 실패: "+e.getMessage());});}
    finally{if(password!=null)Arrays.fill(password,'\0');if(reserved)RestoreRideGate.endExport();}
   },"MOTO-full-backup").start();return;
  }
  if(request==44){
   new Thread(()->{try{
    GpxImporter.Imported imported;
    try(InputStream in=getContentResolver().openInputStream(uri)){
     if(in==null)throw new IOException("GPX 파일을 열 수 없어요");
     imported=GpxImporter.importTrack(in,TrackFile.directory(getFilesDir()));
    }
    runOnUiThread(()->{if(!isFinishing()&&!isDestroyed()){toast(imported.count+"개 좌표 가져오기 완료");tab="라이딩";render();}});
   }catch(Exception e){runOnUiThread(()->{if(!isFinishing()&&!isDestroyed())error("GPX 가져오기 실패: "+e.getMessage());});}
   },"MOTO-GPX-import").start();return;
  }
  if(request==43)attempt(()->{String name=pendingTrack;pendingTrack="";if(!name.matches("[0-9]+-[a-f0-9-]+\\.track"))throw new IOException("내보낼 경로를 다시 선택해 주세요.");File f=new File(TrackFile.directory(getFilesDir()),name);if(RideService.running&&name.equals(RideService.active))throw new IOException("기록 종료 후 내보내세요.");try(OutputStream out=getContentResolver().openOutputStream(uri,"wt")){if(out==null)throw new IOException("파일 열기 실패");int points=TrackFile.gpx(f,out);toast(points+"개 좌표 GPX 저장 완료");}});else if(request==41)attempt(()->{if(pendingExport.isEmpty())throw new Exception("내보내기가 중단됐어요. 다시 시작해 주세요.");try(OutputStream out=getContentResolver().openOutputStream(uri,"wt")){if(out==null)throw new IOException("파일을 열 수 없습니다.");out.write(pendingExport.getBytes(StandardCharsets.UTF_8));}pendingExport="";toast("백업 파일을 저장했어요.");});else if(request==42)attempt(()->{if(restoreIsBlocked())throw new IOException(RestoreRideGate.MESSAGE);try(InputStream in=getContentResolver().openInputStream(uri)){if(in==null)throw new IOException("파일을 열 수 없습니다.");ByteArrayOutputStream buf=new ByteArrayOutputStream();byte[] chunk=new byte[8192];int n;while((n=in.read(chunk))!=-1){if(buf.size()+n>8_000_000)throw new IOException("백업 최대 크기는 8MB입니다.");buf.write(chunk,0,n);}confirmImport(Store.parse(buf.toString("UTF-8")));}});}
 void confirmImport(JSONObject next){
  if(restoreIsBlocked()){explainRestoreBlocked();return;}
  new AlertDialog.Builder(this).setTitle("이 백업으로 복원할까요?")
   .setMessage("바이크 "+next.optJSONArray("vehicles").length()+"대 / 매물 "+next.optJSONArray("listings").length()+"개\n현재 기록은 교체되며 복원 직전 사본을 보존합니다. 자동 병합하지 않습니다.")
   .setNegativeButton("취소",null)
   .setPositiveButton("복원",(d,w)->attempt(()->{
    if(!RestoreRideGate.begin(RideService.running,RideService.autoArmed))throw new IOException(RestoreRideGate.MESSAGE);
    try{
     // Validate first; restore should not mutate the live database on bad JSON.
     JSONObject validated=Store.parse(next.toString());
     byte[] previous=(readOnly?disk.readFully():db.toString(2).getBytes(StandardCharsets.UTF_8));
     AtomicFile rollback=new AtomicFile(new File(getFilesDir(),"before-import.json"));
     atomic(rollback,previous);
     // The daily pre-edit snapshot is a best-effort extra recovery point.
     LocalSafetyBackup.snapshot(this,db);
     // Commit JSON exactly once. Never run a fallible checkpoint inside the commit.
     atomic(disk,validated.toString(2).getBytes(StandardCharsets.UTF_8));
     db=validated;readOnly=false;
     try{LocalSafetyBackup.checkpoint(this,validated);}catch(Exception ignored){}
     try{MotoWidgetProvider.refresh(this);}catch(Exception ignored){}
     selected="";getSharedPreferences("garage_ui",MODE_PRIVATE).edit().remove("selected_bike").apply();compare.clear();render();toast("복원했어요.");
    }finally{RestoreRideGate.end();}
   })).show();
 }
 @Override public void onBackPressed(){back();}
 @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] results){
  super.onRequestPermissionsResult(request,permissions,results);
  if(request==DeveloperObdScreen.CONNECT_PERMISSION_REQUEST||request==DeveloperObdScreen.AUTO_CONNECT_PERMISSION_REQUEST||request==DeveloperObdScreen.ODO_CONNECT_PERMISSION_REQUEST){
   if(developerObd!=null){
    if(Build.VERSION.SDK_INT<31||checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT)==android.content.pm.PackageManager.PERMISSION_GRANTED) {if(request==DeveloperObdScreen.AUTO_CONNECT_PERMISSION_REQUEST)developerObd.loadPairedAuto();else if(request==DeveloperObdScreen.ODO_CONNECT_PERMISSION_REQUEST)developerObd.loadPairedOdo();else developerObd.loadPaired();}
    else toast("Bluetooth 연결 권한이 없어 OBD2 장치를 조회하지 않았어요.");
   }
   return;
  }
  if(request==72){
   if(!isFinishing()&&!isDestroyed()&&checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)==android.content.pm.PackageManager.PERMISSION_GRANTED)
    DailyMaintenanceJob.checkSoon(this);
   else toast("알림 권한이 꺼져 있어 정비 알림을 표시하지 못해요.");
   return;
  }
  if(request!=70&&request!=71)return;
  if(isFinishing()||isDestroyed()){rideStartConsent.cancel();launchNavigationAfterRide=false;return;}
  boolean granted=request==70
   ?checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)==android.content.pm.PackageManager.PERMISSION_GRANTED
   :Build.VERSION.SDK_INT<33||checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)==android.content.pm.PackageManager.PERMISSION_GRANTED;
  if(!rideStartConsent.mayContinue("라이딩".equals(tab),granted)){
   if(!granted){launchNavigationAfterRide=false;toast("권한이 허용되지 않아 GPS 기록을 시작하지 않았어요.");}
   render();return;
  }
  // The user already accepted the recording dialog. Continue without asking twice.
  new RideUi(this).start();
  render();
 }
 void back(){if(dialog!=null&&dialog.isShowing()){close();return;}if(tab.equals("더보기")){tab=previousTab;render();}else if(!tab.equals("홈")){tab="홈";render();}else finish();}

 @Override protected void onDestroy(){if(developerObd!=null){developerObd.stop();developerObd=null;}super.onDestroy();}
 @Override protected void onPause(){persistDraft();super.onPause();}
 void clearDraft(){formDirty=false;getSharedPreferences("draft",MODE_PRIVATE).edit().remove("active").apply();}
 void confirmDiscardDraft(){
  if(!getSharedPreferences("draft",MODE_PRIVATE).contains("active")){render();return;}
  new AlertDialog.Builder(this).setTitle("작성 중인 초안을 삭제할까요?")
   .setMessage("저장되지 않은 입력만 삭제합니다. 이미 저장된 바이크·정비·주유 기록은 그대로 유지됩니다.")
   .setNegativeButton("취소",null)
   .setPositiveButton("초안 삭제",(d,which)->{
    // Remove only the draft preference. Never touch the persistent motorcycle database.
    getSharedPreferences("draft",MODE_PRIVATE).edit().remove("active").apply();
    toast("초안을 삭제했어요.");render();
   }).show();
 }
 void persistDraft(){if(!formDirty||draftFields==null)return;try{JSONObject d=new JSONObject();d.put("key",draftKey);d.put("id",draftId);d.put("vehicle",draftVehicle);JSONObject values=new JSONObject();for(Map.Entry<String,EditText> e:draftFields.entrySet())values.put(e.getKey(),e.getValue().getText().toString());for(Map.Entry<String,Spinner> e:draftSelectors.entrySet())values.put(e.getKey(),e.getValue().getSelectedItem().toString());d.put("values",values);d.put("fullTank",fullTankBox!=null&&fullTankBox.isChecked());getSharedPreferences("draft",MODE_PRIVATE).edit().putString("active",d.toString()).apply();}catch(Exception e){toast("초안을 저장하지 못했어요. 입력 내용을 확인해 주세요.");}}
 boolean hasPopulatedOptionalFields(Map<String,EditText> inputs){
  if(inputs==null)return false;
  for(String key:formOptionalKeys){EditText input=inputs.get(key);
   if(input!=null&&OptionalFieldDisclosure.hasValue(key,input.getText().toString()))return true;
  }
  return false;
 }
 void updateOptionalButton(){
  if(formOptionalSection==null||formOptionalToggle==null)return;
  boolean open=formOptionalSection.getVisibility()==View.VISIBLE;
  formOptionalToggle.setText(open?"선택 입력 접기  ▴":"선택 입력 보기  ▾");
  formOptionalToggle.setContentDescription(open?"선택 입력 항목 접기":"선택 입력 항목 펼치기");
 }
 void revealOptionalDraft(){
  if(formOptionalSection!=null&&hasPopulatedOptionalFields(draftFields)){
   formOptionalSection.setVisibility(View.VISIBLE);updateOptionalButton();
  }
 }
 void resumeDraft(){attempt(()->{String raw=getSharedPreferences("draft",MODE_PRIVATE).getString("active","");JSONObject d=new JSONObject(raw);String key=d.getString("key"),id=d.optString("id"),vehicle=d.optString("vehicle");if(!Arrays.asList("vehicles","listings","services","expenses","routes","gear").contains(key))throw new Exception("지원하지 않는 초안입니다.");JSONObject original=id.isEmpty()?null:Store.find(db,key,id);if(!id.isEmpty()&&original==null)throw new Exception("원본 기록이 삭제되어 초안을 연결할 수 없어요.");if((key.equals("services")||key.equals("expenses"))&&!vehicle.isEmpty()){if(Store.find(db,"vehicles",vehicle)==null)throw new Exception("초안의 바이크가 삭제됐어요.");selected=vehicle;}edit(key,original);if(draftFields==null)return;JSONObject values=d.getJSONObject("values");for(Map.Entry<String,EditText> e:draftFields.entrySet())if(values.has(e.getKey()))e.getValue().setText(values.optString(e.getKey()));for(Map.Entry<String,Spinner> e:draftSelectors.entrySet()){Spinner sp=e.getValue();for(int i=0;i<sp.getCount();i++)if(sp.getItemAtPosition(i).toString().equals(values.optString(e.getKey())))sp.setSelection(i);}if(fullTankBox!=null)fullTankBox.setChecked(d.optBoolean("fullTank"));revealOptionalDraft();formDirty=true;persistDraft();});}
}
