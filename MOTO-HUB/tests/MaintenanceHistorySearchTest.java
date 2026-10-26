package com.motohub.next;
public final class MaintenanceHistorySearchTest {
 static int checks;
 static void ok(boolean p,String name){checks++;if(!p)throw new AssertionError(name);}
 public static void main(String[] args){
  ok(MaintenanceHistorySearch.matches("","엔진오일","2026-10-20",15000),"empty search all");
  ok(MaintenanceHistorySearch.matches("엔진오일","엔진오일","2026-10-20",15000),"exact term");
  ok(MaintenanceHistorySearch.matches("엔진 오일","엔진 오일 교환","2026-10-20",15000),"tokenized terms");
  ok(MaintenanceHistorySearch.matches("오일 2026-10","엔진오일","2026-10-20",15000),"two terms across fields");
  ok(MaintenanceHistorySearch.matches("15000","엔진오일","2026-10-20",15000),"odo query");
  ok(!MaintenanceHistorySearch.matches("타이어","엔진오일","2026-10-20",15000),"other service excluded");
  ok(!MaintenanceHistorySearch.matches("2025","엔진오일","2026-10-20",15000),"other year");
  ok(MaintenanceHistorySearch.matches("  오일   15000 ","엔진오일","2026-10-20",15000),"trim whitespace");
  ok(!MaintenanceHistorySearch.matches("Infinity","엔진오일","2026-10-20",Double.POSITIVE_INFINITY),"no forged invalid ODO");
  ok(MaintenanceHistorySearch.matches(null,"엔진오일","2026-10-20",15000),"null query safe");
  System.out.println("PASS MaintenanceHistorySearchTest "+checks);
 }
}