package com.motohub.next;
import android.app.*;import android.content.*;import android.text.InputType;import android.widget.*;import java.util.concurrent.*;import java.util.concurrent.atomic.AtomicBoolean;import org.json.*;
final class SocialUi {
 static final ExecutorService EXEC=Executors.newSingleThreadExecutor();static final AtomicBoolean BUSY=new AtomicBoolean();final MainActivity a;final SocialClient api=SocialClient.INSTANCE;
 SocialUi(MainActivity a){this.a=a;}
 void run(MainActivity.Work work){if(!BUSY.compareAndSet(false,true)){a.toast("요청 처리 중입니다.");return;}a.toast("서버에 요청하고 있어요.");EXEC.execute(()->{String error=null;try{work.run();}catch(Exception e){error=e.getMessage();}finally{BUSY.set(false);}final String message=error;a.runOnUiThread(()->{if(a.isFinishing()||a.isDestroyed())return;if(message!=null)a.error(message);if(a.tab.equals("지도"))a.render();});});}
 void render(){a.heading("친구와 함께","그룹 라이딩 · 친구 · 위치 공유");
  if(!SocialClient.configured())a.note("친구 서버 연결이 필요해요. 아래에서 연결한 뒤 로그인해 주세요.");
  else a.note(Sharing.enabled?"● 내 위치를 공유하는 중 · 상단에서 언제든 중지 가능":"● 위치 공유 꺼짐  ·  라이딩 그룹 안에서 직접 켤 수 있어요.");
  if(!SocialClient.configured())a.action(a.content,"친구 서버 연결",true,this::configure);
  if(api.uid.isEmpty()){a.section("라이더 계정");a.action(a.content,"로그인하고 친구 찾기  →",true,()->auth(false));a.action(a.content,"처음이라면 회원가입",false,()->auth(true));a.label(a.content,"차고·정비·개인 GPS 기록은 로그인 없이 계속 사용할 수 있어요.",14,a.muted,false);return;}
  a.action(a.content,"그룹 라이딩 열기  →",true,()->new GroupUi(a).launch());
  a.section("내 프로필");a.row(api.profile.optString("display_name","프로필 등록"),api.profile.optString("bike","보유 바이크 미설정"),"수정",this::profile);
  if(!api.code.isEmpty())a.pair(a.content,"내 코드 복사",()->{a.getSystemService(android.content.ClipboardManager.class).setPrimaryClip(ClipData.newPlainText("MOTO HUB 친구 코드",api.code));a.toast("친구 코드를 복사했어요.");},"코드 공유",()->{Intent i=new Intent(Intent.ACTION_SEND);i.setType("text/plain");i.putExtra(Intent.EXTRA_TEXT,"MOTO HUB 친구 코드: "+api.code);a.startActivity(Intent.createChooser(i,"친구 코드 공유"));});
  a.pair(a.content,"＋ 친구 추가",this::invite,"새로고침",()->run(api::load));a.section("받은 요청");int received=0;for(int i=0;i<api.requests.length();i++){JSONObject r=api.requests.optJSONObject(i);if(r.optString("status").equals("pending")&&r.optString("recipient").equals(api.uid)){received++;a.label(a.content,api.name(r.optString("sender")),18,a.fg,true);a.pair(a.content,"수락",()->run(()->api.accept(r.getString("id"))),"거절",()->run(()->api.remove(r.getString("id"))));}}if(received==0)a.label(a.content,"받은 요청이 없어요.",16,a.muted,false);
  a.section("친구 목록");int friends=0;for(int i=0;i<api.requests.length();i++){JSONObject r=api.requests.optJSONObject(i);String other=r.optString("sender").equals(api.uid)?r.optString("recipient"):r.optString("sender");if(r.optString("status").equals("accepted")){friends++;a.row(api.name(other),"공유 여부는 그룹 라이딩에서 확인","관리",()->new AlertDialog.Builder(a).setTitle("친구를 삭제할까요?").setNegativeButton("취소",null).setPositiveButton("삭제",(d,w)->run(()->api.remove(r.getString("id")))).show());}else if(r.optString("sender").equals(api.uid))a.row(api.name(other),"요청 보냄","취소",()->run(()->api.remove(r.getString("id"))));}if(friends==0)a.label(a.content,"수락된 친구가 없어요.",16,a.muted,false);
  a.action(a.content,"로그아웃",false,()->new AlertDialog.Builder(a).setTitle("로그아웃할까요?")
   .setMessage("그룹 위치 공유가 켜져 있다면 즉시 중단합니다. 개인 주행·정비·주유 기록은 기기에 그대로 남습니다.")
   .setNegativeButton("취소",null).setPositiveButton("로그아웃",(d,w)->{Sharing.stop();run(()->{Sharing.awaitEnd(8000);api.logout();});}).show());a.label(a.content,"로그인 세션은 기기 암호화 저장소에 보관됩니다. 그룹 위치 공유는 매번 직접 켜야 하며 앱 재시작 시 자동으로 켜지지 않아요.",14,a.muted,false);
 }
 EditText input(LinearLayout p,String title,String value,int type){a.label(p,title,16,a.fg,true);EditText e=new EditText(a);e.setText(value);e.setTextSize(18);e.setTextColor(a.fg);e.setSingleLine();e.setInputType(type);e.setMinHeight(a.dp(52));e.setSaveEnabled(false);e.setPadding(a.dp(14),a.dp(10),a.dp(14),a.dp(10));e.setBackground(a.shape(a.panel,a.line,13));p.addView(e);a.gap(p,16);return e;}
 void auth(boolean signup){LinearLayout p=a.page(signup?"회원가입":"로그인");EditText email=input(p,"이메일","",InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS),password=input(p,"비밀번호 (8자 이상)","",InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);a.action(p,signup?"가입하기":"로그인",true,()->{String e=email.getText().toString(),pw=password.getText().toString();password.setText("");a.close();run(()->{if(signup){String msg=api.signup(e,pw);a.runOnUiThread(()->{if(!a.isFinishing())a.toast(msg);});}else api.login(e,pw);});});a.show();}
 void profile(){LinearLayout p=a.page("내 프로필");EditText name=input(p,"이름",api.profile.optString("display_name"),InputType.TYPE_CLASS_TEXT),bike=input(p,"보유 바이크",(api.profile.optString("bike").isEmpty()&&a.bike()!=null?a.bike().optString("name"):api.profile.optString("bike")),InputType.TYPE_CLASS_TEXT);a.action(p,"저장",true,()->{String n=name.getText().toString(),b=bike.getText().toString();a.close();run(()->api.saveProfile(n,b));});a.show();}
 void invite(){LinearLayout p=a.page("친구 코드 입력");EditText code=input(p,"친구에게 받은 32자리 코드","",InputType.TYPE_CLASS_TEXT);
  a.action(p,"클립보드에서 코드 붙여넣기",false,()->{
   android.content.ClipboardManager clipboard=a.getSystemService(android.content.ClipboardManager.class);
   if(!clipboard.hasPrimaryClip()){a.toast("복사한 코드가 없어요.");return;}
   ClipData clip=clipboard.getPrimaryClip();String text=clip==null?"":clip.getItemAt(0).coerceToText(a).toString();
   java.util.regex.Matcher match=java.util.regex.Pattern.compile("(?i)[a-f0-9]{32}").matcher(text);
   if(match.find())code.setText(match.group().toLowerCase(java.util.Locale.ROOT));else a.toast("32자리 코드를 찾지 못했어요.");
  });a.action(p,"친구 요청 보내기",true,()->{String value=code.getText().toString();a.close();run(()->api.invite(value));});a.show();}
 void configure(){
  LinearLayout p=a.page("친구 서버 연결");
  a.label(p,"Supabase 프로젝트 URL과 sb_publishable_ 공개키를 입력하세요. 로그인 전에도 설정할 수 있어요. 서비스 관리자키/secret 키는 입력하지 마세요.",15,a.muted,false);
  EditText url=input(p,"프로젝트 URL",SocialClient.serverUrl(),InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_URI);
  EditText key=input(p,"공개키 (sb_publishable_...)",SocialClient.publicKey(),InputType.TYPE_CLASS_TEXT);
  key.setHorizontallyScrolling(true);
  a.action(p,"서버 설정 저장",true,()->a.attempt(()->{
   SocialClient.saveSettings(a,url.getText().toString(),key.getText().toString());
   a.close();a.render();a.toast("연결 정보를 저장했어요. 로그인해 주세요.");
  }));
  a.action(p,"서버 설정 지우기",false,()->new AlertDialog.Builder(a)
   .setTitle("친구 서버 설정을 지울까요?")
   .setMessage("바이크·정비 등 로컬 기록은 유지됩니다. 연결 계정은 로그아웃한 후 변경하세요.")
   .setNegativeButton("취소",null)
   .setPositiveButton("지우기",(d,w)->a.attempt(()->{SocialClient.resetSettings(a);a.close();a.render();}))
   .show());
  a.show();
 }

}
