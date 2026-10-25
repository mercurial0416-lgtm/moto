package com.motohub.next;
import org.json.*;import java.util.*;import java.io.*;
final class GroupClient {
 static final GroupClient INSTANCE=new GroupClient();final SocialClient api=SocialClient.INSTANCE;JSONArray groups=new JSONArray(),members=new JSONArray();
 JSONObject rpc(String action,String group,String value)throws Exception{return new JSONObject(api.call("POST","/rest/v1/rpc/mh_group_action",new JSONObject().put("action",action).put("group_id",group.isEmpty()?JSONObject.NULL:UUID.fromString(group).toString()).put("value",value),true));}
 void load()throws Exception{groups=new JSONArray(api.call("GET","/rest/v1/mh_groups?select=id,owner,name,meetup,destination,active&active=eq.true&limit=100",null,true));members=new JSONArray(api.call("GET","/rest/v1/mh_members?select=group_id,user_id,status&limit=1000",null,true));api.load();}
 void create(String name,String meetup,String destination)throws Exception{if(name.trim().isEmpty()||name.length()>60||meetup.length()>200||destination.length()>200)throw new IOException("방 이름은 1~60자, 집결지·목적지는 각각 200자 이내로 입력하세요.");JSONObject value=new JSONObject().put("name",name.trim()).put("meetup",meetup).put("destination",destination);rpc("create","",value.toString());load();}
 void action(String action,String group,String value)throws Exception{rpc(action,group,value);load();}
 boolean joined(String group){for(int i=0;i<members.length();i++){JSONObject m=members.optJSONObject(i);if(group.equals(m.optString("group_id"))&&api.uid.equals(m.optString("user_id"))&&m.optString("status").equals("accepted"))return true;}return false;}
 JSONArray locations(String group)throws Exception{UUID.fromString(group);return new JSONArray(api.call("GET","/rest/v1/mh_positions?select=user_id,lat,lon,bearing,state,received_at,expires_at&group_id=eq."+group+"&limit=32",null,true));}
}
