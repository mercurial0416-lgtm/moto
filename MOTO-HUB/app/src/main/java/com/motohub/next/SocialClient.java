package com.motohub.next;
import android.content.Context;import android.content.SharedPreferences;import org.json.*;import java.io.*;import java.net.*;import java.nio.charset.StandardCharsets;import java.util.*;
/** Checkpoint A: real HTTPS Auth/PostgREST calls; no simulated users or coordinates. */
final class SocialClient {
 static final SocialClient INSTANCE=new SocialClient();
 String access="",refresh="",uid="";long expires;JSONObject profile=new JSONObject();JSONArray profiles=new JSONArray(),requests=new JSONArray();String code="";
 static volatile String customUrl="",customKey="";static volatile Context app;
 static synchronized void loadSettings(Context ctx){
  app=ctx.getApplicationContext();
  SharedPreferences prefs=ctx.getSharedPreferences("social_server",Context.MODE_PRIVATE);
  try{String url=prefs.getString("url","");String key=prefs.getString("key","");
   if(!url.isEmpty()&&!key.isEmpty()){customUrl=ServerSettings.validateUrl(url);customKey=ServerSettings.validateKey(key);}
  }catch(Exception invalid){customUrl="";customKey="";}
  if(configured())INSTANCE.restoreSession();
 }
 static String serverUrl(){return customUrl.isEmpty()?BuildConfig.SUPABASE_URL:customUrl;}
 static String publicKey(){return customKey.isEmpty()?BuildConfig.SUPABASE_KEY:customKey;}
 static boolean configured(){return !serverUrl().isEmpty()&&!publicKey().isEmpty();}
 static synchronized void saveSettings(Context ctx,String rawUrl,String rawKey)throws Exception{
  if(!INSTANCE.uid.isEmpty()||Sharing.enabled)throw new IOException("서버 변경 전에 위치 공유를 중지하고 로그아웃하세요.");
  String url=ServerSettings.validateUrl(rawUrl),key=ServerSettings.validateKey(rawKey);
  if(!ctx.getSharedPreferences("social_server",Context.MODE_PRIVATE).edit().putString("url",url).putString("key",key).commit())
   throw new IOException("서버 설정을 저장하지 못했습니다.");
  customUrl=url;customKey=key;
 }
 static synchronized void resetSettings(Context ctx)throws Exception{
  if(!INSTANCE.uid.isEmpty()||Sharing.enabled)throw new IOException("서버 변경 전에 위치 공유를 중지하고 로그아웃하세요.");
  if(!ctx.getSharedPreferences("social_server",Context.MODE_PRIVATE).edit().clear().commit())
   throw new IOException("설정 초기화에 실패했습니다.");
  customUrl="";customKey="";
 }
 static String base(String value)throws Exception{URI u=new URI(value);if(!"https".equals(u.getScheme())||u.getHost()==null||u.getUserInfo()!=null||u.getQuery()!=null||u.getFragment()!=null||!(u.getPath().isEmpty()||u.getPath().equals("/")))throw new IOException("Supabase HTTPS 프로젝트 주소를 확인하세요.");return value.replaceAll("/+$","");}
 static final class Control {volatile boolean cancelled;volatile HttpURLConnection connection;void cancel(){cancelled=true;HttpURLConnection c=connection;if(c!=null)c.disconnect();}void attach(HttpURLConnection c)throws IOException{connection=c;if(cancelled){c.disconnect();throw new IOException("요청 취소");}}}
 synchronized String call(String method,String path,JSONObject body,boolean authenticated)throws Exception{return call(method,path,body,authenticated,null);}
 synchronized String call(String method,String path,JSONObject body,boolean authenticated,Control control)throws Exception{
  if(control!=null&&control.cancelled)throw new IOException("요청 취소");
  if(!configured())throw new IOException("Supabase 서버 미연동입니다.");if(!publicKey().startsWith("sb_publishable_"))throw new IOException("publishable key가 필요합니다. 관리자키는 사용할 수 없습니다.");
  if(authenticated)ensureSession();HttpURLConnection c=(HttpURLConnection)new URL(base(serverUrl())+path).openConnection();if(control!=null)control.attach(c);c.setConnectTimeout(10000);c.setReadTimeout(10000);c.setInstanceFollowRedirects(false);c.setRequestMethod(method);c.setRequestProperty("apikey",publicKey());c.setRequestProperty("Content-Type","application/json");c.setRequestProperty("Prefer","return=representation,resolution=merge-duplicates");if(authenticated)c.setRequestProperty("Authorization","Bearer "+access);
  try{if(control!=null&&control.cancelled)throw new IOException("요청 취소");if(body!=null){c.setDoOutput(true);try(OutputStream out=c.getOutputStream()){out.write(body.toString().getBytes(StandardCharsets.UTF_8));}}int status=c.getResponseCode();if(status<200||status>=300){if(status==401&&authenticated)clear();throw new IOException(status==401?"로그인이 만료됐거나 인증에 실패했습니다.":status==429?"요청이 많습니다. 잠시 후 다시 시도하세요.":"서버 요청 실패 (HTTP "+status+"). 계정·이메일 인증·DB 설정을 확인하세요.");}try(InputStream in=c.getInputStream()){return new String(UpdateManifest.bounded(in,1_000_000),StandardCharsets.UTF_8);}}finally{c.disconnect();}
 }
 void restoreSession(){if(app==null)return;try{
  String raw=SessionVault.read(app);if(raw.isEmpty())return;
  JSONObject s=new JSONObject(raw);
  String from=s.optString("serverUrl");
  if(!from.equals(serverUrl())||s.optString("uid").isEmpty()||s.optString("refresh").isEmpty()){SessionVault.clear(app);return;}
  access=s.optString("access");refresh=s.optString("refresh");uid=s.optString("uid");expires=s.optLong("expires");
 }catch(Exception e){SessionVault.clear(app);access="";refresh="";uid="";expires=0;}}
 void persistSession(){if(app==null||uid.isEmpty())return;try{
  JSONObject s=new JSONObject().put("serverUrl",serverUrl()).put("access",access).put("refresh",refresh).put("uid",uid).put("expires",expires);
  SessionVault.save(app,s.toString());
 }catch(Exception e){android.util.Log.w("MOTO-HUB","암호화된 세션 저장 실패");}}
 void acceptSession(JSONObject session)throws Exception{String token=session.optString("access_token"),r=session.optString("refresh_token");JSONObject user=session.optJSONObject("user");if(token.isEmpty()||r.isEmpty()||user==null||user.optString("id").isEmpty())throw new IOException("인증 응답이 올바르지 않습니다.");access=token;refresh=r;uid=user.getString("id");expires=System.currentTimeMillis()+session.optLong("expires_in",3600)*1000;persistSession();}
 void ensureSession()throws Exception{if(uid.isEmpty())throw new IOException("로그인이 필요합니다.");if(System.currentTimeMillis()+60000>=expires){try{acceptSession(new JSONObject(call("POST","/auth/v1/token?grant_type=refresh_token",new JSONObject().put("refresh_token",refresh),false)));}catch(Exception e){clear();throw new IOException("로그인이 만료됐습니다. 다시 로그인하세요.");}}}
 void login(String email,String password)throws Exception{clear();acceptSession(new JSONObject(call("POST","/auth/v1/token?grant_type=password",credentials(email,password),false)));load();}
 String signup(String email,String password)throws Exception{JSONObject r=new JSONObject(call("POST","/auth/v1/signup",credentials(email,password),false));if(!r.optString("access_token").isEmpty()){acceptSession(r);load();return "가입됐어요. 프로필을 등록하세요.";}return "이메일의 가입 확인을 완료한 뒤 로그인하세요.";}
 static JSONObject credentials(String email,String password)throws Exception{email=email.trim();if(!email.contains("@")||email.length()>254||password.length()<8)throw new IOException("이메일과 8자 이상 비밀번호를 입력하세요.");return new JSONObject().put("email",email).put("password",password);}
 void load()throws Exception{profiles=new JSONArray(call("GET","/rest/v1/mh_profiles?select=user_id,display_name,bike&limit=500",null,true));profile=new JSONObject();for(int i=0;i<profiles.length();i++){JSONObject p=profiles.getJSONObject(i);if(uid.equals(p.optString("user_id")))profile=p;}JSONArray codes=new JSONArray(call("GET","/rest/v1/mh_codes?select=code&limit=1",null,true));code=codes.length()==0?"":codes.getJSONObject(0).getString("code");requests=new JSONArray(call("GET","/rest/v1/mh_requests?select=id,sender,recipient,status&order=created_at.desc&limit=500",null,true));}
 void saveProfile(String name,String bike)throws Exception{if(name.trim().isEmpty()||name.length()>40||bike.length()>120)throw new IOException("이름은 1~40자, 바이크는 120자 이내로 입력하세요.");JSONObject body=new JSONObject().put("display_name",name.trim()).put("bike",bike.trim());if(profile.has("user_id"))call("PATCH","/rest/v1/mh_profiles?user_id=eq."+uid,body,true);else call("POST","/rest/v1/mh_profiles",body.put("user_id",uid),true);load();}
 void invite(String friendCode)throws Exception{friendCode=friendCode.trim().toLowerCase(Locale.ROOT);if(!friendCode.matches("[a-f0-9]{32}"))throw new IOException("32자리 친구 코드를 확인하세요.");String r=call("POST","/rest/v1/rpc/mh_request_friend",new JSONObject().put("friend_code",friendCode),true);if(!r.trim().equals("true"))throw new IOException("코드를 확인하거나 잠시 후 다시 시도하세요.");load();}
 void accept(String id)throws Exception{UUID.fromString(id);JSONArray changed=new JSONArray(call("PATCH","/rest/v1/mh_requests?id=eq."+id,new JSONObject().put("status","accepted"),true));if(changed.length()==0)throw new IOException("요청이 변경됐습니다. 새로고침하세요.");load();}
 void remove(String id)throws Exception{UUID.fromString(id);call("DELETE","/rest/v1/mh_requests?id=eq."+id,null,true);load();}
 void logout()throws Exception{try{if(!uid.isEmpty())call("POST","/auth/v1/logout?scope=local",null,true);}finally{clear();}}
 void clear(){if(app!=null)SessionVault.clear(app);Sharing.stop();GroupClient.INSTANCE.groups=new JSONArray();GroupClient.INSTANCE.members=new JSONArray();access="";refresh="";uid="";expires=0;code="";profile=new JSONObject();profiles=new JSONArray();requests=new JSONArray();}
 String name(String id){for(int i=0;i<profiles.length();i++){JSONObject p=profiles.optJSONObject(i);if(id.equals(p.optString("user_id")))return p.optString("display_name","라이더")+ (p.optString("bike").isEmpty()?"":" · "+p.optString("bike"));}return "라이더";}
}
