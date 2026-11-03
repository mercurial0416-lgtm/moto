package com.motohub.next;

import android.app.Dialog;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.*;
import android.view.View;
import java.io.File;
import java.util.*;
import org.json.JSONObject;

/** Read-only all-in-one activity feed: no extra database, no automatic ODO. */
final class MotoTimelineUi {
 private final MainActivity a;
 MotoTimelineUi(MainActivity a){this.a=a;}
 void open(){
  JSONObject active=a.bike();
  if(active==null){a.toast("바이크를 먼저 등록해 주세요.");return;}
  final String bikeId=active.optString("id");
  final java.time.ZoneId localZone=java.time.ZoneId.systemDefault();
  final JSONObject snapshot;
  try{snapshot=new JSONObject(a.db.toString());}
  catch(Exception e){a.error("현재 바이크 기록을 읽을 수 없습니다.");return;}
  LinearLayout body=a.page("바이크 활동");
  a.label(body,active.optString("name")+" · 라이딩 / 정비 / 주유 / 비용",14,a.fg,true);
  a.label(body,"휴대폰 현지 시간대("+localZone+")로 GPS 날짜와 정비·주유 날짜를 함께 표시합니다. 계기판 km는 자동 수정하지 않습니다.",12,a.muted,false);
  a.action(body,"판매·정비 이력 PDF 내보내기",false,()->a.exportOwnershipPdf(bikeId));
  a.gap(body,10);
  HorizontalScrollView scroll=new HorizontalScrollView(a);
  scroll.setHorizontalScrollBarEnabled(false);
  LinearLayout filters=new LinearLayout(a);
  final Map<String,Button> chips=new LinkedHashMap<>();
  final String[] selected={MotoTimeline.ALL};
  final List<MotoTimeline.Event> events=new ArrayList<>();
  final boolean[] loaded={false};
  final int[] visibleLimit={40};
  LinearLayout entries=a.col();
  TextView summary=a.text("이 바이크의 기록을 확인하고 있어요…",13,a.muted,false);
  TextView invalid=a.text("",12,a.muted,false);
  final Runnable[] repaintHolder=new Runnable[1];
  Runnable repaint=()->{
   for(Map.Entry<String,Button> chip:chips.entrySet()){
    boolean on=chip.getKey().equals(selected[0]);
    chip.getValue().setTextColor(on?a.accent:a.fg);
    MotoDesign.ripple(a,chip.getValue(),on?a.highlight:a.panel,on?a.accent:a.line,13);
   }
   if(!loaded[0])return;
   entries.removeAllViews();
   int count=0;
   for(MotoTimeline.Event e:events)if(e.matches(selected[0]))count++;
   summary.setText(selected[0]+" · "+count+"건"+
    (count>visibleLimit[0]?" · "+visibleLimit[0]+"건 표시":""));
   if(count==0){a.label(entries,"이 분류에 표시할 기록이 없습니다.",14,a.muted,false);return;}
   String lastMonth="";
   for(MotoTimeline.Event e:MotoTimeline.filtered(events,selected[0],visibleLimit[0])){
    String month=e.day.length()>=7?e.day.substring(0,7):"";
    if(!lastMonth.equals(month)){
     lastMonth=month;
     TextView group=a.text(month,13,a.muted,true);
     LinearLayout.LayoutParams mp=new LinearLayout.LayoutParams(-1,-2);
     mp.topMargin=a.dp(14);mp.bottomMargin=a.dp(8);
     entries.addView(group,mp);
    }
    LinearLayout card=a.col();card.setPadding(a.dp(14),a.dp(12),a.dp(14),a.dp(12));
    card.setMinimumHeight(a.dp(72));
    card.setBackground(a.shape(a.panel,a.line,13));
    LinearLayout heading=new LinearLayout(a);heading.setGravity(Gravity.CENTER_VERTICAL);
    TextView label=a.text(e.kind+"  ·  "+e.title,14,a.fg,true);
    label.setMaxLines(2);heading.addView(label,new LinearLayout.LayoutParams(0,-2,1));
    TextView date=a.text(e.day,11,a.muted,false);heading.addView(date);
    card.addView(heading);
    TextView sub=a.text(e.detail,12,a.muted,false);
    sub.setMaxLines(3);LinearLayout.LayoutParams dp=new LinearLayout.LayoutParams(-1,-2);
    dp.topMargin=a.dp(6);card.addView(sub,dp);
    card.setClickable(true);card.setFocusable(true);
    card.setContentDescription(e.day+" "+e.kind+" "+e.title+" 상세 보기");
    card.setOnClickListener(v->openEvent(e));
    LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);
    cp.bottomMargin=a.dp(8);entries.addView(card,cp);
   }
   final int totalMatches=count;
   final int remaining=Math.max(0,totalMatches-visibleLimit[0]);
   if(remaining>0){
    a.gap(entries,8);
    Button more=a.button("활동 더 보기 · 남은 "+remaining+"건",false,()->{
     visibleLimit[0]=Math.min(totalMatches,visibleLimit[0]+40);
     repaintHolder[0].run();
    });
    more.setMinHeight(a.dp(48));
    more.setContentDescription("바이크 활동 "+Math.min(40,remaining)+"건 더 보기");
    entries.addView(more);
   }
  };
  repaintHolder[0]=repaint;
  for(String kind:MotoTimeline.FILTERS){
   Button item=a.button(kind,false,()->{selected[0]=kind;visibleLimit[0]=40;repaint.run();});
   item.setMinHeight(a.dp(44));item.setTextSize(13);item.setContentDescription(kind+" 활동 필터");
   LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,-2);lp.rightMargin=a.dp(7);
   filters.addView(item,lp);chips.put(kind,item);
  }
  scroll.addView(filters);body.addView(scroll);
  a.gap(body,12);body.addView(summary);body.addView(invalid);a.gap(body,8);body.addView(entries);
  a.show();repaint.run();
  final Dialog opened=a.dialog;
  new Thread(()->{
   List<MotoTimeline.Event> merged=new ArrayList<>(MotoTimeline.ledger(snapshot,bikeId,localZone));
   int skipped=0,inspected=0;
   try{
    for(File track:RideHistorySort.order(TrackFile.tracks(a.getFilesDir()),RideHistorySort.NEWEST)){
     if(++inspected%64==0&&(a.isFinishing()||a.isDestroyed()||
        a.dialog!=opened||!opened.isShowing()))return;
     if(RideService.running&&track.getName().equals(RideService.active))continue;
     try{
      MotoTimeline.Event e=MotoTimeline.ride(track,snapshot,bikeId,localZone);
      if(e!=null)merged.add(e);
     }catch(Exception ignored){skipped++;}
    }
   }catch(Exception ignored){skipped++;}
   MotoTimeline.sort(merged);
   final int exclusions=skipped;
   a.runOnUiThread(()->{
    if(a.isFinishing()||a.isDestroyed()||a.dialog!=opened||!opened.isShowing())return;
    events.addAll(merged);loaded[0]=true;repaint.run();
    invalid.setText(exclusions>0?"손상되거나 연결을 확인할 수 없는 GPS 기록 "+exclusions+"건 제외":"");
   });
  },"moto-unified-timeline").start();
 }
 private void openEvent(MotoTimeline.Event e){
  a.close();
  if(!e.trackName.isEmpty()){
   a.attempt(()->{
    if(!e.trackName.matches(BackupFormat.TRACK))throw new java.io.IOException("잘못된 GPS 파일입니다.");
    File track=new File(TrackFile.directory(a.getFilesDir()),e.trackName);
    if(!RideMileageIntegrity.validCompleted(track))throw new java.io.IOException("GPS 원본이 변경됐습니다.");
    a.openRideInsights(track);
   });
   return;
  }
  a.attempt(()->{
   JSONObject item=Store.find(a.db,e.collection,e.recordId);
   if(item==null)throw new java.io.IOException("이 기록이 변경되거나 삭제됐어요.");
   a.edit(e.collection,item);
  });
 }
}
