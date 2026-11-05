package com.motohub.next;

import java.io.File;
import java.nio.file.Files;
import org.json.*;

/** Local, user-authored trip stops within existing settings.rideNotes; GPS bytes never change. */
final class TripNotes {
 static final int MAX_STOPS=64;
 private TripNotes(){}
 private static void requireTrack(File track){if(!RideMileageIntegrity.validCompleted(track)||track.getParentFile()==null||Files.isSymbolicLink(track.getParentFile().toPath())||!"rides".equals(track.getParentFile().getName()))throw new IllegalArgumentException("완료된 안전한 라이딩 기록을 선택하세요.");}
 static JSONArray stops(JSONObject database,File track){
  if(database==null||track==null||!track.getName().matches(BackupFormat.TRACK))return new JSONArray();
  JSONObject settings=database.optJSONObject("settings"),all=settings==null?null:settings.optJSONObject("rideNotes"),entry=all==null?null:all.optJSONObject(track.getName());JSONArray result=entry==null?null:entry.optJSONArray("stops");return result==null?new JSONArray():result;
 }
 static JSONObject stop(String name,String memo,String kind,Double latitude,Double longitude)throws Exception{
  name=name==null?"":name.trim();memo=memo==null?"":memo.trim();if(name.isEmpty()||name.length()>80||memo.length()>1000||name.indexOf('\n')>=0||name.indexOf('\0')>=0||memo.indexOf('\0')>=0)throw new IllegalArgumentException("지점 이름은 한 줄 80자, 메모는 1,000자 이내입니다.");
  if(!java.util.Arrays.asList("rest","fuel","visit","note").contains(kind))throw new IllegalArgumentException("방문 지점 종류를 확인하세요.");
  JSONObject result=new JSONObject().put("id",Store.id()).put("name",name).put("memo",memo).put("kind",kind);
  if(latitude!=null||longitude!=null){if(latitude==null||longitude==null||!Double.isFinite(latitude)||!Double.isFinite(longitude)||Math.abs(latitude)>90||Math.abs(longitude)>180)throw new IllegalArgumentException("지도에서 선택한 위치를 확인하세요.");result.put("latitude",latitude).put("longitude",longitude);}return result;
 }
 static JSONArray mapStops(JSONObject database,File track){
  JSONArray source=stops(database,track),safe=new JSONArray();for(int i=0;i<Math.min(source.length(),MAX_STOPS);i++){JSONObject row=source.optJSONObject(i);if(row==null||!row.has("latitude")||!row.has("longitude"))continue;try{JSONObject point=stop(row.optString("name"),row.optString("memo"),row.optString("kind"),row.optDouble("latitude"),row.optDouble("longitude"));safe.put(point);}catch(Exception invalid){/* Imported malformed optional stops must not hide the GPS route. */}}return safe;
 }
 static void append(JSONObject database,File track,JSONObject stop)throws Exception{
  requireTrack(track);if(!RideBikeBinding.valid(stop.optString("id")))throw new IllegalArgumentException("방문 지점 식별자를 확인하세요.");JSONObject checked=stop(stop.optString("name"),stop.optString("memo"),stop.optString("kind"),stop.has("latitude")?stop.optDouble("latitude"):null,stop.has("longitude")?stop.optDouble("longitude"):null);checked.put("id",stop.getString("id"));JSONArray current=stops(database,track);for(int i=0;i<current.length();i++){JSONObject row=current.optJSONObject(i);if(row!=null&&checked.getString("id").equals(row.optString("id")))throw new IllegalArgumentException("이미 등록된 방문 지점입니다.");}if(current.length()>=MAX_STOPS)throw new IllegalArgumentException("한 여행기에 최대 64개 지점을 보관할 수 있습니다.");JSONArray next=new JSONArray(current.toString());next.put(checked);write(database,track,next);
 }
 static void remove(JSONObject database,File track,String id)throws Exception{requireTrack(track);JSONArray current=stops(database,track),next=new JSONArray();int found=0;for(int i=0;i<current.length();i++){JSONObject row=current.getJSONObject(i);if(id.equals(row.optString("id")))found++;else next.put(row);}if(found!=1)throw new IllegalArgumentException("방문 지점이 변경되었습니다.");write(database,track,next);}
 private static void write(JSONObject database,File track,JSONArray stops)throws Exception{
  JSONObject settings=database.getJSONObject("settings"),all=settings.optJSONObject("rideNotes");if(all==null){all=new JSONObject();settings.put("rideNotes",all);}JSONObject old=all.optJSONObject(track.getName()),entry=old==null?new JSONObject():new JSONObject(old.toString());if(old==null&&all.length()>=10000)throw new IllegalArgumentException("여행기 저장 개수를 초과했습니다.");if(stops.length()==0)entry.remove("stops");else entry.put("stops",stops);if(entry.length()==0)all.remove(track.getName());else all.put(track.getName(),entry);
 }
}
