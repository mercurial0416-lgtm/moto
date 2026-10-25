package com.motohub.next;

import java.util.List;

public final class WidgetMaintenanceSummaryTest {
 private static int checks;
 private static void eq(String expected,String actual) {
  checks++;
  if(!expected.equals(actual))throw new AssertionError("expected '"+expected+"', got '"+actual+"'");
 }
 private static WidgetMaintenanceSummary.Service s(String type,String date,String nextDate,double nextKm) {
  return new WidgetMaintenanceSummary.Service(type,date,nextDate,nextKm);
 }
 public static void main(String[] args) {
  String today="2026-10-18";
  eq("정비 일정 없음",WidgetMaintenanceSummary.label(List.of(),today,4000));
  eq("정비 일정 없음",WidgetMaintenanceSummary.label(List.of(s("엔진오일","2026-10-01","",0)),today,4000));
  eq("정비 정상",WidgetMaintenanceSummary.label(List.of(s("엔진오일","2026-10-01","2027-01-01",9000)),today,4000));
  eq("곧 점검 1건",WidgetMaintenanceSummary.label(List.of(s("엔진오일","2026-10-01","2026-11-01",9000)),today,4000));
  eq("점검 필요 1건",WidgetMaintenanceSummary.label(List.of(s("엔진오일","2026-10-01","",4000)),today,4000));
  eq("점검 필요 2건",WidgetMaintenanceSummary.label(List.of(
   s("엔진오일","2026-10-01","",3000),s("체인","2026-10-05","2026-10-18",0)),today,4000));
  eq("정비 정상",WidgetMaintenanceSummary.label(List.of(
   s("엔진오일","2026-05-01","",3000),s("엔진오일","2026-10-01","",9000)),today,4000));
  eq("정비 정상",WidgetMaintenanceSummary.label(List.of(
   s("엔진오일","2026-10-01","",3000),s("엔진오일","2026-10-01","",9000)),today,4000));
  eq("점검 필요 1건",WidgetMaintenanceSummary.label(List.of(
   s("엔진오일","2026-05-01","",3000),s("엔진오일","2026-10-01","",9000),
   s("체인","2026-10-01","",3500)),today,4000));
  eq("곧 점검 1건",WidgetMaintenanceSummary.label(List.of(
   s("엔진오일","2026-10-01","",5000),s("체인","2026-10-01","",9000)),today,4600));
  eq("정비 일정 없음",WidgetMaintenanceSummary.label(null,today,4000));
  eq("점검 필요 1건",WidgetMaintenanceSummary.label(List.of(
   s("엔진오일","2026-10-01","",3000),s("엔진오일","NOT-A-DATE","",9000)),today,4000));
  eq("점검 필요 1건",WidgetMaintenanceSummary.label(List.of(
   s("엔진오일","2026-10-01","",3000),s("엔진오일","2026-99-99","",9000)),today,4000));
  eq("정비 일정 없음",WidgetMaintenanceSummary.label(List.of(s("","2026-10-01","",3000)),today,4000));
  eq("점검 필요 1건",WidgetMaintenanceSummary.label(List.of(
   s("체인","2026-10-01","2026-10-18",5000)),today,Double.NaN));
  eq("곧 점검 1건",WidgetMaintenanceSummary.label(List.of(
   s("체인","2026-10-01","2026-11-01",Double.NaN)),today,-1));
  eq("정비 일정 없음",WidgetMaintenanceSummary.label(List.of(
   s("체인","2026-10-01","",Double.POSITIVE_INFINITY)),today,4000));
  eq("정비 정상",WidgetMaintenanceSummary.label(List.of(
   s(" 엔진오일 ","2026-05-01","",3000),s("엔진오일","2026-10-01","",9000)),today,4000));
  eq("점검 필요 1건",WidgetMaintenanceSummary.label(List.of(
   s("체인"," 2026-10-01 "," 2026-10-18 ",0)),today,4000));
  eq("정비 정상",WidgetMaintenanceSummary.label(List.of(
   s("체인","2026-10-01"," 2027-01-01 ",0)),today,4000));
  System.out.println("PASS: "+checks+" widget maintenance summary checks");
 }
}
