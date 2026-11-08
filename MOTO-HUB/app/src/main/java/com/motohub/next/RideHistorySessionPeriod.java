package com.motohub.next;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Filter a recorded ride by its original start, even when GPS storage rotated
 * to new files after midnight. Read-only, bike-scoped, no GPS data edits.
 */
final class RideHistorySessionPeriod {
 private RideHistorySessionPeriod(){}
 static List<File> select(List<File> tracks,long now,String period){
  List<File> result=new ArrayList<>();
  if(tracks==null||tracks.isEmpty())return result;
  Map<String,Long> first=new HashMap<>();
  Map<File,String> grouping=new HashMap<>();
  for(File track:tracks){
   String session="";
   Long start=RideHistoryLabel.startTime(track);
   // Imported GPX must keep its actual recorded start, not a synthetic filename
   // shared with any local session.
   if(!new File(track.getPath()+".imported").isFile()){
    String id=RideSegmentGroup.id(track);
    if(!id.isEmpty())try{
     session=id+"|"+RideBikeBinding.readStrict(track);
    }catch(IOException invalid){/* Corrupt bike sidecar stays independent. */}
   }
   grouping.put(track,session);
   if(!session.isEmpty()&&start!=null&&start>0)
    first.merge(session,start,Math::min);
  }
  for(File track:tracks){
   String session=grouping.get(track);
   Long firstTime=session==null||session.isEmpty()?null:first.get(session);
   Long started=firstTime==null?RideHistoryLabel.startTime(track):firstTime;
   if(RideHistoryPeriod.include(started,now,period))result.add(track);
  }
  return result;
 }
}
