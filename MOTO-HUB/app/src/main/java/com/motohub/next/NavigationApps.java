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
 /** A failed launcher can switch directly to another user-selected installed navigation app. */
 static void chooseInstalledAndLaunch(MainActivity activity){
  java.util.ArrayList<NavigationChoice> found=new java.util.ArrayList<>();
  for(NavigationChoice app:NavigationChoice.values())if(installedLaunchIntent(activity,app)!=null)found.add(app);
  if(found.isEmpty()){choose(activity);return;}
  String[] labels=new String[found.size()];
  for(int i=0;i<found.size();i++)labels[i]=found.get(i).label;
  new AlertDialog.Builder(activity).setTitle("설치된 내비 바로 실행")
   .setItems(labels,(d,index)->{
    NavigationChoice picked=found.get(index);
    if(!activity.getSharedPreferences(PREF,Context.MODE_PRIVATE).edit().putString(KEY,picked.id).commit()){
     activity.toast("기본 내비 저장에 실패했어요.");return;
    }
    MotoWidgetProvider.refresh(activity);
    RideService.refreshNotification();
    open(activity,picked);
   }).setNegativeButton("취소",null).show();
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
   .setNeutralButton("다른 내비 실행",(d,w)->chooseInstalledAndLaunch(activity))
   .setPositiveButton("Google Play 열기",(d,w)->{
    Uri store=Uri.parse("market://details?id="+choice.packageName);
    Uri web=Uri.parse("https://play.google.com/store/apps/details?id="+choice.packageName);
    try{activity.startActivity(new Intent(Intent.ACTION_VIEW,store));}
    catch(Exception e){try{activity.startActivity(new Intent(Intent.ACTION_VIEW,web));}
      catch(Exception noStore){activity.toast("스토어를 열 수 없어요.");}}
   }).show();
 }
}
