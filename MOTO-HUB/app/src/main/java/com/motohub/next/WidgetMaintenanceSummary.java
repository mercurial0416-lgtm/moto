package com.motohub.next;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Builds the launcher widget's maintenance label from the latest service per type.
 *  Older service records are history, not outstanding reminders. No network or GPS involved.
 */
final class WidgetMaintenanceSummary {
 private WidgetMaintenanceSummary() {}
 static final class Service {
  final String type, serviceDate, nextDate;
  final double nextKm;
  Service(String type, String serviceDate, String nextDate, double nextKm) {
   this.type=type==null?"":type;
   this.serviceDate=serviceDate==null?"":serviceDate;
   this.nextDate=nextDate==null?"":nextDate;
   this.nextKm=nextKm;
  }
 }
 static String label(List<Service> history, String today, double odometer) {
  Map<String,Service> latest=new LinkedHashMap<>();
  if(history==null)return "정비 일정 없음";
  for(Service service:history) {
   if(service==null||service.type.isBlank())continue;
   String normalizedType=service.type.trim();
   String serviceDate=service.serviceDate.trim();
   // Corrupt imported dates must not replace a valid recent maintenance record.
   if(!serviceDate.isBlank()) {
    try { LocalDate.parse(serviceDate); }
    catch(DateTimeParseException invalid) { continue; }
   }
   Service previous=latest.get(normalizedType);
   // When dates tie, the last appended record wins, matching Store.upsert's append order.
   if(previous==null||serviceDate.compareTo(previous.serviceDate.trim())>=0)
    latest.put(normalizedType,service);
  }
  int overdue=0, soon=0, scheduled=0;
  for(Service service:latest.values()) {
   // Imported odometer values can be NaN/negative; keep valid date reminders visible.
   double safeOdo=Double.isFinite(odometer)&&odometer>=0?odometer:0;
   double safeNextKm=Double.isFinite(service.nextKm)&&service.nextKm>=0?service.nextKm:0;
   MaintenancePlanner.Status status=HomeMaintenanceStatus.safeStatus(service.nextDate.trim(),safeNextKm,today,safeOdo);
   if(status==MaintenancePlanner.Status.NONE)continue;
   scheduled++;
   if(status==MaintenancePlanner.Status.OVERDUE)overdue++;
   else if(status==MaintenancePlanner.Status.SOON)soon++;
  }
  if(overdue>0)return "점검 필요 "+overdue+"건";
  if(soon>0)return "곧 점검 "+soon+"건";
  return scheduled>0?"정비 정상":"정비 일정 없음";
 }
}
