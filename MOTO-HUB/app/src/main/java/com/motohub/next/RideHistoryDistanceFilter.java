package com.motohub.next;

import java.io.File;
import java.util.List;

/** Opt-in bounded GPS distance filtering. Never silently present truncated results as complete. */
final class RideHistoryDistanceFilter {
 static final String ALL="전체",SHORT="10km 미만",MEDIUM="10~50km",LONG="50km 이상";
 static final String[] OPTIONS={ALL,SHORT,MEDIUM,LONG};
 static final int MAX_FILES=150;
 static final long MAX_TOTAL_BYTES=80_000_000L;
 private RideHistoryDistanceFilter(){}
 static String normalize(String value){
  for(String option:OPTIONS)if(option.equals(value))return option;
  return ALL;
 }
 static boolean accepts(String filter,double km){
  if(!Double.isFinite(km)||km<0)return false;
  switch(normalize(filter)){
   case SHORT:return km<10;
   case MEDIUM:return km>=10&&km<50;
   case LONG:return km>=50;
   default:return true;
  }
 }
 static boolean withinBudget(List<File> files){
  if(files==null||files.size()>MAX_FILES)return false;
  long size=0;
  for(File file:files){
   if(file==null||file.length()<0||file.length()>TrackFile.MAX_BYTES)return false;
   size+=file.length();
   if(size>MAX_TOTAL_BYTES)return false;
  }
  return true;
 }
}
