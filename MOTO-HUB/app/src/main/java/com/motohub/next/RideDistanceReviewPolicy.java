package com.motohub.next;

/** Advisory-only GPS trip distance; never mutates a bike's odometer. */
final class RideDistanceReviewPolicy {
 private RideDistanceReviewPolicy(){}
 static String suggestion(double distanceKm,boolean verified){
  if(!verified)return "GPS 기록 검증 전 · 누적거리 반영하지 않음";
  if(!Double.isFinite(distanceKm)||distanceKm<=0||distanceKm>1500)
   return "GPS 거리 이상 또는 미측정 · 수동 확인 필요";
  return String.format(java.util.Locale.KOREA,
   "GPS 주행 %.1f km · 정비 주기 확인 시 참고하세요. 계기판 누적거리는 자동 변경하지 않습니다.",distanceKm);
 }
 static boolean plausible(double distanceKm,boolean verified){
  return verified&&Double.isFinite(distanceKm)&&distanceKm>0&&distanceKm<=1500;
 }
}
