package com.motohub.next;
import java.io.File;
import org.json.JSONObject;

/** Ride title and memo live in the ordinary backup JSON; GPS track bytes never change. */
final class RideNotes {
 private RideNotes(){}
 private static boolean valid(File track){
  return track!=null&&track.isFile()&&track.getName().matches(BackupFormat.TRACK)
   &&track.getParentFile()!=null&&"rides".equals(track.getParentFile().getName());
 }
 static String title(JSONObject database,File track){
  return detail(database,track,"title");
 }
 static String memo(JSONObject database,File track){
  return detail(database,track,"memo");
 }
 private static String detail(JSONObject database,File track,String field){
  if(!valid(track)||database==null)return "";
  JSONObject settings=database.optJSONObject("settings");
  JSONObject notes=settings==null?null:settings.optJSONObject("rideNotes");
  JSONObject record=notes==null?null:notes.optJSONObject(track.getName());
  return record==null?"":record.optString(field,"");
 }
 static void update(JSONObject database,File track,String title,String memo)throws Exception{
  if(!valid(track))throw new IllegalArgumentException("기록된 라이딩 파일을 찾지 못했어요.");
  if(title==null||memo==null||title.length()>60||memo.length()>500||
     title.indexOf('\n')>=0||title.indexOf('\r')>=0)
   throw new IllegalArgumentException("제목은 한 줄 60자, 메모는 500자 이내로 입력해 주세요.");
  JSONObject settings=database.getJSONObject("settings");
  JSONObject all=settings.optJSONObject("rideNotes");
  if(all==null){all=new JSONObject();settings.put("rideNotes",all);}
  if(all.length()>10000&&!all.has(track.getName()))throw new IllegalArgumentException("메모 저장 개수를 초과했어요.");
  if(title.trim().isEmpty()&&memo.trim().isEmpty())all.remove(track.getName());
  else all.put(track.getName(),new JSONObject().put("title",title.trim()).put("memo",memo.trim()));
 }
}