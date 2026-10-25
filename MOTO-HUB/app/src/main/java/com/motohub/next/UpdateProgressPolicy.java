package com.motohub.next;

/** Download progress is display-only. APK verification still uses exact byte counts and SHA-256. */
final class UpdateProgressPolicy {
 private UpdateProgressPolicy(){}

 static int percentage(long downloaded,long total){
  if(downloaded<=0||total<=0)return 0;
  if(downloaded>=total)return 100;
  return (int)Math.min(99,Math.floor(100.0*downloaded/total));
 }

 static String label(String action,int percent){
  if(percent<0)return action;
  return action+" · "+Math.max(0,Math.min(100,percent))+"%";
 }
}
