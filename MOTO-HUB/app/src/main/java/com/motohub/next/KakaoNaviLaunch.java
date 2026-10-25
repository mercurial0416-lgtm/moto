package com.motohub.next;
import android.content.Intent;
import android.net.Uri;
/** Opens the independent navigation app. Never forwards our ride GPS or group data. */
final class KakaoNaviLaunch {
 static final String PACKAGE="com.locnall.KimGiSa";
 static void open(MainActivity activity){
  try{
   Intent launch=activity.getPackageManager().getLaunchIntentForPackage(PACKAGE);
   if(launch==null){
    new android.app.AlertDialog.Builder(activity).setTitle("카카오내비가 설치되어 있지 않아요")
     .setMessage("카카오내비를 설치하면 MOTO HUB 라이딩 기록과 별개로 길 안내를 사용할 수 있습니다. 라이딩 기록은 카카오내비로 자동 전송하지 않습니다.")
     .setNegativeButton("취소",null).setPositiveButton("스토어 열기",(d,w)->{
      try{activity.startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("market://details?id="+PACKAGE)));}
      catch(Exception e){activity.startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://play.google.com/store/apps/details?id="+PACKAGE)));}
     }).show();
    return;
   }
   activity.startActivity(launch);
  }catch(Exception e){activity.toast("카카오내비를 열지 못했어요. 설치 상태를 확인해 주세요.");}
 }
 private KakaoNaviLaunch(){}
}
