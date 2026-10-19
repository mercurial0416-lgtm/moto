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
  return value.toLowerCase(Locale.ROOT).replaceAll("(?<=\\d)[년월일]", "").replaceAll("[\\s./:()-]", "");
 }
 /** Pre-normalize a snapshot label once, not for every keystroke or page expansion. */
 static String index(String label,String status){return normalize(label)+"|"+normalize(status);}
 /** Space-separated terms are ANDed; punctuation within a date remains searchable. */
 static String[] terms(String query){
  String safe=limit(query);
  if(safe.isEmpty())return new String[0];
  java.util.ArrayList<String> terms=new java.util.ArrayList<>();
  for(String part:safe.split("\\s+")){
   String normalized=normalize(part);
   if(!normalized.isEmpty())terms.add(normalized);
  }
  return terms.toArray(new String[0]);
 }
 static boolean matchesIndex(String index,String[] terms){
  for(String term:terms)if(!index.contains(term))return false;
  return true;
 }
 static boolean matches(String date,String status,String query){
  return matchesIndex(index(date,status),terms(query));
 }
}
