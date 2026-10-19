package com.motohub.next;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/** Read-only history caption; corrupt bike metadata must never masquerade as unassigned. */
final class RideHistoryBikeLabel {
 private RideHistoryBikeLabel(){}
 static String label(File track,Map<String,String> bikeNames){
  try{
   String id=RideBikeBinding.readStrict(track);
   if(id.isEmpty())return "바이크 미지정";
   String name=bikeNames.get(id);
   return name==null||name.trim().isEmpty()?"등록되지 않은 바이크":name;
  }catch(IOException damaged){return "바이크 연결 정보 손상";}
 }
}
