package com.motohub.next;

import android.graphics.*;
import android.view.*;
import android.widget.*;
import java.time.*;
import org.json.JSONObject;

/** Screen-level composition primitives. Values always come from the existing local stores. */
final class ExperienceUi {
 private ExperienceUi(){}
 static boolean stacked(MainActivity a){return a.getResources().getConfiguration().screenWidthDp<340||a.getResources().getConfiguration().fontScale>=1.5f;}
 static void title(MainActivity a,String label,String detail){
  TextView title=a.text(label,32,a.fg,true);title.setLetterSpacing(-.035f);a.content.addView(title);
  TextView sub=a.text(detail,13,a.muted,false);sub.setPadding(0,a.dp(6),0,a.dp(20));a.content.addView(sub);
 }
 static LinearLayout section(MainActivity a,String label,String link,Runnable click){
  LinearLayout row=new LinearLayout(a);row.setGravity(Gravity.CENTER_VERTICAL);
  if(stacked(a))row.setOrientation(LinearLayout.VERTICAL);
  TextView name=a.text(label,19,a.fg,true);row.addView(name,stacked(a)?new LinearLayout.LayoutParams(-1,-2):new LinearLayout.LayoutParams(0,-2,1));
  if(click!=null){TextView more=a.text(link,13,a.accent,true);more.setMinHeight(a.dp(48));more.setGravity(Gravity.CENTER_VERTICAL);more.setClickable(true);more.setFocusable(true);more.setOnClickListener(v->click.run());row.addView(more);}
  a.content.addView(row);return row;
 }
 static void dock(MainActivity a,View action){
  LinearLayout dock=a.col();dock.setTag("primary-action-dock");dock.setPadding(a.dp(18),a.dp(9),a.dp(18),a.dp(9));dock.setBackgroundColor(a.bg);
  dock.addView(action,new LinearLayout.LayoutParams(-1,-2));
  // The navigation rule and navigation bar are the final two children.
  a.root.addView(dock,Math.max(0,a.root.getChildCount()-2),new LinearLayout.LayoutParams(-1,-2));
 }
 static void switcher(MainActivity a,String first,String second,boolean firstSelected,Runnable left,Runnable right){
  LinearLayout row=new LinearLayout(a);row.setTag("workspace-switcher");if(stacked(a))row.setOrientation(LinearLayout.VERTICAL);
  Button one=a.button(first,firstSelected,left),two=a.button(second,!firstSelected,right);one.setSelected(firstSelected);two.setSelected(!firstSelected);
  row.addView(one,stacked(a)?new LinearLayout.LayoutParams(-1,-2):new LinearLayout.LayoutParams(0,-2,1));
  LinearLayout.LayoutParams lp=stacked(a)?new LinearLayout.LayoutParams(-1,-2):new LinearLayout.LayoutParams(0,-2,1);if(stacked(a))lp.topMargin=a.dp(8);else lp.leftMargin=a.dp(8);row.addView(two,lp);a.content.addView(row);a.gap(a.content,12);
 }
 static LinearLayout tile(MainActivity a,String label,String detail,int icon,Runnable click){
  LinearLayout tile=a.col();tile.setTag("destination-tile:"+label);tile.setPadding(a.dp(17),a.dp(16),a.dp(17),a.dp(18));
  MotoDesign.ripple(a,tile,a.panel,a.line,22);tile.setMinimumHeight(a.dp(132));
  NavIcon glyph=new NavIcon(a,icon,a.accent);tile.addView(glyph,new LinearLayout.LayoutParams(a.dp(27),a.dp(27)));a.gap(tile,14);
  tile.addView(a.text(label,17,a.fg,true));a.gap(tile,5);tile.addView(a.text(detail,12,a.muted,false));
  tile.setClickable(true);tile.setFocusable(true);tile.setContentDescription(label+", "+detail+", 열기");tile.setOnClickListener(v->click.run());return tile;
 }
 static void pair(MainActivity a,LinearLayout host,View first,View second){
  LinearLayout row=new LinearLayout(a);if(stacked(a))row.setOrientation(LinearLayout.VERTICAL);
  row.addView(first,stacked(a)?new LinearLayout.LayoutParams(-1,-2):new LinearLayout.LayoutParams(0,-1,1));
  LinearLayout.LayoutParams lp=stacked(a)?new LinearLayout.LayoutParams(-1,-2):new LinearLayout.LayoutParams(0,-1,1);
  if(stacked(a))lp.topMargin=a.dp(10);else lp.leftMargin=a.dp(10);row.addView(second,lp);host.addView(row);a.gap(host,10);
 }
 static RecordMonthlySummary totals(MainActivity a,JSONObject bike){
  LocalDate today=LocalDate.parse(a.today());return RecordMonthlySummary.forBikeAsOf(a.items("expenses"),a.items("services"),bike.optString("id"),YearMonth.from(today),today);
 }
 static void spending(MainActivity a,JSONObject bike,boolean chart){spending(a,a.content,bike,chart);}
 static void spending(MainActivity a,LinearLayout host,JSONObject bike,boolean chart){
  RecordMonthlySummary totals=totals(a,bike);LinearLayout panel=a.col();panel.setTag("monthly-spending");
  panel.setPadding(a.dp(20),a.dp(18),a.dp(20),a.dp(18));panel.setBackground(a.shape(a.panel,a.line,24));
  panel.addView(a.text("이번 달 유지비",13,a.muted,false));a.gap(panel,7);
  TextView amount=MotoDesign.figure(a,a.won(totals.monthTotal(0)),32,a.fg);panel.addView(amount);
  a.gap(panel,8);panel.addView(a.text("주유 "+a.won(totals.fuel[0])+"  ·  정비 "+a.won(totals.service[0])+"  ·  기타 "+a.won(totals.other[0]),12,a.muted,false));
  if(chart){a.gap(panel,18);panel.addView(new SpendingChart(a,totals),new LinearLayout.LayoutParams(-1,a.dp(105)));}
  if(totals.scheduledOrFuture>0){a.gap(panel,8);panel.addView(a.text("예정·미래 지출 "+totals.scheduledOrFuture+"건 제외",12,a.muted,false));}
  panel.setClickable(true);panel.setFocusable(true);panel.setContentDescription("이번 달 유지비 "+a.won(totals.monthTotal(0))+", 바이크별 유지비 통계 열기");panel.setOnClickListener(v->new HubScreens(a).costDashboard());
  host.addView(panel);a.gap(host,18);
 }
 static final class SpendingChart extends View {
  final MainActivity a;final RecordMonthlySummary data;final Paint p=new Paint(3);
  SpendingChart(MainActivity a,RecordMonthlySummary data){super(a);this.a=a;this.data=data;StringBuilder desc=new StringBuilder("최근 6개월 유지비. ");for(int i=5;i>=0;i--)desc.append(YearMonth.parse(a.today().substring(0,7)).minusMonths(i)).append(" ").append(a.won(data.monthTotal(i))).append(". ");setContentDescription(desc);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);}
  @Override protected void onDraw(Canvas c){super.onDraw(c);float d=getResources().getDisplayMetrics().density;double max=0;for(int i=0;i<6;i++)max=Math.max(max,data.monthTotal(i));float step=getWidth()/6f,base=getHeight()-24*d;
   for(int col=0;col<6;col++){int month=5-col;float x=step*col+step*.22f,h=max>0?(float)(data.monthTotal(month)/max)*(base-5*d):0;
    p.setColor(a.line);c.drawRoundRect(x,0,x+step*.56f,base,4*d,4*d,p);p.setColor(month==0?a.accent:a.highlight);if(h>0)c.drawRoundRect(x,base-h,x+step*.56f,base,4*d,4*d,p);
    p.setColor(a.muted);p.setTextSize(11*d);p.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));p.setTextAlign(Paint.Align.CENTER);c.drawText(YearMonth.parse(a.today().substring(0,7)).minusMonths(month).getMonthValue()+"월",step*(col+.5f),getHeight()-4*d,p);
   }
  }
 }
 static final class RideConsole {
  final FrameLayout view;final TextView speed;
  RideConsole(MainActivity a,boolean running){
   view=new FrameLayout(a);view.setTag("ride-instrument");view.addView(new InstrumentFace(a),new FrameLayout.LayoutParams(-1,-1));
   LinearLayout center=a.col();center.setGravity(Gravity.CENTER);speed=MotoDesign.figure(a,"—",68,a.fg);speed.setGravity(Gravity.CENTER);speed.setMaxLines(1);speed.setAutoSizeTextTypeUniformWithConfiguration(24,68,1,android.util.TypedValue.COMPLEX_UNIT_SP);center.addView(speed,new LinearLayout.LayoutParams(-1,a.dp(96)));
   TextView unit=a.text("km/h",14,a.muted,false);unit.setGravity(Gravity.CENTER);center.addView(unit);a.gap(center,12);
   TextView state=a.text(running?"GPS 관측 속도":"기록을 시작하면 표시",12,a.muted,false);state.setGravity(Gravity.CENTER);center.addView(state);
   view.addView(center,new FrameLayout.LayoutParams(-1,-1));
  }
 }
 static final class InstrumentFace extends View {
  final MainActivity a;final Paint p=new Paint(3);InstrumentFace(MainActivity a){super(a);this.a=a;setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
  @Override protected void onDraw(Canvas c){super.onDraw(c);float d=getResources().getDisplayMetrics().density,cx=getWidth()/2f,cy=getHeight()/2f,r=Math.min(cx,cy)-12*d;p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3*d);p.setColor(a.line);c.drawArc(cx-r,cy-r,cx+r,cy+r,140,260,false,p);
   for(int i=0;i<=26;i++){double ang=Math.toRadians(140+i*10);float len=(i%5==0?11:5)*d;p.setColor(i%5==0?a.accent:a.line);p.setStrokeWidth(2*d);c.drawLine(cx+(float)Math.cos(ang)*(r-7*d),cy+(float)Math.sin(ang)*(r-7*d),cx+(float)Math.cos(ang)*(r-7*d-len),cy+(float)Math.sin(ang)*(r-7*d-len),p);}
  }
 }
}
