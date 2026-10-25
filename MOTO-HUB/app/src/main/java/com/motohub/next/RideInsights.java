package com.motohub.next;

import java.io.File;
import java.util.Locale;

/** GPS-only ride summary; never silently changes a vehicle odometer. */
public final class RideInsights {
 private RideInsights() {}
 public static final class Result {
  public final int points, discarded;
  public final double distanceKm, averageKmh, movingAverageKmh, peakKmh;
  public final long startedAt, endedAt, durationMillis, movingMillis, gpsObservedMillis;
  Result(int count,int bad,double distance,double maximum,long start,long end,long moving){
   this(count,bad,distance,maximum,start,end,moving,0);
  }
  Result(int count,int bad,double distance,double maximum,long start,long end,long moving,long observed){
   points=count;discarded=bad;distanceKm=distance;peakKmh=maximum;startedAt=start;endedAt=end;
   durationMillis=Math.max(0,end-start);movingMillis=Math.max(0,Math.min(moving,durationMillis));
   gpsObservedMillis=Math.max(0,Math.min(observed,durationMillis));
   averageKmh=durationMillis>0?distance/(durationMillis/3600000.0):0;
   movingAverageKmh=movingMillis>0?Math.min(250,distance/(movingMillis/3600000.0)):0;
  }
  public int gpsCoveragePercent(){return durationMillis<=0?0:(int)Math.round(gpsObservedMillis*100.0/durationMillis);}
  public String distanceLabel(){return String.format(Locale.KOREA,"%.1f km",distanceKm);}
 }
 /** Great-circle distance in metres. */
 public static double meters(double a,double b,double c,double d){
  double r1=Math.toRadians(a),r2=Math.toRadians(c),lat=Math.toRadians(c-a),lon=Math.toRadians(d-b);
  double h=Math.pow(Math.sin(lat/2),2)+Math.cos(r1)*Math.cos(r2)*Math.pow(Math.sin(lon/2),2);
  return 6371000.0*2*Math.atan2(Math.sqrt(Math.min(1,h)),Math.sqrt(Math.max(0,1-h)));
 }
 public static Result analyze(File track)throws Exception{
  final int[] count={0},bad={0};final long[] start={0},end={0},last={0},lastObserved={0},observed={0};
  final double[] lat={0},lon={0},previousAccuracy={0},km={0},max={0};final long[] moving={0};final boolean[] have={false};
  TrackFile.read(track,(x,y,accuracy,t)->{
   count[0]++;if(start[0]==0)start[0]=t;end[0]=t;
   if(accuracy>40){bad[0]++;have[0]=false;lastObserved[0]=0;return;}
   if(lastObserved[0]>0){long gap=t-lastObserved[0];
    if(gap>0&&gap<=RideMotionGate.MAX_CONTINUOUS_FIX_MS)observed[0]+=gap;
   }
   lastObserved[0]=t;
   if(have[0]){
    long delta=t-last[0];double metres=meters(lat[0],lon[0],x,y);
    if(delta>0&&delta<=RideMotionGate.MAX_CONTINUOUS_FIX_MS &&
       metres<Math.max(Math.max(previousAccuracy[0],accuracy),5)){
     // Ignore GPS jitter without adding distance; retain prior point for a stable reference.
     return;
    }
    if(RideMotionGate.countDistance(delta,metres,previousAccuracy[0],accuracy)){
     double speed=metres*3600.0/delta;
     km[0]+=metres/1000.0;max[0]=Math.max(max[0],speed);
     if(speed>=3)moving[0]+=delta;
    }else if(delta<=RideMotionGate.MAX_CONTINUOUS_FIX_MS){
     bad[0]++; // Discard impossible spikes without poisoning the last reliable location.
     return;
    }
    // On a long GPS outage, restart measurement here; never bridge the missing segment.
   }
   lat[0]=x;lon[0]=y;last[0]=t;previousAccuracy[0]=accuracy;have[0]=true;
  });
  return new Result(count[0],bad[0],km[0],max[0],start[0],end[0],moving[0],observed[0]);
 }
}
