package com.motohub.next;
/** Explicit, manual dashboard-to-garage ODO confirmation; never derived from speed or GPS. */
final class OdometerSyncPolicy {
 static double checkedManual(String raw,double previous){
  if(raw==null||!raw.trim().matches("[0-9]{1,7}(?:[.][0-9])?"))throw new IllegalArgumentException("실제 계기판 주행거리를 숫자로 입력해 주세요 (km).");
  double odo=Double.parseDouble(raw.trim());
  if(!Double.isFinite(previous)||previous<0||previous>2_000_000)throw new IllegalArgumentException("기존 주행거리 값을 확인해 주세요.");
  if(!Double.isFinite(odo)||odo<0||odo>2_000_000)throw new IllegalArgumentException("주행거리 범위를 확인해 주세요.");
  if(odo<previous)throw new IllegalArgumentException("계기판 값이 기존 기록보다 작습니다. 정정은 바이크 정보 수정에서 직접 확인해 주세요.");
  return odo;
 }
 private OdometerSyncPolicy(){}
}
