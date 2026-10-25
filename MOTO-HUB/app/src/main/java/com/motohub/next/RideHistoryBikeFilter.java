package com.motohub.next;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** On-device vehicle filter for ride history; corrupt associations are never treated as unassigned. */
final class RideHistoryBikeFilter {
 static final String ALL="전체 바이크", UNASSIGNED="바이크 미지정";
 private RideHistoryBikeFilter(){}
 static String normalize(String scope){
  if(UNASSIGNED.equals(scope))return UNASSIGNED;
  return RideBikeBinding.valid(scope)?scope:ALL;
 }
 static String restore(String persisted,String saved){return normalize(saved!=null?saved:persisted);}
 static boolean include(File track,String scope){
  String selected=normalize(scope);
  if(ALL.equals(selected))return true;
  try{
   String bike=RideBikeBinding.readStrict(track);
   return UNASSIGNED.equals(selected)?bike.isEmpty():selected.equals(bike);
  }catch(IOException corrupt){return false;}
 }
 static List<File> select(List<File> tracks,String scope){
  List<File> result=new ArrayList<>();
  for(File track:tracks)if(include(track,scope))result.add(track);
  return result;
 }
 /** Warn rather than silently hiding invalid .bike metadata in a scoped view. */
 static int damaged(List<File> tracks){
  int damaged=0;
  for(File track:tracks)try{RideBikeBinding.readStrict(track);}catch(IOException ex){damaged++;}
  return damaged;
 }
}
