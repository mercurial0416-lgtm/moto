package com.motohub.next;

import org.json.JSONObject;
import java.time.LocalDate;

/** Offline quick-entry writes exactly the same v3 records as the full editor. */
final class QuickRecordDraft {
 private QuickRecordDraft(){}
 static JSONObject fuel(String vehicleId,String date,double mileage,double amount,double liters,boolean full)throws Exception{
  requireVehicle(vehicleId);Store.validateDate(date);
  if(date.isEmpty())throw new IllegalArgumentException("주유 날짜를 확인해 주세요.");
  if(!Double.isFinite(mileage)||mileage<0||mileage>1e9)throw new IllegalArgumentException("계기판 주행거리를 확인해 주세요.");
  FuelRecordValidation.requireFuel(amount,liters);
  return new JSONObject().put("id",Store.id()).put("type","주유").put("vehicleId",vehicleId)
   .put("date",date).put("mileage",mileage).put("amount",amount)
   .put("liters",liters).put("fullTank",full).put("note","");
 }
 static JSONObject maintenance(String vehicleId,String type,String date,double mileage,double nextKm,String nextDate,
                                int intervalKm,int intervalMonths)throws Exception{
  requireVehicle(vehicleId);
  type=type==null?"":type.trim();
  if(type.isEmpty()||type.length()>100)throw new IllegalArgumentException("정비 항목을 선택해 주세요.");
  Store.validateDate(date);Store.validateDate(nextDate);
  if(date.isEmpty())throw new IllegalArgumentException("등록 날짜를 확인해 주세요.");
  if(!Double.isFinite(mileage)||mileage<0||mileage>1e9)throw new IllegalArgumentException("현재 주행거리를 확인해 주세요.");
  if(!Double.isFinite(nextKm)||nextKm<0||nextKm>1e9)throw new IllegalArgumentException("다음 정비 거리를 확인해 주세요.");
  if(intervalKm<0||intervalMonths<0||intervalMonths>120)throw new IllegalArgumentException("정비 간격이 올바르지 않습니다.");
  if(nextKm<=0&&nextDate.isEmpty())throw new IllegalArgumentException("거리 또는 날짜 주기를 하나 이상 선택해 주세요.");
  if(nextKm>0&&nextKm<=mileage)throw new IllegalArgumentException("다음 정비 거리는 현재 주행거리보다 커야 해요.");
  if(!nextDate.isEmpty()&&LocalDate.parse(nextDate).isBefore(LocalDate.parse(date)))
   throw new IllegalArgumentException("다음 정비일은 등록일 이후여야 해요.");
  return new JSONObject().put("id",Store.id()).put("type",type).put("vehicleId",vehicleId)
    .put("date",date).put("mileage",mileage).put("cost",0)
    .put("nextKm",nextKm).put("nextDate",nextDate)
    .put("intervalKm",intervalKm).put("intervalMonths",intervalMonths)
    .put("note","정비 예정 주기 등록 · 비용 발생 없음").put("scheduleOnly",true);
 }
 static double nextOdometer(double current,int interval){
  if(!Double.isFinite(current)||current<0||current>1e9||interval<0||interval>100000)return -1;
  return interval==0?0:Math.ceil(current)+interval;
 }
 static String dueAfterMonths(String today,int months){
  if(months<0||months>120)throw new IllegalArgumentException("정비 월 주기가 올바르지 않습니다.");
  return months==0?"":LocalDate.parse(today).plusMonths(months).toString();
 }
 private static void requireVehicle(String id){
  if(id==null||id.trim().isEmpty())throw new IllegalArgumentException("바이크를 먼저 선택해 주세요.");
 }
}
