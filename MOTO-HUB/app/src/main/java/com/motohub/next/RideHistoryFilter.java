package com.motohub.next;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

/** Local-only ride history state; no track coordinates are loaded or transmitted. */
final class RideHistoryFilter {
 static final String ALL="전체", ACTIVE="기록 중", COMPLETE="완료", IMPORTED="GPX", RECOVERED="복구", INTERRUPTED="중단";
 static final String[] OPTIONS={ALL,ACTIVE,COMPLETE,IMPORTED,RECOVERED,INTERRUPTED};
 private RideHistoryFilter(){}

 static String normalize(String value){
  for(String option:OPTIONS)if(option.equals(value))return option;
  return ALL;
 }

 /** A rotation snapshot takes precedence; otherwise restore this device's UI preference. */
 static String restore(String persisted,String savedState){
  return normalize(savedState!=null?savedState:persisted);
 }

 static String status(File track,String activeName,boolean recording){
  if(recording&&track.getName().equals(activeName))return ACTIVE;
  // A GPX marker takes precedence over .done, matching the existing UI semantics.
  if(new File(track.getPath()+".imported").isFile())return IMPORTED;
  if(new File(track.getPath()+".done").isFile())return COMPLETE;
  if(new File(track.getPath()+".recovered").isFile())return RECOVERED;
  return INTERRUPTED;
 }

 static List<File> select(List<File> rides,String filter,String activeName,boolean recording){
  String wanted=normalize(filter);
  List<File> selected=new ArrayList<>();
  for(File ride:rides)if(ALL.equals(wanted)||wanted.equals(status(ride,activeName,recording)))selected.add(ride);
  return selected;
 }

 static Map<String,Integer> counts(List<File> rides,String activeName,boolean recording){
  Map<String,Integer> counts=new LinkedHashMap<>();
  for(String option:OPTIONS)counts.put(option,0);
  counts.put(ALL,rides.size());
  for(File ride:rides){
   String status=status(ride,activeName,recording);
   counts.put(status,counts.get(status)+1);
  }
  return counts;
 }

 static int count(List<File> rides,String filter,String activeName,boolean recording){
  return select(rides,filter,activeName,recording).size();
 }
}
