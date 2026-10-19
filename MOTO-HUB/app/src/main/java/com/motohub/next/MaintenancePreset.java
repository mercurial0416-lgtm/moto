package com.motohub.next;
/** Non-authoritative examples: rider must opt in to applying a suggested interval. */
final class MaintenancePreset {
 private MaintenancePreset(){}
 static final String[] LABELS={"+1,000 km","+3,000 km","+5,000 km","+10,000 km","사용 안 함"};
 static double next(double odometer,int choice){
  if(!Double.isFinite(odometer)||odometer<0||odometer>10_000_000||choice<0||choice>=LABELS.length)
   throw new IllegalArgumentException("계기판 값 또는 정비 간격이 올바르지 않습니다.");
  long[] gap={1000,3000,5000,10000,0};return choice==4?0:odometer+gap[choice];
 }
}
