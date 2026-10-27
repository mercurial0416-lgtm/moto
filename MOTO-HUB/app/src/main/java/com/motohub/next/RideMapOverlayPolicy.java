package com.motohub.next;
final class RideMapOverlayPolicy{
 private RideMapOverlayPolicy(){}
 static boolean validPeak(double speed,double lat,double lon){
  return Double.isFinite(speed)&&speed>0&&speed<=250&&
   Double.isFinite(lat)&&Double.isFinite(lon)&&lat>=-90&&lat<=90&&lon>=-180&&lon<=180;
 }
}
