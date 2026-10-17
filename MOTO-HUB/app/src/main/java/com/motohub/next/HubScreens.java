package com.motohub.next;
import android.app.AlertDialog;import android.graphics.Color;import android.view.*;import android.widget.*;import org.json.*;import java.util.*;
/** Screen composition only. Existing Store, forms and AtomicFile persistence are reused. */
final class HubScreens {
 final MainActivity a;
 HubScreens(MainActivity activity){a=activity;}
 void title(String name,String detail){a.heading(name,detail);}
 void go(String tab){a.navigate(tab);}
 void empty(String title,String description,String action,Runnable click){a.gap(a.content,16);a.label(a.content,title,22,a.fg,true);a.label(a.content,description,16,a.muted,false);a.gap(a.content,12);a.action(a.content,action,true,click);}
 void line(String title,String detail,String value,Runnable click){line(a.content,title,detail,value,click);}
 void line(LinearLayout target,String title,String detail,String value,Runnable click){
  LinearLayout block=a.col();block.setPadding(a.dp(2),a.dp(15),a.dp(2),a.dp(15));block.setMinimumHeight(a.dp(66));
  LinearLayout head=new LinearLayout(a);head.setGravity(Gravity.CENTER_VERTICAL);TextView label=a.text(title,17,a.fg,true);head.addView(label,new LinearLayout.LayoutParams(0,-2,1));
  if(!value.isEmpty()){TextView hint=a.text(value,14,a.accent,true);hint.setGravity(Gravity.END);hint.setPadding(a.dp(8),0,0,0);head.addView(hint);}block.addView(head);
  if(!detail.isEmpty()){TextView summary=a.text(detail,14,a.muted,false);summary.setLineSpacing(a.dp(2),1f);LinearLayout.LayoutParams sl=new LinearLayout.LayoutParams(-1,-2);sl.topMargin=a.dp(7);block.addView(summary,sl);}
  if(click!=null){block.setFocusable(true);block.setClickable(true);android.util.TypedValue out=new android.util.TypedValue();a.getTheme().resolveAttribute(android.R.attr.selectableItemBackground,out,true);block.setBackgroundResource(out.resourceId);block.setOnClickListener(v->click.run());block.setContentDescription(title+", "+detail+", "+value);}
  target.addView(block);View rule=new View(a);rule.setBackgroundColor(a.line);target.addView(rule,new LinearLayout.LayoutParams(-1,a.dp(1)));
 }
 void bikePicker(){JSONObject b=a.bike();if(b==null)return;Button picker=a.button(b.optString("name")+"  ▾",false,this::chooseBike);picker.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);a.content.addView(picker,new LinearLayout.LayoutParams(-1,-2));a.gap(a.content,12);}
 void chooseBike(){List<JSONObject> list=a.items("vehicles");String[] options=new String[list.size()+1];for(int i=0;i<list.size();i++)options[i]=list.get(i).optString("name");options[list.size()]="＋ 바이크 추가";new AlertDialog.Builder(a).setTitle("내 바이크").setItems(options,(d,n)->{if(n==list.size()){a.edit("vehicles",null);return;}a.selected=list.get(n).optString("id");a.render();}).show();}
 // Signature screen: one strong garage hero, then one-tap actions and clear records.
 void home(){new GarageHomeUi(a).render();}
 LinearLayout quickTile(String name,String detail,Runnable click){
  LinearLayout tile=a.col();tile.setGravity(Gravity.CENTER_VERTICAL);tile.setPadding(a.dp(13),a.dp(17),a.dp(9),a.dp(16));tile.setMinimumHeight(a.dp(86));tile.setBackground(a.shape(a.panel,0,15));
  TextView t=a.text(name,17,a.fg,true);tile.addView(t);TextView d=a.text(detail,12,a.accent,true);LinearLayout.LayoutParams dp=new LinearLayout.LayoutParams(-1,-2);dp.topMargin=a.dp(9);tile.addView(d,dp);
  tile.setClickable(true);tile.setFocusable(true);tile.setContentDescription(name+" "+detail);tile.setOnClickListener(v->click.run());return tile;
 }
 List<JSONObject> records(String mode,JSONObject bike){List<JSONObject> result=new ArrayList<>();for(String key:new String[]{"services","expenses"}){if(mode.equals("정비")&&!key.equals("services")||mode.equals("주유·비용")&&!key.equals("expenses"))continue;for(JSONObject x:a.items(key))if(x.optString("vehicleId").equals(bike.optString("id")))result.add(x);}result.sort((x,y)->y.optString("date").compareTo(x.optString("date")));return result;}
 void records(){title("정비와 주유","날짜별 기록 · 바이크별 관리");if(a.bike()==null){empty("기록할 바이크가 없어요","먼저 바이크를 등록해 주세요.","바이크 등록",()->a.edit("vehicles",null));return;}bikePicker();tabs(new String[]{"전체","정비","주유·비용"},a.garageMode,v->{a.garageMode=v;a.render();});List<JSONObject> list=records(a.garageMode,a.bike());if(list.isEmpty())a.note("아직 기록이 없어요. 아래 기록 추가 버튼을 이용해 주세요.");String last="";for(JSONObject x:list){String month=x.optString("date");month=month.length()>=7?month.substring(0,7):"날짜 없음";if(!month.equals(last)){a.section(month);last=month;}line(x.optString("type"),x.optString("date")+" · "+a.num(x.optDouble("mileage"))+" km",a.won(x.optDouble(x.has("amount")?"amount":"cost")),()->a.edit(x.has("amount")?"expenses":"services",x));}}
 void tabs(String[] values,String current,MainActivity.Choice select){HorizontalScrollView sc=new HorizontalScrollView(a);sc.setHorizontalScrollBarEnabled(false);LinearLayout group=new LinearLayout(a);for(String value:values){Button b=a.button(value,current.equals(value),()->select.pick(value));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,-2);lp.rightMargin=a.dp(8);group.addView(b,lp);}sc.addView(group);a.content.addView(sc);a.gap(a.content,12);}
 void manageBike(){JSONObject b=a.bike();if(b==null){a.edit("vehicles",null);return;}LinearLayout p=a.page("내 바이크 관리");a.label(p,b.optString("name"),25,a.fg,true);a.label(p,b.optInt("year")+"년식 · "+a.num(b.optDouble("odo"))+" km",17,a.muted,false);a.label(p,"구매가 "+a.won(b.optDouble("purchase")),17,a.fg,false);a.action(p,"바이크 정보 수정",true,()->a.edit("vehicles",b));a.action(p,"대표 바이크로 설정",false,()->a.attempt(()->{JSONObject next=new JSONObject(a.db.toString());next.getJSONObject("settings").put("primary",b.getString("id"));a.write(next);a.selected="";a.close();a.render();}));a.action(p,"다른 바이크 추가",false,()->a.edit("vehicles",null));a.divider(p);a.action(p,"바이크와 관련 기록 삭제",false,()->{a.close();a.remove("vehicles",b);});a.show();}
 void listings(){
  title("관심 매물","찾고, 저장하고, 세 대까지 비교하기");
  EditText search=new EditText(a);search.setSingleLine();search.setTextSize(16);search.setTextColor(a.fg);search.setHintTextColor(a.muted);search.setHint("모델명으로 빠르게 찾기");search.setText(a.query);search.setContentDescription("저장한 매물 검색");search.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);
  search.setPadding(a.dp(16),a.dp(12),a.dp(16),a.dp(12));search.setBackground(a.shape(a.panel,0,14));a.content.addView(search,new LinearLayout.LayoutParams(-1,a.dp(53)));
  a.gap(a.content,13);tabs(new String[]{"전체","확인필요","판매중","예약중","판매완료"},a.filter,v->{a.filter=v;a.render();});
  LinearLayout toolbar=new LinearLayout(a);toolbar.setGravity(Gravity.CENTER_VERTICAL);TextView label=a.text("저장한 매물",17,a.fg,true);toolbar.addView(label,new LinearLayout.LayoutParams(0,-2,1));Button order=a.button(a.sort+"  ▾",false,()->a.choose("정렬",new String[]{"최신순","가격 낮은순","주행거리순"},v->{a.sort=v;a.render();}));order.setMinHeight(a.dp(42));order.setTextSize(13);toolbar.addView(order);a.content.addView(toolbar);
  LinearLayout results=a.col();a.content.addView(results);
  search.setOnEditorActionListener((v,id,event)->{a.hideKeyboard();return true;});
  search.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int x,int c,int d){}public void onTextChanged(CharSequence s,int x,int before,int count){a.query=s.toString().trim();showListingResults(results);}public void afterTextChanged(android.text.Editable e){}});
  showListingResults(results);
 }
 void showListingResults(LinearLayout into){
  into.removeAllViews();List<JSONObject> list=a.items("listings");list.removeIf(x->!x.optString("model").toLowerCase(Locale.ROOT).contains(a.query.toLowerCase(Locale.ROOT))||!a.filter.equals("전체")&&!a.filter.equals(x.optString("status","확인필요")));
  if(a.sort.equals("가격 낮은순"))list.sort(Comparator.comparingDouble(x->x.optDouble("price")));if(a.sort.equals("주행거리순"))list.sort(Comparator.comparingDouble(x->x.optDouble("mileage")));
  a.gap(into,12);a.label(into,list.size()+"개 매물",13,a.muted,false);
  if(list.isEmpty()){TextView none=a.text("조건에 맞는 매물이 없어.\n필터를 바꾸거나 매물을 추가해 봐.",15,a.muted,false);none.setPadding(a.dp(18),a.dp(25),a.dp(18),a.dp(25));none.setBackground(a.shape(a.panel,0,16));into.addView(none);return;}
  for(JSONObject x:list){
   LinearLayout card=a.col();card.setPadding(a.dp(16),a.dp(17),a.dp(16),a.dp(10));card.setBackground(a.shape(a.panel,0,17));
   LinearLayout head=new LinearLayout(a);head.setGravity(Gravity.CENTER_VERTICAL);TextView model=a.text(x.optString("model"),19,a.fg,true);head.addView(model,new LinearLayout.LayoutParams(0,-2,1));TextView status=a.text(x.optString("status","확인필요"),12,a.accent,true);status.setPadding(a.dp(8),a.dp(5),a.dp(8),a.dp(5));status.setBackground(a.shape(a.highlight,0,9));head.addView(status);card.addView(head);
   TextView price=a.text(a.won(x.optDouble("price")),25,a.fg,true);LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(-1,-2);pp.topMargin=a.dp(11);card.addView(price,pp);
   TextView specs=a.text(x.optInt("year")+"년식  ·  "+a.num(x.optDouble("mileage"))+" km  ·  "+x.optString("region","지역 미입력"),13,a.muted,false);LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,-2);sp.topMargin=a.dp(9);card.addView(specs,sp);
   card.setClickable(true);card.setFocusable(true);card.setContentDescription(x.optString("model")+", "+a.won(x.optDouble("price"))+". 자세히 보기");card.setOnClickListener(v->a.detail(x));
   CheckBox check=new CheckBox(a);check.setText("비교에 담기");check.setTextSize(13);check.setTextColor(a.muted);check.setMinHeight(a.dp(44));check.setChecked(a.compare.contains(x.optString("id")));check.setOnCheckedChangeListener((v,on)->{String id=x.optString("id");if(on&&a.compare.size()>=3){check.setChecked(false);a.toast("최대 3개까지 비교할 수 있어요.");return;}if(on)a.compare.add(id);else a.compare.remove(id);});
   card.addView(check);LinearLayout.LayoutParams cr=new LinearLayout.LayoutParams(-1,-2);cr.bottomMargin=a.dp(11);into.addView(card,cr);
  }
 }
 void compare(){if(a.compare.isEmpty()){a.toast("비교할 매물을 선택해 주세요.");return;}LinearLayout p=a.page("매물 비교");for(JSONObject x:a.items("listings"))if(a.compare.contains(x.optString("id"))){a.label(p,x.optString("model"),22,a.fg,true);a.label(p,a.won(x.optDouble("price")),26,a.accent,true);a.label(p,x.optInt("year")+"년식 · "+a.num(x.optDouble("mileage"))+" km",17,a.fg,false);a.label(p,"퀵시프터 "+x.optString("quick","미확인")+" · "+x.optString("status","확인필요"),16,a.muted,false);a.divider(p);}a.show();}
 void settings(){title("설정과 도구","내 기록은 로그인 없이 기기에 보관됩니다");line("백업 · 복원","JSON 내보내기와 기존 데이터 이전","열기",()->{LinearLayout p=a.page("백업 · 복원");a.label(p,"ZIP 하나로 바이크·정비·주유·GPS 원본·사진을 백업하고 복원할 수 있어요. 복원은 현재 기록을 교체하므로 기존 기록이 필요하다면 먼저 내보내세요.",16,a.muted,false);a.action(p,"전체 ZIP 백업 복원",true,a::importBundle);a.action(p,"모든 기록 ZIP으로 내보내기",false,a::exportBundle);a.action(p,"JSON 백업만 내보내기",false,a::exportData);a.action(p,"JSON 백업 가져오기",false,()->{android.content.Intent i=new android.content.Intent(android.content.Intent.ACTION_OPEN_DOCUMENT);i.setType("*/*");i.addCategory(android.content.Intent.CATEGORY_OPENABLE);a.startActivityForResult(i,42);});a.action(p,"이전 JSON 되돌리기 (사진·GPS 제외)",false,()->a.attempt(()->{android.util.AtomicFile f=new android.util.AtomicFile(new java.io.File(a.getFilesDir(),"before-import.json"));a.confirmImport(Store.parse(new String(f.readFully(),java.nio.charset.StandardCharsets.UTF_8)));}));a.show();});
  line("친구 서버 고급 설정","주소·공개키 변경 (필요할 때만)","열기",()->new SocialUi(a).configure());
  line("기변 계산기","구매·정비·등록 비용과 판매 예상액","열기",a::tradeIn);
  line("화면 테마","현재: "+a.settings().optString("theme","dark"),"변경",()->a.choose("화면 테마",new String[]{"system","light","dark"},v->a.attempt(()->{JSONObject next=new JSONObject(a.db.toString());next.getJSONObject("settings").put("theme",v);a.write(next);a.render();})));
  line("유지비 통계","월별 비용 합계","열기",()->{LinearLayout p=a.page("월별 유지비");TreeSet<String> months=new TreeSet<>(Comparator.reverseOrder());for(String k:new String[]{"services","expenses"})for(JSONObject x:a.items(k))if(x.optString("date").length()>=7)months.add(x.optString("date").substring(0,7));if(months.isEmpty())a.label(p,"저장한 비용 기록이 없어요.",17,a.muted,false);for(String m:months)a.attempt(()->a.label(p,m+" · "+a.won(Store.monthly(a.db,m)),20,a.fg,true));a.show();});
  line("보호장구","장비와 교체 메모","열기",()->{LinearLayout p=a.page("보호장구");a.action(p,"장구 추가",true,()->a.edit("gear",null));for(JSONObject x:a.items("gear"))a.action(p,x.optString("name"),false,()->a.edit("gear",x));a.show();});line("앱 업데이트",BuildConfig.UPDATE_MANIFEST_URL.isEmpty()?"서버 미연동":"최신 버전 확인과 설치","열기",()->new AppUpdater(a).show());a.section("앱 정보");a.label(a.content,"MOTO HUB 4.0 Alpha6 N · 라이딩 분석 · GPX 가져오기 · 암호화 로그인",16,a.muted,false);a.label(a.content,"앱 삭제 전 전체 ZIP 백업을 저장하세요. GPS는 기기 내 기록입니다. 친구 서버는 공개 URL·공개키로 설정돼 있으나 실기기 검증은 아직 필요합니다.",16,a.muted,false);
 }
}
