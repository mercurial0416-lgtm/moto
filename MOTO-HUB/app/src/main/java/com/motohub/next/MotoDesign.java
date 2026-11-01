package com.motohub.next;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.content.res.ColorStateList;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * A small, consistent native design language. No image fonts, generated assets,
 * remote styles or dependencies. Readable on both system light and dark modes.
 */
final class MotoDesign {
 private MotoDesign(){}
 static final int DARK_BACKGROUND=0xFF0F1114;
 static final int DARK_SURFACE=0xFF1B1E22;
 static final int DARK_STROKE=0xFF34383D;
 static final int DARK_TEXT=0xFFF4F5F5;
 static final int DARK_MUTED=0xFF9DA3AA;
 static final int DARK_ACCENT=0xFFFF762F;
 static final int DARK_TINT=0xFF30231C;
 static final int LIGHT_BACKGROUND=0xFFF3F4F3;
 static final int LIGHT_SURFACE=0xFFFFFFFF;
 static final int LIGHT_STROKE=0xFFDCE0E1;
 static final int LIGHT_TEXT=0xFF181A1D;
 static final int LIGHT_MUTED=0xFF656B71;
 static final int LIGHT_ACCENT=0xFFB74B1B;
 static final int LIGHT_TINT=0xFFF8EDE5;
 static int[] colors(boolean dark){
  return dark?new int[]{DARK_BACKGROUND,DARK_TEXT,DARK_MUTED,DARK_STROKE,DARK_ACCENT,DARK_SURFACE,DARK_TINT}:
   new int[]{LIGHT_BACKGROUND,LIGHT_TEXT,LIGHT_MUTED,LIGHT_STROKE,LIGHT_ACCENT,LIGHT_SURFACE,LIGHT_TINT};
 }
 static Typeface medium(){return Typeface.create("sans-serif-medium",Typeface.NORMAL);}
 static Typeface display(){return Typeface.create("sans-serif",Typeface.BOLD);}
 /** Stable-width digits keep odometers, prices and service amounts easy to scan. */
 static TextView figure(MainActivity a,String value,int size,int color){
  TextView number=a.text(value,size,color,true);
  number.setFontFeatureSettings("tnum");
  number.setLetterSpacing(-0.01f);
  return number;
 }
 static void ripple(MainActivity a,View view,int fill,int stroke,int radius){
  GradientDrawable background=a.shape(fill,stroke,radius);
  view.setBackground(new RippleDrawable(ColorStateList.valueOf(a.dark?0x32FFFFFF:0x16000000),background,null));
 }
 static String heading(String tab){
  switch(tab){
   case "홈":return "내 차고";
   case "차고":return "정비와 주유";
   case "라이딩":return "라이딩";
   default:return "설정과 도구";
  }
 }
 /** Brand is subordinate to the current task, not another large title. */
 static void addHeader(MainActivity a,LinearLayout root){
  LinearLayout header=new LinearLayout(a);
  header.setGravity(Gravity.CENTER_VERTICAL);
  header.setPadding(a.dp(20),a.dp(8),a.dp(18),a.dp(9));
  LinearLayout identity=new LinearLayout(a);
  identity.setOrientation(LinearLayout.VERTICAL);
  TextView brand=a.text("MOTO HUB",10,a.accent,true);
  brand.setLetterSpacing(.16f);
  brand.setContentDescription("MOTO HUB");
  identity.addView(brand);
  TextView title=a.text(heading(a.tab),23,a.fg,true);
  title.setTypeface(display());
  LinearLayout.LayoutParams titleLp=new LinearLayout.LayoutParams(-1,-2);
  titleLp.topMargin=a.dp(3);
  identity.addView(title,titleLp);
  header.addView(identity,new LinearLayout.LayoutParams(0,-2,1));
  if("홈".equals(a.tab)){
   TextView notice=headerAction(a,"알림");
   notice.setContentDescription("정비 알림 열기");
   notice.setOnClickListener(v->new GarageHomeUi(a).showNotices());
   header.addView(notice);
  }else if(!"더보기".equals(a.tab)){
   TextView settings=headerAction(a,"설정");
   settings.setContentDescription("설정과 도구로 이동");
   settings.setOnClickListener(v->a.navigate("더보기"));
   header.addView(settings);
  }
  root.addView(header);
 }
 static TextView headerAction(MainActivity a,String label){
  TextView view=a.text(label,13,a.fg,true);
  view.setGravity(Gravity.CENTER);
  view.setMinWidth(a.dp(48));
  view.setMinHeight(a.dp(44));
  view.setPadding(a.dp(10),0,a.dp(10),0);
  ripple(a,view,a.panel,a.line,12);
  view.setClickable(true);view.setFocusable(true);
  return view;
 }
 /** Compact bottom navigation with a real 48dp+ touch target and clear active pill. */
 static void addNavigation(MainActivity a,LinearLayout root){
  View rule=new View(a);rule.setBackgroundColor(a.line);
  root.addView(rule,new LinearLayout.LayoutParams(-1,a.dp(1)));
  LinearLayout bar=new LinearLayout(a);
  bar.setPadding(a.dp(9),a.dp(5),a.dp(9),a.dp(5));
  bar.setBackgroundColor(a.panel);
  String[] labels={"홈","라이딩","정비","설정"};
  final int[] icons={0,3,2,4};
  for(int i=0;i<a.tabs.length;i++){
   final String destination=a.tabs[i];
   final boolean selected=destination.equals(a.tab);
   LinearLayout cell=a.col();cell.setGravity(Gravity.CENTER);
   cell.setMinimumHeight(a.dp(62));
   cell.setPadding(a.dp(2),a.dp(5),a.dp(2),a.dp(4));
   // Selected tab is legible without relying on color alone.
   FrameLayout pill=new FrameLayout(a);
   pill.setBackground(a.shape(selected?a.highlight:a.panel,0,14));
   NavIcon graphic=new NavIcon(a,icons[i],selected?a.accent:a.muted);
   FrameLayout.LayoutParams iconParams=new FrameLayout.LayoutParams(a.dp(22),a.dp(22),Gravity.CENTER);
   pill.addView(graphic,iconParams);
   cell.addView(pill,new LinearLayout.LayoutParams(a.dp(52),a.dp(30)));
   TextView name=a.text(labels[i],12,selected?a.accent:a.muted,selected);
   name.setGravity(Gravity.CENTER);
   LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);
   lp.topMargin=a.dp(5);cell.addView(name,lp);
   cell.setContentDescription(labels[i]+(selected?", 현재 탭":" 탭 열기"));
   cell.setClickable(true);cell.setFocusable(true);cell.setSelected(selected);
   ripple(a,cell,a.panel,0,14);
   cell.setOnClickListener(v->{if(!destination.equals(a.tab))a.navigate(destination);});
   bar.addView(cell,new LinearLayout.LayoutParams(0,-2,1));
  }
  root.addView(bar);
 }
}
}
