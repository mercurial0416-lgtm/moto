package com.motohub.next;

import android.text.*;
import android.view.*;
import android.widget.*;

/** Reusable, local-only feedback for the everyday entry forms. */
final class EntryAssist {
 private EntryAssist(){}
 static TextWatcher watch(Runnable update){return new TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int count,int after){}public void onTextChanged(CharSequence s,int start,int before,int count){update.run();}public void afterTextChanged(Editable s){}};}
 static TextView preview(MainActivity a,LinearLayout parent,String tag){
  TextView view=a.text("",17,a.fg,true);view.setTag(tag);view.setPadding(a.dp(16),a.dp(15),a.dp(16),a.dp(15));view.setBackground(a.shape(a.highlight,0,18));
  view.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
  LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=a.dp(14);parent.addView(view,lp);return view;
 }
 static void distanceChips(MainActivity a,LinearLayout parent,Spinner choices){
  HorizontalScrollView scroll=new HorizontalScrollView(a);scroll.setHorizontalScrollBarEnabled(false);LinearLayout row=new LinearLayout(a);
  String[] labels={"+1,000km","+3,000km","+5,000km"};
  for(int i=0;i<labels.length;i++){
   final int position=i+2;Button chip=a.button(labels[i],false,()->choices.setSelection(position));chip.setTextSize(13);chip.setMinHeight(a.dp(48));chip.setContentDescription("거리 주기 "+labels[i]+" 선택");
   LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,-2);lp.rightMargin=a.dp(8);row.addView(chip,lp);
  }
  scroll.addView(row);parent.addView(scroll);a.gap(parent,12);
 }
 static void amountChips(MainActivity a,LinearLayout parent,EditText amount){
  a.chips(parent,new String[]{"1만원","2만원","3만원"},new String[]{"10000","20000","30000"},amount);
  HorizontalScrollView scroll=(HorizontalScrollView)parent.getChildAt(parent.getChildCount()-1);LinearLayout row=(LinearLayout)scroll.getChildAt(0);
  for(int i=0;i<row.getChildCount();i++){TextView chip=(TextView)row.getChildAt(i);chip.setMinHeight(a.dp(48));chip.setContentDescription("주유 금액 "+chip.getText()+" 선택");}
 }
 static String volume(double liters){return java.math.BigDecimal.valueOf(liters).stripTrailingZeros().toPlainString();}
 static double decimal(String raw){return Double.parseDouble(raw.replace(",","").trim());}
}
