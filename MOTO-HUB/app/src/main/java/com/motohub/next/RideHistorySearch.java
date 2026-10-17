package com.motohub.next;

import java.util.Locale;

/** Offline search of rendered ride labels only; never reads or transmits coordinates. */
final class RideHistorySearch {
 static final int MAX_LENGTH=40;
 private RideHistorySearch(){}
 static String limit(String value){
  if(value==null)return "";
  String trimmed=value.trim();
  return trimmed.length()>MAX_LENGTH?trimmed.substring(0,MAX_LENGTH):trimmed;
 }
 static String normalize(String value){
  if(value==null)return "";
  return value.toLowerCase(Locale.ROOT).replaceAll("[\\s./:()년월일-]", "");
 }
 static boolean matches(String date,String status,String query){
  String needle=normalize(limit(query));
  if(needle.isEmpty())return true;
  return normalize(date).contains(needle)||normalize(status).contains(needle);
 }
}
