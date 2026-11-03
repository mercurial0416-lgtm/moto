package com.motohub.next;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Session-only, bounded cache for read-only GPS distance charts and filters.
 * This is deliberately NEVER evidence for odometer edits, GPS backups, exports,
 * track deletion, or signed-update integrity checks.
 */
final class RideDistanceCache {
 static final long MAX_ANALYSIS_BYTES=5_000_000L;
 private static final int MAX_ENTRIES=4096;
 static final class Summary {
  final double distanceKm;
  final long durationMillis;
  Summary(double distanceKm,long durationMillis){
   this.distanceKm=distanceKm;this.durationMillis=durationMillis;
  }
 }
 private static final class Entry {
  final long size,modified;
  final Summary value;
  Entry(long size,long modified,Summary value){
   this.size=size;this.modified=modified;this.value=value;
  }
 }
 private static final LinkedHashMap<String,Entry> CACHE=
  new LinkedHashMap<String,Entry>(64,.75f,true){
   @Override protected boolean removeEldestEntry(Map.Entry<String,Entry> eldest){
    return size()>MAX_ENTRIES;
   }
  };
 private static long analyses=0,hits=0;
 private RideDistanceCache(){}

 static Summary read(File track)throws Exception{
  if(!RideMileageIntegrity.validCompleted(track))
   throw new IOException("검증되지 않은 완료 GPS 기록");
  long size=track.length(),modified=track.lastModified();
  if(size<=0||size>MAX_ANALYSIS_BYTES)throw new IOException("GPS 분석 크기 제한");
  String path=track.getCanonicalPath();
  synchronized(CACHE){
   Entry cached=CACHE.get(path);
   if(cached!=null&&cached.size==size&&cached.modified==modified){
    hits++;
    return cached.value;
   }
  }
  RideInsights.Result details=RideInsights.analyze(track);
  if(!RideMileageIntegrity.validCompleted(track)||
     track.length()!=size||track.lastModified()!=modified)
   throw new IOException("GPS 분석 중 원본 파일이 변경됨");
  Summary value=new Summary(details.distanceKm,details.durationMillis);
  synchronized(CACHE){
   CACHE.put(path,new Entry(size,modified,value));
   analyses++;
  }
  return value;
 }
 /** Clear only derived display values, never raw GPS or stored ride data. */
 static void clear(){
  synchronized(CACHE){CACHE.clear();}
 }
 static long analysesForTest(){synchronized(CACHE){return analyses;}}
 static long hitsForTest(){synchronized(CACHE){return hits;}}
}
