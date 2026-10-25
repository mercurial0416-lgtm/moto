package com.motohub.next;
/** User-facing monthly cost comparison; never treats missing prior data as percentage growth. */
final class CostTrend {
 private CostTrend(){}
 static String caption(double current,double previous){
  if(!Double.isFinite(current)||!Double.isFinite(previous)||current<0||previous<0)return "비용 확인 필요";
  if(previous==0)return current==0?"변동 없음":"지난달 지출 0원";
  double diff=current-previous;
  if(Math.abs(diff)<0.5)return "변동 없음";
  double pct=Math.abs(diff)/previous*100;
  return (diff>0?"증가 ":"감소 ")+String.format(java.util.Locale.KOREA,"%.0f%%",pct);
 }
}
