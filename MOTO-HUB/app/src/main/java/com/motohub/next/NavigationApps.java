package com.motohub.next;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

/** Open installed navigation app without passing user GPS, GPX or OBD data. */
final class NavigationApps {
 private static final String PREF="navigation_choice",KEY="preferred";
 private NavigationApps(){}
 static NavigationChoice preferred(Context context){
  return NavigationChoice.fromId(context.getSharedPreferences(PREF,Context.MODE_PRIVATE)
   .getString(KEY,NavigationChoice.KAKAO.id));
 }
 static void choose(MainActivity activity){
  NavigationChoice selected=preferred(activity);
  new AlertDialog.Builder(activity)
   .setTitle("내비게이션 선택")
   .setMessage("선택한 내비 앱을 실행하고 다음부터 라이딩 알림에도 표시합니다. GPS·GPX·주행 정보는 전달하지 않습니다.")
   .setSingleChoiceItems(NavigationChoice.labels(),selected.ordinal(),(dialog,which)->{
    NavigationChoice app=NavigationChoice.at(which);
    activity.getSharedPreferences(PREF,Context.MODE_PRIVATE).edit().putString(KEY,app.id).apply();
    dialog.dismiss();
    open(activity,app);
   })
   .setNegativeButton("취소",null)
   .show();
 }
 static Intent installedLaunchIntent(Context context,NavigationChoice choice){
  try{
   Intent intent=context.getPackageManager().getLaunchIntentForPackage(choice.packageName);
   if(intent==null)return null;
   intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
   return intent;
  }catch(RuntimeException unavailable){return null;}
 }
 static void open(MainActivity activity,NavigationChoice choice){
  Intent launch=installedLaunchIntent(activity,choice);
  if(launch!=null){
   try{activity.startActivity(launch);return;}
   catch(RuntimeException ignored){}
  }
  new AlertDialog.Builder(activity).setTitle(choice.label+" 앱이 설치되지 않았어요")
   .setMessage(choice.label+" 설치 후 선택해 주세요. MOTO HUB는 경로를 전달하거나 자동차 전용 도로를 추천하지 않습니다.")
   .setNegativeButton("취소",null)
   .setPositiveButton("Google Play 열기",(dialog,button)->{
    Uri store=Uri.parse("market://details?id="+choice.packageName);
    Uri web=Uri.parse("https://play.google.com/store/apps/details?id="+choice.packageName);
    try{activity.startActivity(new Intent(Intent.ACTION_VIEW,store));}
    catch(Exception e){try{activity.startActivity(new Intent(Intent.ACTION_VIEW,web));}
       catch(Exception noStore){activity.toast("스토어를 열 수 없어요.");}}
   }).show();
 }
}
