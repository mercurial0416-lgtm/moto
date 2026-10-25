package com.motohub.next;

/** Conservative advisory indications, never manufacturer fault-code claims. */
final class ObdGaugeWarnings {
 private ObdGaugeWarnings(){}
 static String advisory(double rpm,double coolant,double voltage){
  if(Double.isFinite(coolant)&&coolant>=110)return "냉각수 온도 높음 의심 · 순정 계기판 확인";
  // Charging checks require confirmed running RPM; OFF battery voltage alone is not a defect.
  if(Double.isFinite(rpm)&&rpm>=400&&Double.isFinite(voltage)){
   if(voltage<12.0)return "주행 중 전압 낮음 의심 · 배터리와 충전 상태 확인";
   if(voltage>15.5)return "전압 높음 의심 · 충전 상태 점검";
  }
  return "참고용 표시 · 수온/전압 경고 기준은 순정 경고등을 우선하세요";
 }
 static int rpmBar(double rpm){
  if(!Double.isFinite(rpm)||rpm<0)return 0;
  return (int)Math.max(0,Math.min(15000,Math.round(rpm)));
 }
}
