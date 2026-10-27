package com.motohub.next;

/** Route links must not bridge a GPS outage or draw impossible teleport lines.
 * Bad fixes remain untouched in the archived GPS record.
 */
final class MapPathContinuity {
 private MapPathContinuity(){}
 static boolean validFix(double lat,double lon,double accuracy){
  return Double.isFinite(lat)&&Double.isFinite(lon)&&Double.isFinite(accuracy)&&
   lat>=-90&&lat<=90&&lon>=-180&&lon<=180&&accuracy>=0&&accuracy<=40;
 }
 static boolean validLink(double lat1,double lon1,double accuracy1,long time1,
                          double lat2,double lon2,double accuracy2,long time2){
  if(!validFix(lat1,lon1,accuracy1)||!validFix(lat2,lon2,accuracy2))return false;
  long dt=time2-time1;
  if(dt<=0||dt>RideMotionGate.MAX_CONTINUOUS_FIX_MS)return false;
  double metres=RideInsights.meters(lat1,lon1,lat2,lon2);
  return Double.isFinite(metres)&&metres*3600.0/dt<=250;
 }
}
