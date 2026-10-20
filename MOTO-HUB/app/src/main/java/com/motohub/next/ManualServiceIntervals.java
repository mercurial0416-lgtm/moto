package com.motohub.next;
/** Manually entered intervals. Never claim these are official manufacturer schedules. */
final class ManualServiceIntervals {
 private ManualServiceIntervals(){}
 static final int MIN_KM=100,MAX_KM=100_000;
 static int parse(String input){
  if(input==null||!input.matches("[0-9]{1,6}"))throw new IllegalArgumentException("정비 간격을 km 단위 숫자로 입력해 주세요.");
  int km=Integer.parseInt(input);
  if(km<MIN_KM||km>MAX_KM)throw new IllegalArgumentException("정비 간격은 100~100,000 km로 입력해 주세요.");
  return km;
 }
 static long next(double odo,int interval){
  if(!Double.isFinite(odo)||odo<0||odo>10_000_000||interval<MIN_KM||interval>MAX_KM)throw new IllegalArgumentException("계기판·정비 간격을 확인해 주세요.");
  return (long)Math.ceil(odo+interval);
 }
}
