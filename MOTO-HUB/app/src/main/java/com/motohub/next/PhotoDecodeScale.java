package com.motohub.next;

/** Compute a bounded, power-of-two BitmapFactory sample size without Android dependencies. */
final class PhotoDecodeScale {
 private PhotoDecodeScale() {}
 static int sampleSize(int width,int height,int maxDimension){
  if(width<=0||height<=0||maxDimension<=0)throw new IllegalArgumentException("invalid bitmap dimensions");
  int sample=1;
  while(width/sample>maxDimension||height/sample>maxDimension){
   if(sample>=(1<<29))return sample;
   sample*=2;
  }
  return sample;
 }
}
