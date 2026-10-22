package com.motohub.next;

import org.json.JSONObject;
import java.time.LocalDate;

/** Explicit, repeatable service cycle: a completed repair becomes a real record, not a fake paid reminder. */
final class ServiceCycle {
 private ServiceCycle(){}
 static JSONObject completed(JSONObject previous,String date,double currentKm,double cost)throws Exception {
  if(previous==null||previous.optString("vehicleId").isEmpty()||previous.optString("type").isEmpty())
   throw new IllegalArgumentException("완료할 정비 항목이 없습니다.");
  try{Store.validateDate(date);}catch(java.time.format.DateTimeParseException invalid){throw new IllegalArgumentException("정비 완료 날짜가 올바르지 않습니다.");}
  if(date.isEmpty())throw new IllegalArgumentException("정비 완료 날짜를 입력해 주세요.");
  if(!Double.isFinite(currentKm)||currentKm<0||currentKm>1e9)throw new IllegalArgumentException("계기판을 확인해 주세요.");
  if(!Double.isFinite(cost)||cost<0||cost>1_000_000_000)
   throw new IllegalArgumentException("정비 비용을 확인해 주세요.");
  int km=previous.optInt("intervalKm",0),months=previous.optInt("intervalMonths",0);
  if(km<0||km>100000||months<0||months>120)throw new IllegalArgumentException("정비 주기가 잘못되었습니다.");
  String nextDate=months>0?LocalDate.parse(date).plusMonths(months).toString():"";
  double nextKm=km>0?QuickRecordDraft.nextOdometer(currentKm,km):0;
  if(nextKm<0)throw new IllegalArgumentException("다음 정비 거리를 계산할 수 없습니다.");
  return new JSONObject().put("id",Store.id()).put("vehicleId",previous.optString("vehicleId"))
   .put("type",previous.optString("type")).put("date",date)
   .put("mileage",currentKm).put("cost",cost)
   .put("nextKm",nextKm).put("nextDate",nextDate)
   .put("intervalKm",km).put("intervalMonths",months)
   .put("alertKmBefore",previous.optInt("alertKmBefore",0))
   .put("alertDaysBefore",previous.optInt("alertDaysBefore",0))
   .put("scheduleOnly",false)
   .put("note","정비 완료 · 다음 주기 자동 계산");
 }
 static double calculatedFuelAmount(double liters,double unitPrice){
  if(!Double.isFinite(liters)||liters<=0||liters>1000
   ||!Double.isFinite(unitPrice)||unitPrice<=0||unitPrice>1_000_000)throw new IllegalArgumentException("리터당 가격과 주유량을 확인해 주세요.");
  double amount=Math.round(liters*unitPrice);
  if(amount<=0||amount>1_000_000_000)throw new IllegalArgumentException("계산된 주유 금액이 너무 큽니다.");
  return amount;
 }
}
