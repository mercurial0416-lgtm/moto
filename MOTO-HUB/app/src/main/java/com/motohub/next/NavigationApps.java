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
  new AlertDialog.Builder(activity)
   .setTitle("기본 내비 설정")
   .setMessage("선택만 저장합니다. 내비는 '내비 실행'을 눌렀을 때 열립니다. GPS·GPX·OBD 데이터는 전달하지 않습니다.")
   .setSingleChoiceItems(NavigationChoice.labels(),selected.ordinal(),(dialog,which)->{
    NavigationChoice selectedApp=NavigationChoice.at(which);
    boolean saved=activity.getSharedPreferences(PREF,Context.MODE_PRIVATE)
     .edit().putString(KEY,selectedApp.id).commit();
    dialog.dismiss();
    if(!saved){activity.toast("기본 내비 설정 저장에 실패했어요.");return;}
    RideService.refreshNotification();
    activity.toast("기본 내비: "+selectedApp.label);
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
 static void openPreferred(MainActivity activity){open(activity,preferred(activity));}
 static void open(MainActivity activity,NavigationChoice choice){
  Intent launch=installedLaunchIntent(activity,choice);
  if(launch!=null){
   try{activity.startActivity(launch);return;}
   catch(RuntimeException ignored){}
  }
  new AlertDialog.Builder(activity).setTitle(choice.label+" 앱을 실행할 수 없어요")
   .setMessage("앱 설치 상태를 확인하거나 다른 내비를 선택해 주세요. MOTO HUB는 오토바이 길찾기를 보장하지 않습니다.")
   .setNegativeButton("취소",null)
   .setNeutralButton("다른 내비 선택",(d,w)->choose(activity))
   .setPositiveButton("Google Play 열기",(d,w)->{
    Uri store=Uri.parse("market://details?id="+choice.packageName);
    Uri web=Uri.parse("https://play.google.com/store/apps/details?id="+choice.packageName);
    try{activity.startActivity(new Intent(Intent.ACTION_VIEW,store));}
    catch(Exception e){try{activity.startActivity(new Intent(Intent.ACTION_VIEW,web));}
      catch(Exception noStore){activity.toast("스토어를 열 수 없어요.");}}
   }).show();
 }
}
