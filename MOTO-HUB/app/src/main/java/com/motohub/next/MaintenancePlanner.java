package com.motohub.next;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** Offline due-date and odometer alert classification. No background notifications. */
public final class MaintenancePlanner {
 private MaintenancePlanner() {}
 public enum Status { OVERDUE, SOON, LATER, NONE }
 public static Status status(String dueDate, double dueOdometer, String today,
                             double currentOdometer, int daysBefore, double kmBefore) {
  if(!Double.isFinite(dueOdometer) || !Double.isFinite(currentOdometer) ||
     dueOdometer < 0 || currentOdometer < 0 || daysBefore < 0 ||
     !Double.isFinite(kmBefore) || kmBefore < 0)
   throw new IllegalArgumentException("정비 예정 주기 정보가 올바르지 않습니다.");
  LocalDate now = LocalDate.parse(today);
  String normalizedDate = dueDate == null ? "" : dueDate.trim();
  boolean hasDate = !normalizedDate.isEmpty();
  LocalDate deadline = hasDate ? LocalDate.parse(normalizedDate) : null;
  boolean hasKm = dueOdometer > 0;
  if(!hasDate && !hasKm) return Status.NONE;
  if((hasDate && !deadline.isAfter(now)) || (hasKm && currentOdometer >= dueOdometer))
   return Status.OVERDUE;
  if((hasDate && ChronoUnit.DAYS.between(now,deadline) <= daysBefore) ||
     (hasKm && dueOdometer - currentOdometer <= kmBefore)) return Status.SOON;
  return Status.LATER;
 }
 public static int order(Status status) {
  return switch(status) {case OVERDUE -> 0;case SOON -> 1;case LATER -> 2;case NONE -> 3;};
 }
 public static String caption(Status status) {
  return switch(status) {case OVERDUE -> "점검 필요";case SOON -> "곧 점검";case LATER -> "예정";case NONE -> "미설정";};
 }
}
