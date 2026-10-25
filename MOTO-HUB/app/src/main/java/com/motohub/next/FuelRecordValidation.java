package com.motohub.next;
/** Validate actual refueling without affecting non-fuel expenses. */
final class FuelRecordValidation {
 private FuelRecordValidation(){}
 static void requireFuel(double amount,double liters){
  if(!Double.isFinite(amount)||amount<=0||amount>1_000_000_000)throw new IllegalArgumentException("주유 금액은 0원보다 커야 합니다.");
  if(!Double.isFinite(liters)||liters<=0||liters>1000)throw new IllegalArgumentException("주유량을 0보다 큰 리터 단위로 입력해 주세요.");
 }
}
