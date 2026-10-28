package com.motohub.next;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

/** User launches an independent navigator; no GPS, GPX or ECU data is passed. */
final class NavigationApps {
 private static final String PREF="navigation_choice",KEY="preferred";
 private NavigationApps(){}
 static NavigationChoice preferred(Context context){
  return NavigationChoice.fromId(context.getSharedPreferences(PREF,Context.MODE_PRIVATE)
   .getString(KEY,NavigationChoice.KAKAO.id));
 }
 /** Change default without unexpectedly switching away from MOTO HUB. */
 static void choose(MainActivity activity){
  NavigationChoice selected=preferred(activity);
  String[] options=NavigationChoice.labels();
  for(int i=0;i<options.length;i++){
   NavigationChoice option=NavigationChoice.at(i);
   options[i]=(option==selected?"✓ ":"")+option.label+
    (installedLaunchIntent(activity,option)==null?" · 설치 확인 필요":" · 설치됨");
  }
  new AlertDialog.Builder(activity)
   .setTitle("내비 선택 · 현재 "+selected.label)
   .setItems(options,(dialog,which)->{
    NavigationChoice selectedApp=NavigationChoice.at(which);
    if(installedLaunchIntent(activity,selectedApp)==null){
     // Do not overwrite a working default with an unavailable app.
     new AlertDialog.Builder(activity).setTitle(selectedApp.label+" 실행 불가")
      .setMessage("설치 상태를 확인할 수 없어요. 기존 기본 내비 설정은 그대로 유지합니다.")
      .setNegativeButton("취소",null)
      .setNeutralButton("다른 내비 실행",(d,w)->chooseInstalledAndLaunch(activity))
      .setPositiveButton("설치 페이지",(d,w)->openStore(activity,selectedApp))
      .show();
     return;
    }
    boolean saved=activity.getSharedPreferences(PREF,Context.MODE_PRIVATE)
     .edit().putString(KEY,selectedApp.id).commit();
    if(!saved){activity.toast("내비 선택을 저장하지 못했어요.");return;}
    RideService.refreshNotification();
    MotoWidgetProvider.refresh(activity);
    activity.toast("선택 완료: "+selectedApp.label+" · 내비 실행 버튼을 누르세요.");
    if(!activity.isFinishing()&&!activity.isDestroyed())activity.render();
   })
   .setNegativeButton("취소",null).show();
 }
 static Intent installedLaunchIntent(Context context,NavigationChoice choice){
  try{
   Intent intent=context.getPackageManager().getLaunchIntentForPackage(choice.packageName);
   if(intent==null)return null;
   intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
   return intent;
  }catch(RuntimeException unavailable){return null;}
 }
 /** User-selected installed navigation fallback; no coordinates or ride data shared. */
 static void chooseInstalledAndLaunch(MainActivity activity){
  java.util.ArrayList<NavigationChoice> installed=new java.util.ArrayList<>();
  for(NavigationChoice app:NavigationChoice.values())
   if(installedLaunchIntent(activity,app)!=null)installed.add(app);
  if(installed.isEmpty()){
   // Never bounce between two selection dialogs when no navigator is installed.
   NavigationChoice preferred=preferred(activity);
   new AlertDialog.Builder(activity).setTitle("내비 앱 설치 필요")
    .setMessage("설치된 내비를 찾지 못했어요. 먼저 "+preferred.label+"을 설치하거나 다른 내비를 선택하세요. GPS 기록은 중단되지 않습니다.")
    .setNegativeButton("닫기",null)
    .setNeutralButton("다른 내비 선택",(d,w)->choose(activity))
    .setPositiveButton(preferred.label+" 설치",(d,w)->openStore(activity,preferred))
    .show();
   return;
  }
  String[] labels=new String[installed.size()];
  for(int i=0;i<installed.size();i++)labels[i]=installed.get(i).label;
  new AlertDialog.Builder(activity).setTitle("설치된 다른 내비 실행")
   .setItems(labels,(d,which)->{
    NavigationChoice target=installed.get(which);
    if(!activity.getSharedPreferences(PREF,Context.MODE_PRIVATE)
        .edit().putString(KEY,target.id).commit()){
     activity.toast("기본 내비 저장에 실패했어요.");return;
    }
    MotoWidgetProvider.refresh(activity);RideService.refreshNotification();
    open(activity,target);
   }).setNegativeButton("취소",null).show();
 }
 static void openPreferred(MainActivity activity){open(activity,preferred(activity));}
 static void open(MainActivity activity,NavigationChoice choice){
  Intent launch=installedLaunchIntent(activity,choice);
  if(launch!=null){
   try{
    activity.suppressUpdatePromptUntilElapsed=android.os.SystemClock.elapsedRealtime()+120_000L;
    activity.startActivity(launch);return;
   }catch(RuntimeException ignored){}
  }
  // This is only the external app failing. Never stop or modify GPS/OBD.
  // A configured but crashed navigator may be retried without reinstalling it.
  boolean installed=launch!=null;
  new AlertDialog.Builder(activity).setTitle(choice.label+" 실행 실패")
   .setMessage(installed
     ?"내비가 설치되어 있지만 실행되지 않았어요. 다시 시도하거나 다른 내비를 실행하세요. GPS 기록은 계속됩니다."
     :"선택한 내비를 찾을 수 없어요. 설치 상태를 확인하거나 다른 내비를 실행하세요. GPS 기록은 계속됩니다.")
   .setNegativeButton("닫기",null)
   .setNeutralButton("다른 내비 실행",(d,w)->chooseInstalledAndLaunch(activity))
   .setPositiveButton(installed?"다시 실행":"설치 페이지",
     (d,w)->{if(installed)open(activity,choice);else openStore(activity,choice);}).show();
 }
 private static void openStore(MainActivity activity,NavigationChoice choice){
  Uri store=Uri.parse("market://details?id="+choice.packageName);
  Uri web=Uri.parse("https://play.google.com/store/apps/details?id="+choice.packageName);
  try{
   activity.suppressUpdatePromptUntilElapsed=android.os.SystemClock.elapsedRealtime()+120_000L;
   activity.startActivity(new Intent(Intent.ACTION_VIEW,store));
  }catch(Exception e){
   try{activity.startActivity(new Intent(Intent.ACTION_VIEW,web));}
   catch(Exception noStore){activity.toast("스토어를 열 수 없어요.");}
  }
 }
}
