package com.motohub.next;

/** Explicit, bounded Cardo GPS motion safety net for missing ECU speed. */
final class CardoGpsFallbackGate {
 private int segments;
 private double metres;
 private long spanMs;
 void reset(){segments=0;metres=0;spanMs=0;}
 boolean accept(long gapMs,double distanceMetres,double previousAccuracy,
                double accuracy,boolean obdSpeedFresh){
  if(obdSpeedFresh||gapMs<1500||gapMs>12000||
     !Double.isFinite(distanceMetres)||!Double.isFinite(previousAccuracy)||
     !Double.isFinite(accuracy)||previousAccuracy<0||accuracy<0||
     previousAccuracy>20||accuracy>20||distanceMetres<10){
   reset();return false;
  }
  double kmh=distanceMetres*3600.0/gapMs;
  if(kmh<8||kmh>160){reset();return false;}
  // Low-speed launches need longer, GPS-accurate evidence so a walking phone
  // or a stationary jitter cluster cannot easily trigger a motorcycle ride.
  if(kmh<12&&(previousAccuracy>10||accuracy>10)){reset();return false;}
  segments++;metres+=distanceMetres;spanMs+=gapMs;
  boolean lowSpeed=kmh<12;
  if(segments>=(lowSpeed?5:3)&&metres>=(lowSpeed?95:70)&&
     spanMs>=(lowSpeed?30_000:10_000)){reset();return true;}
  return false;
 }
}