package com.motohub.next;

import java.time.DateTimeException;
import java.util.List;

/** Home summary uses the same due/soon thresholds as the maintenance reminder. */
final class HomeMaintenanceStatus {
 private HomeMaintenanceStatus() {}
 static final class Schedule {
  final String dueDate;
  final double dueKm;
  Schedule(String dueDate, double dueKm) { this.dueDate=dueDate; this.dueKm=dueKm; }
 }
 static MaintenancePlanner.Status safeStatus(String dueDate,double dueKm,String today,double odo) {
  try { return MaintenancePlanner.status(dueDate,dueKm,today,odo,30,500); }
  catch (DateTimeException invalidDate) {
   // A malformed imported date must not crash the home screen or hide a valid km reminder.
   return MaintenancePlanner.status("",dueKm,today,odo,30,500);
  }
 }
 static MaintenancePlanner.Status summary(List<Schedule> schedules,String today,double odo) {
  MaintenancePlanner.Status worst=MaintenancePlanner.Status.NONE;
  for (Schedule s:schedules) {
   MaintenancePlanner.Status current=safeStatus(s.dueDate,s.dueKm,today,odo);
   if (MaintenancePlanner.order(current)<MaintenancePlanner.order(worst)) worst=current;
  }
  return worst;
 }
 static String label(MaintenancePlanner.Status status) {
  return switch(status) {
   case OVERDUE -> "점검 필요";
   case SOON -> "곧 점검";
   case LATER -> "정비 정상";
   case NONE -> "정비 일정 없음";
  };
 }
}
