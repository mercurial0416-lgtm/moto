package com.motohub.next;
/** Preserve reliable real GPS corners when reducing map detail. */
final class MapPathDetailPolicy {
 private MapPathDetailPolicy(){}
 static boolean corner(double aLat,double aLon,double bLat,double bLon,
                       double cLat,double cLon,double accuracy){
  if(!Double.isFinite(accuracy)||accuracy>20||accuracy<0)return false;
  double x1=(bLon-aLon)*Math.cos(Math.toRadians(bLat))*111_000;
  double y1=(bLat-aLat)*111_000;
  double x2=(cLon-bLon)*Math.cos(Math.toRadians(cLat))*111_000;
  double y2=(cLat-bLat)*111_000;
  double n1=Math.hypot(x1,y1),n2=Math.hypot(x2,y2);
  if(n1<8||n2<8)return false;
  double dot=(x1*x2+y1*y2)/(n1*n2);
  return dot<Math.cos(Math.toRadians(35));
 }
}
