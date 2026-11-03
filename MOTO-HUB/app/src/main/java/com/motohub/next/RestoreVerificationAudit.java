package com.motohub.next;

import android.content.Context;
import android.content.SharedPreferences;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Private, bounded result of the LAST committed full-ZIP restoration.
 * No GPS coordinates, media paths, personal data or original file hashes are stored.
 */
final class RestoreVerificationAudit {
 private static final String PREFS="full_restore_verification";
 private RestoreVerificationAudit(){}

 static boolean record(Context c,boolean passed,int verifiedFiles,int rides,int photos,int receipts){
  if(c==null)return false;
  return c.getSharedPreferences(PREFS,0).edit()
   .putLong("at",System.currentTimeMillis())
   .putBoolean("passed",passed)
   .putInt("files",bounded(passed?verifiedFiles:0))
   .putInt("rides",bounded(rides))
   .putInt("photos",bounded(photos))
   .putInt("receipts",bounded(receipts))
   .commit();
 }
 private static int bounded(int n){return Math.max(0,Math.min(n,100_000));}
 static boolean failed(Context c){
  SharedPreferences p=c.getSharedPreferences(PREFS,0);
  return p.getLong("at",0)>0&&!p.getBoolean("passed",false);
 }
 static String summary(Context c){
  SharedPreferences p=c.getSharedPreferences(PREFS,0);
  long time=p.getLong("at",0);
  if(time<=0||time>System.currentTimeMillis())
   return "마지막 전체 복원 검사: 이 기기에 저장된 결과 없음";
  String stamp=new SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.KOREA).format(new Date(time));
  if(!p.getBoolean("passed",false))
   return "마지막 전체 복원 "+stamp+" · 내용 재검증 실패 · 저장된 GPS/사진 원본을 삭제하지 마세요.";
  return "마지막 전체 복원 "+stamp+" · 재검증 통과 · 파일 "+bounded(p.getInt("files",0))+
   "개 (GPS "+bounded(p.getInt("rides",0))+" · 사진 "+bounded(p.getInt("photos",0))+
   " · 영수증 "+bounded(p.getInt("receipts",0))+")";
 }
}
