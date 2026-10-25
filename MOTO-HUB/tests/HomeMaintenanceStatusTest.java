package com.motohub.next;
import java.util.List;
public final class HomeMaintenanceStatusTest {
 static int checks=0;
 static void check(boolean yes,String message){checks++;if(!yes)throw new AssertionError(message);}
 static HomeMaintenanceStatus.Schedule s(String date,double km){return new HomeMaintenanceStatus.Schedule(date,km);}
 static void status(MaintenancePlanner.Status expected,HomeMaintenanceStatus.Schedule... schedules){
  check(HomeMaintenanceStatus.summary(List.of(schedules),"2026-10-18",10000)==expected,"expected "+expected);
 }
 public static void main(String[] args){
  status(MaintenancePlanner.Status.NONE);
  status(MaintenancePlanner.Status.NONE,s("",0));
  status(MaintenancePlanner.Status.LATER,s("2027-01-01",12000));
  status(MaintenancePlanner.Status.SOON,s("2026-11-01",12000));
  status(MaintenancePlanner.Status.SOON,s("",10400));
  status(MaintenancePlanner.Status.OVERDUE,s("2026-10-18",12000));
  status(MaintenancePlanner.Status.OVERDUE,s("",10000));
  status(MaintenancePlanner.Status.SOON,s("2027-01-01",12000),s("2026-10-30",14000));
  status(MaintenancePlanner.Status.OVERDUE,s("2026-10-30",14000),s("2026-10-01",0));
  status(MaintenancePlanner.Status.SOON,s("invalid",10300));
  status(MaintenancePlanner.Status.NONE,s("invalid",0));
  check(HomeMaintenanceStatus.label(MaintenancePlanner.Status.SOON).equals("곧 점검"),"soon label");
  check(HomeMaintenanceStatus.label(MaintenancePlanner.Status.OVERDUE).equals("점검 필요"),"overdue label");
  check(HomeMaintenanceStatus.label(MaintenancePlanner.Status.NONE).equals("정비 일정 없음"),"no schedule label");
  System.out.println("PASS: "+checks+" home maintenance status checks");
 }
}
