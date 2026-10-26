package com.motohub.next;

import java.util.Locale;

/** Offline service-history query. No remote calls or indexing of personal notes. */
final class MaintenanceHistorySearch {
 private MaintenanceHistorySearch(){}
 static String normalize(String text){
  if(text==null)return "";
  String compact=text.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+"," ");
  return compact.length()>80?compact.substring(0,80):compact;
 }
 static boolean matches(String query,String type,String date,double mileage){
  String needle=normalize(query);
  if(needle.isEmpty())return true;
  String haystack=normalize(type)+" "+normalize(date)+
   " "+(Double.isFinite(mileage)?String.format(Locale.ROOT,"%.0f",mileage):"");
  for(String term:needle.split(" "))if(!haystack.contains(term))return false;
  return true;
 }
}
