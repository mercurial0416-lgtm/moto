package com.motohub.next;

public final class BwImprovementsTest {
 private static int passed=0;
 private static void eq(String label,String actual,String wanted){
  if(!wanted.equals(actual))throw new AssertionError(label+" expected="+wanted+" actual="+actual);
  System.out.println("PASS " + label);passed++;
 }
 public static void main(String[] args){
  eq("fuel unit price",FuelEntryMath.unitPrice("10000","5"),"리터당 약 2,000원");
  eq("empty liters",FuelEntryMath.unitPrice("10000","0"),"금액·주유량을 입력하면 리터당 가격을 표시합니다");
  eq("bad fuel",FuelEntryMath.unitPrice("10000","oops"),"금액·주유량을 입력하면 리터당 가격을 표시합니다");
  eq("negative fuel",FuelEntryMath.unitPrice("-1","3"),"금액·주유량을 입력하면 리터당 가격을 표시합니다");
  eq("infinite fuel",FuelEntryMath.unitPrice("Infinity","3"),"금액·주유량을 입력하면 리터당 가격을 표시합니다");
  eq("ride average",RideMetrics.averageText(12,1800),"24.0 km/h");
  eq("ride not started",RideMetrics.averageText(0,0),"—");
  eq("ride bad distance",RideMetrics.averageText(Double.NaN,100),"—");
  eq("ride unrealistic display bounded",RideMetrics.averageText(1000,1),"250.0 km/h");
  eq("cost no prior history",CostTrend.caption(50000,0),"지난달 지출 0원");
  eq("cost no expenses",CostTrend.caption(0,0),"변동 없음");
  eq("cost growth",CostTrend.caption(150,100),"증가 50%");
  eq("cost reduction",CostTrend.caption(50,100),"감소 50%");
  eq("cost invalid",CostTrend.caption(-1,100),"비용 확인 필요");
  System.out.println("TOTAL "+passed+" improvements passed");
 }
}
