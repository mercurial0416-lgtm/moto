package com.motohub.next;
/** GPS freshness is not proof of parking. Requires sustained, accurate,
 * spatially bounded observations before auto-closing a GPS fallback ride. */
final class GpsStationaryEvidence {
 private long first=-1,last=-1;
 private double anchorLat,anchorLon,lastLat,lastLon,total;
 private int samples;
 synchronized void reset(){first=-1;last=-1;total=0;samples=0;}
 synchronized void accept(long time,double lat,double lon,double accuracy){
  if(time<0||!Double.isFinite(lat)||!Double.isFinite(lon)||
    !Double.isFinite(accuracy)||accuracy<0||accuracy>20||
    lat<-90||lat>90||lon<-180||lon>180){reset();return;}
  if(last<0||time<=last||time-last>15_000L){reset();begin(time,lat,lon);return;}
  double step=metres(lastLat,lastLon,lat,lon);
  double offset=metres(anchorLat,anchorLon,lat,lon);
  if(step>20||offset>28||total+step>90){reset();begin(time,lat,lon);return;}
  total+=step;last=time;lastLat=lat;lastLon=lon;samples++;
 }
 private void begin(long time,double lat,double lon){
  first=last=time;anchorLat=lastLat=lat;anchorLon=lastLon=lon;samples=1;
 }
 synchronized boolean confirmed(long now){
  return first>=0&&last>=first&&now>=last&&now-last<=15_000L&&
   last-first>=75_000L&&samples>=8&&total<=90;
 }
 static double metres(double a,double b,double c,double d){
  double x=Math.toRadians(c-a),y=Math.toRadians(d-b);
  double h=Math.sin(x/2)*Math.sin(x/2)+Math.cos(Math.toRadians(a))*
   Math.cos(Math.toRadians(c))*Math.sin(y/2)*Math.sin(y/2);
  return 6371000.0*2*Math.atan2(Math.sqrt(h),Math.sqrt(Math.max(0,1-h)));
 }
}
