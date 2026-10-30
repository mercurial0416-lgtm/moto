package com.motohub.next;

import android.os.StatFs;
import java.io.File;
import java.io.IOException;

/** Do not open a new GPS track if internal storage cannot hold the maximum track and recovery margin. */
final class GpsStorageSpacePolicy {
 private GpsStorageSpacePolicy(){}
 static final long MIN_FREE_BYTES=48L*1024L*1024L; // 25 MiB track cap + Android/sidecar margin.
 static boolean sufficient(long available){return available>=MIN_FREE_BYTES;}
 static String message(long available){
  return available<0?"GPS 저장공간을 확인할 수 없습니다. 저장공간과 권한을 확인해 주세요.":
   "GPS 저장공간 부족 · 여유 "+(available/(1024L*1024L))+
   "MB · 안전한 기록 시작에는 48MB 이상이 필요합니다. 저장공간을 확보해 주세요.";
 }
 static void require(File filesDir)throws IOException{
  if(filesDir==null||!filesDir.isDirectory())throw new IOException(message(-1));
  long bytes=-1;
  // Some Android device/OS implementations may not provide usable StatFs data.
  // Fallback uses the same internal-files partition and still fails closed if unknown.
  try{bytes=new StatFs(filesDir.getAbsolutePath()).getAvailableBytes();}
  catch(RuntimeException ignored){}
  if(bytes<=0)bytes=filesDir.getUsableSpace();
  if(!sufficient(bytes))throw new IOException(message(bytes));
 }
}
