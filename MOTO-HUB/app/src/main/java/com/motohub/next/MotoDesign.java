package com.motohub.next;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.content.res.ColorStateList;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.FrameLayout;
import android.widget.Spinner;
import android.widget.TextView;

/**
 * A small, consistent native design language. No image fonts, generated assets,
 * remote styles or dependencies. Readable on both system light and dark modes.
 */
final class MotoDesign {
 private MotoDesign(){}
 // A restrained, Korean ride-app inspired palette: white, blue, and spacious cards.
 // All assets, labels and interaction behavior remain MOTO HUB originals.
 static final int DARK_BACKGROUND=0xFF101826;
 static final int DARK_SURFACE=0xFF1B2637;
 static final int DARK_STROKE=0xFF334156;
 static final int DARK_TEXT=0xFFF4F7FF;
 static final int DARK_MUTED=0xFFA8B7CC;
 static final int DARK_ACCENT=0xFF80ADFF;
 static final int DARK_TINT=0xFF243D61;
 static final int LIGHT_BACKGROUND=0xFFF7F9FE;
 static final int LIGHT_SURFACE=0xFFFFFFFF;
 static final int LIGHT_STROKE=0xFFE5ECF6;
 static final int LIGHT_TEXT=0xFF17253B;
 static final int LIGHT_MUTED=0xFF5D6D83;
 static final int LIGHT_ACCENT=0xFF1D66DC;
 static final int LIGHT_TINT=0xFFEAF2FF;
 static int[] colors(boolean dark){
  return dark?new int[]{DARK_BACKGROUND,DARK_TEXT,DARK_MUTED,DARK_STROKE,DARK_ACCENT,DARK_SURFACE,DARK_TINT}:
   new int[]{LIGHT_BACKGROUND,LIGHT_TEXT,LIGHT_MUTED,LIGHT_STROKE,LIGHT_ACCENT,LIGHT_SURFACE,LIGHT_TINT};
 }
 static Typeface medium(){return Typeface.create("sans-serif-medium",Typeface.NORMAL);}
 static Typeface display(){return Typeface.create("sans-serif",Typeface.BOLD);}
 static void ripple(MainActivity a,View view,int fill,int stroke,int radius){
  GradientDrawable background=a.shape(fill,stroke,radius);
  view.setBackground(new RippleDrawable(ColorStateList.valueOf(a.dark?0x32FFFFFF:0x16000000),background,null));
 }
 static String heading(String tab){
  switch(tab){
   case "홈":return "내 차고";
   case "차고":return "정비와 주유";
   case "매물":return "관심 매물";
   case "라이딩":return "라이딩";
   default:return "설정과 도구";
  }
 }
 static void addHeader(MainActivity a,LinearLayout root){
  LinearLayout header=new LinearLayout(a);
  header.setGravity(Gravity.CENTER_VERTICAL);
  header.setPadding(a.dp(20),a.dp(20),a.dp(20),a.dp(15));
  header.setBackgroundColor(a.panel);
  LinearLayout caption=a.col();
  TextView brand=a.text("MOTO HUB",12,a.accent,true);
  brand.setLetterSpacing(.04f);caption.addView(brand);
  TextView title=a.text(heading(a.tab),25,a.fg,true);
  title.setTypeface(display());LinearLayout.LayoutParams titleParams=new LinearLayout.LayoutParams(-1,-2);
  titleParams.topMargin=a.dp(7);caption.addView(title,titleParams);
  header.addView(caption,new LinearLayout.LayoutParams(0,-2,1));
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
  view.setMinWidth(a.dp(56));
  view.setMinHeight(a.dp(43));
  view.setPadding(a.dp(10),0,a.dp(10),0);
  ripple(a,view,a.highlight,0,16);
  view.setClickable(true);view.setFocusable(true);
  return view;
 }
 static void spinnerField(MainActivity a,LinearLayout parent,Spinner spinner,int bottomMarginDp){
  FrameLayout shell=new FrameLayout(a);
  shell.setBackground(a.shape(a.panel,a.line,17));
  spinner.setBackgroundColor(Color.TRANSPARENT);
  spinner.setPadding(a.dp(14),a.dp(8),a.dp(35),a.dp(8));
  shell.addView(spinner,new FrameLayout.LayoutParams(-1,-1));
  TextView chevron=a.text("⌄",21,a.muted,true);
  chevron.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
  chevron.setGravity(Gravity.CENTER);
  FrameLayout.LayoutParams icon=new FrameLayout.LayoutParams(a.dp(25),a.dp(35),Gravity.RIGHT|Gravity.CENTER_VERTICAL);
  icon.rightMargin=a.dp(10);shell.addView(chevron,icon);
  LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,a.dp(56));
  p.bottomMargin=a.dp(bottomMarginDp);parent.addView(shell,p);
 }
 static void addNavigation(MainActivity a,LinearLayout root){
  View rule=new View(a);rule.setBackgroundColor(a.line);
  root.addView(rule,new LinearLayout.LayoutParams(-1,a.dp(1)));
  LinearLayout bar=new LinearLayout(a);
  bar.setPadding(a.dp(10),a.dp(8),a.dp(10),a.dp(7));
  bar.setBackgroundColor(a.panel);
  String[] labels={"홈","바이크","매물","라이딩","도구"};
  final int[] icons={0,2,1,3,4};
  for(int i=0;i<a.tabs.length;i++){
   final String destination=a.tabs[i];
   final boolean selected=destination.equals(a.tab);
   LinearLayout cell=a.col();cell.setGravity(Gravity.CENTER);
   cell.setMinimumHeight(a.dp(61));
   cell.setPadding(a.dp(2),a.dp(4),a.dp(2),a.dp(3));
   LinearLayout iconShell=a.col();iconShell.setGravity(Gravity.CENTER);
   iconShell.setBackground(a.shape(selected?a.highlight:a.panel,0,15));
   iconShell.addView(new NavIcon(a,icons[i],selected?a.accent:a.muted),
    new LinearLayout.LayoutParams(a.dp(24),a.dp(24)));
   cell.addView(iconShell,new LinearLayout.LayoutParams(a.dp(52),a.dp(35)));
   TextView name=a.text(labels[i],11,selected?a.accent:a.muted,selected);
   name.setGravity(Gravity.CENTER);
   LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);
   lp.topMargin=a.dp(4);cell.addView(name,lp);
   cell.setContentDescription(labels[i]+(selected?", 선택됨":""));
   cell.setClickable(true);cell.setFocusable(true);cell.setSelected(selected);
   ripple(a,cell,a.panel,0,15);
   cell.setOnClickListener(v->a.navigate(destination));
   bar.addView(cell,new LinearLayout.LayoutParams(0,-2,1));
  }
  root.addView(bar);
 }
}
