package com.motohub.next;

/** Pure JDK regression for bad imported maintenance values. */
public final class MaintenanceImportedDataTest {
 private static int checks;
 private static void same(Object actual,Object expected,String label){
  checks++;
  if(!expected.equals(actual))throw new AssertionError(label+": "+actual+" expected "+expected);
 }
 private static void rejects(Runnable work,String label){
  checks++;
  try{work.run();throw new AssertionError(label+": accepted invalid threshold");}
  catch(IllegalArgumentException expected){}
 }
 public static void main(String[] args){
  MaintenancePlanner.Status none=MaintenancePlanner.Status.NONE;
  MaintenancePlanner.Status soon=MaintenancePlanner.Status.SOON;
  MaintenancePlanner.Status overdue=MaintenancePlanner.Status.OVERDUE;
  MaintenancePlanner.Status later=MaintenancePlanner.Status.LATER;
  same(MaintenancePlanner.status(" 2026-10-30 ",0,"2026-10-28",1000,30,500),soon,"trimmed due date");
  same(MaintenancePlanner.status("  ",0,"2026-10-28",1000,30,500),none,"blank due date");
  same(HomeMaintenanceStatus.safeStatus("2026-10-27",Double.NaN,"2026-10-28",800),overdue,"date survives NaN mileage");
  same(HomeMaintenanceStatus.safeStatus("2026-11-30",-100,"2026-10-28",800),later,"date survives negative mileage");
  same(HomeMaintenanceStatus.safeStatus(null,1000,"2026-10-28",Double.NaN),none,"unknown odometer suppresses false km notice");
  same(HomeMaintenanceStatus.safeStatus("bad-date",1200,"2026-10-28",1000),soon,"malformed date retains km reminder");
  same(HomeMaintenanceStatus.safeStatus("bad-date",-5,"2026-10-28",1000),none,"fully broken record harmless");
  same(HomeMaintenanceStatus.safeStatus("",800,"2026-10-28",1000),overdue,"overdue mileage");
  rejects(()->MaintenancePlanner.status("",1000,"2026-10-28",0,30,Double.NaN),"NaN alert distance");
  rejects(()->MaintenancePlanner.status("",1000,"2026-10-28",0,30,Double.POSITIVE_INFINITY),"infinite alert distance");
  System.out.println("PASS MaintenanceImportedDataTest "+checks);
 }
}
