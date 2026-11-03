package com.motohub.next;

import java.io.File;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import org.json.*;

/** View-only chronology. Never infers a physical odometer reading from GPS. */
final class MotoTimeline {
 static final String ALL="전체",RIDE="라이딩",SERVICE="정비",FUEL="주유",OTHER="비용";
 static final String[] FILTERS={ALL,RIDE,SERVICE,FUEL,OTHER};
 // Activity passes one snapshot ZoneId for consistent GPS/ledger ordering.
 // No hard-coded Korean zone: travel screens must agree with ride history.
 static final class Event {
  final String kind,day,title,detail,collection,recordId,trackName;
  final long time;
  Event(String kind,String day,String title,String detail,String collection,String recordId,String trackName,long time){
   this.kind=kind;this.day=day;this.title=title;this.detail=detail;
   this.collection=collection;this.recordId=recordId;this.trackName=trackName;this.time=time;
  }
  boolean matches(String filter){return ALL.equals(filter)||kind.equals(filter);}
 }
 private MotoTimeline(){}
 static long startOfDay(String date){return startOfDay(date,ZoneId.systemDefault());}
 static long startOfDay(String date,ZoneId zone){
  try{return LocalDate.parse(date).atStartOfDay(zone).toInstant().toEpochMilli();}
  catch(Exception ignored){return -1;}
 }
 static String safeText(String raw,int max){
  if(raw==null)return "";
  return raw.replace('\n',' ').replace('\r',' ').replace('\t',' ').trim().substring(0,Math.min(max,raw.replace('\n',' ').replace('\r',' ').replace('\t',' ').trim().length()));
 }
 static List<Event> ledger(JSONObject snapshot,String bikeId){
  return ledger(snapshot,bikeId,ZoneId.systemDefault());
 }
 static List<Event> ledger(JSONObject snapshot,String bikeId,ZoneId zone){
  ArrayList<Event> events=new ArrayList<>();
  if(snapshot==null||!RideBikeBinding.valid(bikeId))return events;
  for(String collection:new String[]{"services","expenses"}){
   JSONArray rows=snapshot.optJSONArray(collection);
   if(rows==null)continue;
   for(int i=0;i<rows.length();i++){
    JSONObject item=rows.optJSONObject(i);
    if(item==null||!bikeId.equals(item.optString("vehicleId")))continue;
    String day=item.optString("date","");
    long timestamp=startOfDay(day,zone);
    if(timestamp<0)continue;
    String kind="services".equals(collection)?SERVICE:
      ("주유".equals(item.optString("type"))?FUEL:OTHER);
    String name=safeText(item.optString("type"),45);
    if(name.isEmpty())name="기타 기록";
    boolean pending="services".equals(collection)&&item.optBoolean("scheduleOnly",false);
    double amount=item.optDouble("services".equals(collection)?"cost":"amount",Double.NaN);
    String money=Double.isFinite(amount)&&amount>=0?
      String.format(Locale.KOREA,"%,.0f원",amount):"금액 미입력";
    String note=safeText(item.optString("note"),75);
    String title=pending?name+" · 정비 예정":name;
    String detail=money+(item.has("mileage")?" · "+String.format(Locale.KOREA,"%,.0f km",item.optDouble("mileage")):"")+
      (note.isEmpty()?"":" · "+note);
    events.add(new Event(kind,day,title,detail,collection,item.optString("id"),"",timestamp));
   }
  }
  sort(events);
  return events;
 }
 static Event ride(File track,JSONObject db,String expectedBike)throws java.io.IOException{
  return ride(track,db,expectedBike,ZoneId.systemDefault());
 }
 static Event ride(File track,JSONObject db,String expectedBike,ZoneId zone)throws java.io.IOException{
  if(track==null||!RideMileageIntegrity.validCompleted(track))return null;
  if(!expectedBike.equals(RideBikeBinding.readStrict(track)))return null;
  Long started=RideHistoryLabel.startTime(track);
  if(started==null||started<=0)return null;
  String date=java.time.Instant.ofEpochMilli(started).atZone(zone).toLocalDate().toString();
  String title=safeText(RideNotes.title(db,track),65);
  if(title.isEmpty())title="GPS 라이딩";
  String category=RideCategory.read(track);
  String memo=safeText(RideNotes.memo(db,track),75);
  String detail="GPS 완료 기록"+(category==null||category.isEmpty()?"":" · "+category)+
    (memo.isEmpty()?"":" · "+memo);
  return new Event(RIDE,date,title,detail,"","",track.getName(),started);
 }
 static void sort(List<Event> events){
  events.sort((x,y)->{
   int cmp=Long.compare(y.time,x.time);
   if(cmp!=0)return cmp;
   cmp=x.kind.compareTo(y.kind);
   return cmp!=0?cmp:x.recordId.compareTo(y.recordId);
  });
 }
 /** Select most recent N in O(total log N) without sorting the whole archive on every progress update. */
 static List<Event> newest(List<Event> events,int limit){
  ArrayList<Event> result=new ArrayList<>();
  if(events==null||limit<=0)return result;
  java.util.Comparator<Event> oldest=(x,y)->{
   int c=Long.compare(x.time,y.time);
   if(c!=0)return c;
   c=y.kind.compareTo(x.kind);
   return c!=0?c:y.recordId.compareTo(x.recordId);
  };
  java.util.PriorityQueue<Event> heap=new java.util.PriorityQueue<>(Math.min(limit,64),oldest);
  for(Event e:events){
   if(heap.size()<limit)heap.add(e);
   else if(oldest.compare(e,heap.peek())>0){heap.poll();heap.add(e);}
  }
  result.addAll(heap);sort(result);return result;
 }
 static List<Event> filtered(List<Event> events,String filter,int max){
  ArrayList<Event> selected=new ArrayList<>();
  if(events==null||max<=0)return selected;
  for(Event event:events)if(event.matches(filter)){
   selected.add(event);
   if(selected.size()>=max)break;
  }
  return selected;
 }
}
