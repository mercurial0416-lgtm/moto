package com.motohub.next;
import java.util.*;
/** Pure Java checkpoint BZ regression; no Android runtime assumptions. */
public final class BzImprovementsTest {
 private static int n=0;
 static void eq(Object a,Object b){n++;if(!Objects.equals(a,b))throw new AssertionError("Expected "+b+" but got "+a);}
 static void yes(boolean ok){n++;if(!ok)throw new AssertionError("assertion "+n+" failed");}
 public static void main(String[] args){
  eq(ServiceDueSummary.caption("2026-10-20",12500,"2026-10-18",10000),"D-2 · 2,500 km 남음");
  eq(ServiceDueSummary.caption("2026-10-16",10000,"2026-10-18",12000),"2일 경과 · 2,000 km 초과");
  eq(ServiceDueSummary.caption("2026-10-18",0,"2026-10-18",20000),"오늘");
  eq(ServiceDueSummary.caption("bad",Double.NaN,"2026-10-18",Double.NaN),"미설정");
  eq(ServiceDueSummary.caption("",5000,"2026-10-18",4700),"300 km 남음");
  eq(ServiceDueSummary.caption("2026-10-18",0,"2026-10-18",0),"오늘");
  eq(ServiceDueSummary.nearest(Arrays.asList("2026-10-21","2026-10-20"),Arrays.asList(14000.0,12500.0),"2026-10-18",10000),"D-2 / 2,500 km");
  eq(ServiceDueSummary.nearest(Arrays.asList("invalid"),Arrays.asList(Double.NaN),"2026-10-18",10000),"미설정");
  eq(ServiceDueSummary.nearest(Arrays.asList("2026-10-18"),Arrays.asList(-10.0),"2026-10-18",10000),"오늘");
  eq(ServiceDueSummary.nearest(new ArrayList<>(),Arrays.asList(10000.0),"2026-10-18",10000),"0 km 초과");
  eq(RideTrend.compare(30,20),"이전 30일 대비 +50%");
  eq(RideTrend.compare(10,20),"이전 30일 대비 -50%");
  eq(RideTrend.compare(0,0),"이전 30일 대비 동일");
  eq(RideTrend.compare(5,0),"이전 30일 기록 없음");
  eq(RideTrend.compare(Double.NaN,0),"거리 비교 불가");
  eq(RideTrend.compare(-1,15),"거리 비교 불가");
  yes(RideTrend.compare(0,20).endsWith("-100%"));
  System.out.println("PASS: "+n+" BZ pure-Java assertions");
 }
}
