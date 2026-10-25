package com.motohub.next;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

/** Optional on-demand 15-minute ride breakdown; no cloud calls or location export. */
final class RidePaceSegments {
 static final long BIN_MS=900_000L;
 static final int MAX_BINS=96;
 static final int DISPLAY_BINS=12;
 private RidePaceSegments(){}
 static final class Bucket {
  double distanceKm;
  long observedMs,movingMs;
 }
 static int bucketIndex(long sinceStartMs){
  if(sinceStartMs<0)return -1;
  long value=sinceStartMs/BIN_MS;
  return value>=MAX_BINS?-1:(int)value;
 }
 static String describe(int index,Bucket b){
  if(index<0||b==null)return "구간 없음";
  long from=index*15L,to=from+15;
  return String.format(Locale.KOREA,"%d~%d분 · %.1f km · GPS 관측 %d분 · 이동 추정 %d분",
   from,to,b.distanceKm,b.observedMs/60_000,b.movingMs/60_000);
 }
 static String analyze(File track)throws Exception{
  if(track==null||!track.isFile())return "분석할 GPS 파일이 없어요.";
  ArrayList<Bucket> bins=new ArrayList<>();
  final long[] started={-1},priorAt={0};
  final double[] previousLat={0},previousLon={0},previousAccuracy={0};
  final boolean[] goodPrevious={false};
  final int[] beyond={0};
  TrackFile.read(track,(lat,lon,accuracy,time)->{
   if(started[0]<0)started[0]=time;
   int index=bucketIndex(time-started[0]);
   if(index<0){beyond[0]++;return;}
   while(bins.size()<=index)bins.add(new Bucket());
   Bucket bucket=bins.get(index);
   boolean good=accuracy<=40;
   if(good&&goodPrevious[0]){
    long elapsed=time-priorAt[0];
    if(elapsed>0&&elapsed<=RideMotionGate.MAX_CONTINUOUS_FIX_MS){
     double metres=RideInsights.meters(previousLat[0],previousLon[0],lat,lon);
     if(RideMotionGate.countDistance(elapsed,metres,previousAccuracy[0],accuracy)){
      bucket.distanceKm+=metres/1000.0;
      if(metres*3600.0/elapsed>=3)bucket.movingMs+=elapsed;
      bucket.observedMs+=elapsed;
     }else {
      double noiseThreshold=Math.max(4.0,Math.min(12.0,(previousAccuracy[0]+accuracy)*0.4));
      if(metres<noiseThreshold)bucket.observedMs+=elapsed;
     }
    }
   }
   goodPrevious[0]=good;
   if(good){priorAt[0]=time;previousLat[0]=lat;previousLon[0]=lon;previousAccuracy[0]=accuracy;}
  });
  if(bins.isEmpty())return "분석할 정상 GPS 좌표가 없어요.";
  StringBuilder summary=new StringBuilder();
  summary.append("GPS 원본의 15분 구간 분석 · 추정값\\n");
  int count=0;for(int i=0;i<bins.size()&&count<DISPLAY_BINS;i++){
   Bucket b=bins.get(i);
   if(b.observedMs==0&&b.distanceKm==0)continue;
   if(count++>0)summary.append("\\n");
   summary.append(describe(i,b));
  }
  if(count==0)summary.append("구간별 유효한 GPS 이동·시간 관측이 없어요.");
  if(bins.size()>DISPLAY_BINS)summary.append("\\n긴 경로는 처음 12구간까지만 표시");
  if(beyond[0]>0)summary.append("\\n24시간 이후 좌표 제외: ").append(beyond[0]).append("개");
  summary.append("\\n\\nGPS 미수신 시간은 정차로 계산하지 않아요. 정확도 40m 초과, 30초 초과 공백 및 비정상 속도는 분석에서 제외합니다.");
  return summary.toString().replace("\\n","\n");
 }
}
