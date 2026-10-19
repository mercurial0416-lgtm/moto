package com.motohub.next;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Offline ordering for the garage's maintenance dialog. No user data leaves the device. */
final class MaintenanceNoticeOrder {
 private MaintenanceNoticeOrder() {}
 static final class Notice {
  final String type, serviceDate, nextDate;
  final double nextKm;
  Notice(String type,String serviceDate,String nextDate,double nextKm){
   this.type=type==null?"":type;
   this.serviceDate=serviceDate==null?"":serviceDate;
   this.nextDate=nextDate==null?"":nextDate;
   this.nextKm=nextKm;
  }
 }
 /** ISO service dates sort lexicographically; equal dates use the last saved record. */
 static boolean isNewerOrSame(String candidateDate,String previousDate){
  String candidate=candidateDate==null?"":candidateDate;
  String previous=previousDate==null?"":previousDate;
  return candidate.compareTo(previous)>=0;
 }
 static List<Notice> prioritized(List<Notice> history,String today,double odometer){
  Map<String,Notice> latest=new LinkedHashMap<>();
  if(history!=null)for(Notice n:history){
   if(n==null||n.type.trim().isEmpty())continue;
   String canonical=n.type.trim();
   n=new Notice(canonical,n.serviceDate,n.nextDate,n.nextKm);
   Notice old=latest.get(canonical);
   if(old==null||isNewerOrSame(n.serviceDate,old.serviceDate))latest.put(n.type,n);
  }
  List<Notice> scheduled=new ArrayList<>();
  for(Notice n:latest.values()){
   if(HomeMaintenanceStatus.safeStatus(n.nextDate,n.nextKm,today,odometer)!=MaintenancePlanner.Status.NONE)
    scheduled.add(n);
  }
  scheduled.sort(Comparator
   .comparingInt((Notice n)->MaintenancePlanner.order(HomeMaintenanceStatus.safeStatus(n.nextDate,n.nextKm,today,odometer)))
   .thenComparing(n->n.nextDate.isEmpty()?"9999-12-31":n.nextDate)
   .thenComparingDouble(n->n.nextKm>0?n.nextKm:Double.POSITIVE_INFINITY)
   .thenComparing(n->n.type));
  return scheduled;
 }
}
