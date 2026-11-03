package com.motohub.next;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Session-only, bounded cache for read-only GPS distance charts and filters.
 * Strong SHA-256 revalidation prevents stale hits after same-size/same-mtime rewrites.
 * This is deliberately NEVER evidence for odometer edits, GPS backups, exports,
 * track deletion, or signed-update integrity checks.
 */
final class RideDistanceCache {
 static final long MAX_ANALYSIS_BYTES=TrackFile.MAX_BYTES;
 private static final int MAX_ENTRIES=4096;
 static final class Summary {
  final double distanceKm;
  final long durationMillis,movingMillis;
  Summary(double distanceKm,long durationMillis,long movingMillis){
   this.distanceKm=distanceKm;this.durationMillis=durationMillis;
   this.movingMillis=movingMillis;
  }
 }
 private static final class CacheEntry {
  final long size,modified;
  final String sha256;
  final Summary value;
  CacheEntry(long size,long modified,String sha256,Summary value){
   this.size=size;this.modified=modified;this.sha256=sha256;this.value=value;
  }
 }
 private static final LinkedHashMap<String,CacheEntry> CACHE=
  new LinkedHashMap<String,CacheEntry>(64,.75f,true){
   @Override protected boolean removeEldestEntry(Map.Entry<String,CacheEntry> eldest){
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
  // mtime and length can be preserved by a same-size replacement or a rewrite.
  // Hash every prospective cache hit on this worker thread, never on the UI.
  // Unlike a GPS replay, streaming SHA-256 doesn't parse every location point.
  String before=RideMileageIntegrity.fingerprint(track);
  if(!RideMileageIntegrity.validCompleted(track)||
     track.length()!=size||track.lastModified()!=modified)
   throw new IOException("GPS 확인 중 원본 파일이 변경됨");
  synchronized(CACHE){
   CacheEntry cached=CACHE.get(path);
   if(cached!=null&&cached.size==size&&cached.modified==modified&&
      cached.sha256.equals(before)){
    hits++;
    return cached.value;
   }
   // A stale value must not be reused if it has the same size/mtime.
   CACHE.remove(path);
  }
  RideInsights.Result details=RideInsights.analyze(track);
  String after=RideMileageIntegrity.fingerprint(track);
  if(!before.equals(after)||!RideMileageIntegrity.validCompleted(track)||
     track.length()!=size||track.lastModified()!=modified)
   throw new IOException("GPS 분석 중 원본 파일이 변경됨");
  Summary value=new Summary(details.distanceKm,details.durationMillis,details.movingMillis);
  synchronized(CACHE){
   CACHE.put(path,new CacheEntry(size,modified,after,value));
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
