package com.motohub.next;

import java.io.File;
import java.util.Locale;

/** GPS-only ride summary; never silently changes a vehicle odometer. */
public final class RideInsights {
 private RideInsights() {}
 public static final class Result {
  public final int points, discarded;
  public final double distanceKm, averageKmh, peakKmh;
  public final long startedAt, endedAt, durationMillis;
  Result(int count,int bad,double distance,double maximum,long start,long end){
   points=count;discarded=bad;distanceKm=distance;peakKmh=maximum;startedAt=start;endedAt=end;
   durationMillis=Math.max(0,end-start);averageKmh=durationMillis>0?distance/(durationMillis/3600000.0):0;
  }
  public String distanceLabel(){return String.format(Locale.KOREA,"%.1f km",distanceKm);}
 }
 /** Great-circle distance in metres. */
 public static double meters(double a,double b,double c,double d){
  double r1=Math.toRadians(a),r2=Math.toRadians(c),lat=Math.toRadians(c-a),lon=Math.toRadians(d-b);
  double h=Math.pow(Math.sin(lat/2),2)+Math.cos(r1)*Math.cos(r2)*Math.pow(Math.sin(lon/2),2);
  return 6371000.0*2*Math.atan2(Math.sqrt(Math.min(1,h)),Math.sqrt(Math.max(0,1-h)));
 }
 public static Result analyze(File track)throws Exception{
  final int[] count={0},bad={0};final long[] start={0},end={0},last={0};
  final double[] lat={0},lon={0},km={0},max={0};final boolean[] have={false};
  TrackFile.read(track,(x,y,accuracy,t)->{
   count[0]++;if(start[0]==0)start[0]=t;end[0]=t;
   if(accuracy>70){bad[0]++;return;}
   if(have[0]){
    long delta=t-last[0];double metres=meters(lat[0],lon[0],x,y);
    double speed=delta>0?metres*3600.0/delta:Double.POSITIVE_INFINITY; // km/h: metres / millis * 3600
    if(delta>600000 || speed>250 || (metres<Math.max(accuracy,5)&&delta<30000)){
     if(speed>250)bad[0]++;
     if(delta>600000){lat[0]=x;lon[0]=y;last[0]=t;}
     return;
    }
    km[0]+=metres/1000.0;max[0]=Math.max(max[0],speed);
   }
   lat[0]=x;lon[0]=y;last[0]=t;have[0]=true;
  });
  return new Result(count[0],bad[0],km[0],max[0],start[0],end[0]);
 }
}
