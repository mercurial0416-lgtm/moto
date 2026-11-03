package com.motohub.next;

import java.util.Locale;

/** Explicit, same-period comparison of entered costs against recorded GPS kilometres. */
final class RideCostPerKm {
 private RideCostPerKm(){}
 static String caption(String bikeId,double sixMonthWon,double recordedGpsKm,int unscanned) {
  if(bikeId==null||bikeId.isEmpty())
   return "유지비 비교는 바이크를 선택하면 표시됩니다.";
  if(!Double.isFinite(sixMonthWon)||sixMonthWon<0||!Double.isFinite(recordedGpsKm)||recordedGpsKm<0)
   return "입력 비용 또는 GPS 기록을 확인할 수 없습니다.";
  if(unscanned>0)
   return "GPS 분석 건수 제한으로 비용/km 비교를 생략합니다.";
  if(recordedGpsKm<1)
   return "비용/km 비교는 최근 6개월 GPS 주행 기록이 1km 이상일 때 표시합니다.";
  if(sixMonthWon<=0)
   return "최근 6개월 입력한 주유·정비·기타 비용이 없어 비용/km를 표시하지 않습니다.";
  double rate=sixMonthWon/recordedGpsKm;
  if(!Double.isFinite(rate)||rate>1_000_000_000) return "비용/km 비교 범위를 초과했습니다.";
  return String.format(Locale.KOREA,
   "참고: 입력비용 %,.0f원 ÷ 기록된 GPS %,.1fkm = 약 %,.0f원/GPS km\n"+
   "GPS 기록이 없는 운행은 제외됩니다. 실제 km당 유지비·연비로 해석하지 마세요.",
   sixMonthWon,recordedGpsKm,rate);
 }
}
