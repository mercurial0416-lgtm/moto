package com.motohub.next;
/** Tiny text-based offline chart; each bar uses the SAME reference scale. */
final class RideReportBars {
 private RideReportBars(){}
 static String render(double value,double max){
  if(!Double.isFinite(value)||!Double.isFinite(max)||value<0||max<0||value>max)return "비교 불가";
  int length=max==0?0:(int)Math.round(12*value/max);
  StringBuilder b=new StringBuilder();for(int i=0;i<12;i++)b.append(i<length?'■':'·');
  return b.toString();
 }
}
