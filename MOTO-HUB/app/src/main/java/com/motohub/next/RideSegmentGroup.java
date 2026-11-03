package com.motohub.next;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * A capped live GPS session is represented by multiple ordinary .track files
 * whose names share exactly one UUID. No schema or sidecar extension is needed:
 * existing ZIP export/restore, recovery and deletion already handle each file.
 */
final class RideSegmentGroup {
 private RideSegmentGroup(){}
 static String id(String name){
  if(name==null||!name.matches("[0-9]{10,15}-[a-f0-9-]{36}\\.track"))return "";
  return name.substring(name.indexOf('-')+1,name.length()-6);
 }
 static String id(File track){return track==null?"":id(track.getName());}
 static Map<String,Integer> counts(List<File> tracks){
  Map<String,Integer> counts=new HashMap<>();
  if(tracks!=null)for(File f:tracks){
   String key=id(f);
   if(!key.isEmpty())counts.put(key,counts.getOrDefault(key,0)+1);
  }
  return counts;
 }
 static List<File> members(File filesDir,String trackName)throws IOException{
  String key=id(trackName);
  if(key.isEmpty())throw new IOException("유효하지 않은 GPS 구간");
  List<File> found=new ArrayList<>();
  for(File f:TrackFile.tracks(filesDir))
   if(key.equals(id(f))&&RideMileageIntegrity.validCompleted(f))found.add(f);
  found.sort((a,b)->a.getName().compareTo(b.getName()));
  return found;
 }
 /** Collapse only known-valid rides. A split never alters underlying per-file evidence. */
 static List<MotoTimeline.Event> collapse(List<MotoTimeline.Event> raw){
  List<MotoTimeline.Event> result=new ArrayList<>();
  Map<String,List<MotoTimeline.Event>> groups=new LinkedHashMap<>();
  if(raw==null)return result;
  for(MotoTimeline.Event e:raw){
   String key=e==null?"":id(e.trackName);
   if(e!=null&&MotoTimeline.RIDE.equals(e.kind)&&!key.isEmpty())
    groups.computeIfAbsent(key,k->new ArrayList<>()).add(e);
   else if(e!=null)result.add(e);
  }
  for(List<MotoTimeline.Event> group:groups.values()){
   group.sort(Comparator.comparingLong(e->e.time));
   MotoTimeline.Event first=group.get(0);
   if(group.size()==1){result.add(first);continue;}
   result.add(new MotoTimeline.Event(MotoTimeline.RIDE,first.day,first.title,
    "한 번의 라이딩 · 연속 GPS "+group.size()+"구간 · 선택하면 구간별 상세 보기",
    first.collection,first.recordId,first.trackName,first.time));
  }
  MotoTimeline.sort(result);
  return result;
 }
}
