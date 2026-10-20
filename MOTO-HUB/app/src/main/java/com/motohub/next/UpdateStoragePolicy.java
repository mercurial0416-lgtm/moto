package com.motohub.next;
/** Keep both downloaded and installer-copied APK plus an additional cache safety margin. */
final class UpdateStoragePolicy {
 static final long RESERVE=32L*1024*1024;
 private UpdateStoragePolicy(){}
 static long required(long apkSize){
  if(apkSize<=0||apkSize>200_000_000L)throw new IllegalArgumentException("잘못된 업데이트 크기");
  return apkSize*2+RESERVE;
 }
 static boolean fits(long apkSize,long usableBytes){
  return usableBytes>=required(apkSize);
 }
}
