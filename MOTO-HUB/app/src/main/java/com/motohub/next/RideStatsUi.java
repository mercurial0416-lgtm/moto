package com.motohub.next;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;
import android.widget.*;
import java.io.File;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.*;

/** Six-month offline chart assembled from the same completed GPS tracks as existing reports. */
final class RideStatsUi {
 static void open(MainActivity a){
  LinearLayout body=a.page("라이딩 통계 · 최근 6개월");
  a.label(body,"실제 GPS 기록 시간을 기준으로 집계합니다. 가져온 날짜나 파일 수정일은 사용하지 않아요.",13,a.muted,false);
  TextView status=a.text("GPS 기록 분석 중…",14,a.muted,false);
  body.addView(status);a.gap(body,12);
  RideBarChart chart=new RideBarChart(a);
  chart.setMinimumHeight(a.dp(260));
  body.addView(chart,new LinearLayout.LayoutParams(-1,a.dp(260)));
  TextView totals=a.text("",14,a.fg,true);body.addView(totals);
  TextView foot=a.text("통계는 GPS 계산값이며 실제 계기판 주행거리를 수정하지 않습니다.",12,a.muted,false);
  body.addView(foot);a.show();
  Dialog owner=a.dialog;
  final String selected=a.bike()==null?"":a.bike().optString("id");
  final RideStatsSeries data=new RideStatsSeries(YearMonth.now(ZoneId.of("Asia/Seoul")));
  new Thread(()->{
   try{
    long now=System.currentTimeMillis();int inspected=0;
    for(File ride:RideHistorySort.order(TrackFile.tracks(a.getFilesDir()),RideHistorySort.NEWEST)){
     if(RideService.running&&ride.getName().equals(RideService.active))continue;
     if(RideTrackMerge.mergedSource(ride)||RideTrackMerge.archived(ride))continue;
     if(!RideMileageIntegrity.validCompleted(ride))continue;
     if(!selected.isEmpty()){
      try{if(!selected.equals(RideBikeBinding.readStrict(ride)))continue;}
      catch(Exception ignored){data.excluded++;continue;}
     }
     Long t=RideHistoryLabel.startTime(ride);
     if(t==null||t<=0||t>now||!RideMonthlyReport.recent(ride,now,190))continue;
     if(inspected++>=180){data.scanned++;continue;}
     try{
      if(ride.length()>5_000_000){data.excluded++;continue;}
      long size=ride.length(),modified=ride.lastModified();
      RideInsights.Result x=RideInsights.analyze(ride);
      if(ride.length()!=size||ride.lastModified()!=modified){data.excluded++;continue;}
      data.add(t,x.distanceKm,x.durationMillis);
     }catch(Exception ignored){data.excluded++;}
    }
   }catch(Exception e){data.excluded++;}
   a.runOnUiThread(()->{
    if(a.isFinishing()||a.isDestroyed()||a.dialog!=owner||!owner.isShowing())return;
    status.setText(data.rides()==0?"최근 6개월에 분석 가능한 라이딩이 없어요.":"GPS 주행거리 · 월별");
    chart.setData(data,a.dark);
    totals.setText(String.format(Locale.KOREA,
     "6개월 %d회 · %.1f km  |  월 평균 %.1f km",
     data.rides(),data.totalKm,data.totalKm/6));
    foot.setText("바이크 "+(selected.isEmpty()?"전체":a.bikeName(selected))+
      " · 손상·기준 초과 제외 "+data.excluded+"건"+
      (data.scanned>0?" · 처리 제한으로 제외 "+data.scanned+"건":"")+
      "\nGPS 추정값이며 실주행거리·계기판 ODO를 자동 수정하지 않습니다.");
   });
  },"moto-ride-6month-bars").start();
 }
 static final class RideBarChart extends View{
  RideStatsSeries data;boolean dark;final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
  RideBarChart(Context c){super(c);}
  void setData(RideStatsSeries d,boolean isDark){data=d;dark=isDark;invalidate();}
  @Override protected void onDraw(Canvas c){
   super.onDraw(c);
   int w=getWidth(),h=getHeight();if(w<=0||h<=0)return;
   c.drawColor(dark?0xff1b2637:Color.WHITE);
   if(data==null)return;
   float scale=getResources().getDisplayMetrics().scaledDensity;
   float left=22*scale,right=w-15*scale,top=30*scale,bottom=h-43*scale;
   if(right<=left||bottom<=top)return;
   float col=(right-left)/6f;
   double max=Math.max(1,data.maxKm());
   paint.setTextAlign(Paint.Align.CENTER);paint.setTypeface(android.graphics.Typeface.DEFAULT);
   for(int i=0;i<6;i++){
    float cx=left+col*(i+.5f);
    float ratio=(float)Math.max(0,Math.min(1,data.km[i]/max));
    float bh=(bottom-top)*ratio;
    paint.setColor(dark?0xff8ab4ff:0xff2870e2);
    float half=col*.31f;
    c.drawRoundRect(cx-half,bottom-bh,cx+half,bottom,7*scale,7*scale,paint);
    paint.setColor(dark?Color.WHITE:0xff20344e);paint.setTextSize(11*scale);
    c.drawText(String.format(Locale.KOREA,"%.0f",data.km[i]),cx,Math.max(top+8*scale,bottom-bh-7*scale),paint);
    paint.setColor(dark?0xffb5c3db:0xff52647d);paint.setTextSize(12*scale);
    c.drawText(data.labels[i],cx,bottom+24*scale,paint);
   }
   paint.setColor(dark?0xffaab9cf:0xff66788e);paint.setTextSize(10*scale);paint.setTextAlign(Paint.Align.LEFT);
   c.drawText("km",12*scale,17*scale,paint);
   setContentDescription("월별 GPS 주행거리 "+Arrays.toString(data.labels)+" "+Arrays.toString(data.km)+" km");
  }
 }
}
