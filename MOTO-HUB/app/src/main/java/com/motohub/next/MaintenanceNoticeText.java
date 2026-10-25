package com.motohub.next;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeParseException;
import java.util.Locale;

/** Human-readable, offline maintenance deadlines; never sends bike data to a server. */
final class MaintenanceNoticeText {
 private MaintenanceNoticeText() {}
 static String detail(String dueDate, double dueKm, String today, double odometer) {
  StringBuilder result = new StringBuilder();
  if (dueDate != null && !dueDate.isBlank()) {
   try {
    long days = ChronoUnit.DAYS.between(LocalDate.parse(today), LocalDate.parse(dueDate));
    result.append(dueDate).append(" (");
    if (days < 0) result.append(Math.abs(days)).append("일 지남");
    else if (days == 0) result.append("오늘");
    else result.append("D-").append(days);
    result.append(")");
   } catch (DateTimeParseException ignored) { /* Corrupt imported dates are not displayed. */ }
  }
  if (Double.isFinite(dueKm) && dueKm > 0 && Double.isFinite(odometer) && odometer >= 0) {
   if (result.length() > 0) result.append(" · ");
   double remaining = dueKm - odometer;
   if (remaining <= 0) result.append(String.format(Locale.KOREA, "%,.0f km 초과", -remaining));
   else result.append(String.format(Locale.KOREA, "%,.0f km 남음", remaining));
  }
  return result.toString();
 }
}
