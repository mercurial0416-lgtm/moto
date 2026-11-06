package com.motohub.next;

import android.app.Dialog;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

/**
 * One branded confirmation surface for ride and record actions.
 * This is an app-owned dialog, not a system permission or security prompt.
 * No GPS, network, storage, or background operations happen from rendering it.
 */
final class MotoDialog {
 private MotoDialog(){}

 static void rideStart(MainActivity a,String bikeName,boolean launchNavigator,
                       Runnable onStart,Runnable onCancel){
  List<String> facts=new ArrayList<>();
  facts.add("GPS 경로를 이 휴대폰에 저장해요.");
  facts.add("화면이 꺼져도 지속 알림으로 기록해요.");
  facts.add("서버나 친구에게 자동 전송하지 않아요.");
  if(launchNavigator){
   facts.add("기록이 시작된 다음 외부 내비를 실행해요.");
   facts.add("OBD2는 자동으로 연결하지 않아요.");
  }
  show(a,"라이딩을 시작할까요?","기록할 바이크",bikeName,
       facts,"이 바이크로 기록 시작","취소",onStart,onCancel);
 }

 static void confirm(MainActivity a,String title,String message,
                     String primaryLabel,String secondaryLabel,
                     Runnable onConfirm,Runnable onCancel){
  List<String> facts=new ArrayList<>();
  if(message!=null&&!message.trim().isEmpty())facts.add(message);
  show(a,title,null,null,facts,primaryLabel,secondaryLabel,onConfirm,onCancel);
 }

 private static void show(MainActivity a,String title,String infoLabel,String infoValue,
                          List<String> facts,String primaryLabel,String secondaryLabel,
                          Runnable onConfirm,Runnable onCancel){
  if(a.isFinishing()||a.isDestroyed())return;
  final Dialog dialog=new Dialog(a);
  dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
  dialog.setCancelable(true);
  dialog.setCanceledOnTouchOutside(true);
  dialog.setOnCancelListener(ignored->{if(onCancel!=null)onCancel.run();});

  LinearLayout shell=new LinearLayout(a);
  shell.setOrientation(LinearLayout.VERTICAL);
  shell.setPadding(a.dp(12),a.dp(8),a.dp(12),a.dp(12));

  LinearLayout sheet=new LinearLayout(a);
  sheet.setOrientation(LinearLayout.VERTICAL);
  sheet.setPadding(a.dp(22),a.dp(14),a.dp(22),a.dp(20));
  sheet.setBackground(a.shape(a.panel,a.line,26));
  shell.addView(sheet,new LinearLayout.LayoutParams(-1,-2));

  View grip=new View(a);
  grip.setBackground(a.shape(a.muted,0,3));
  grip.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
  LinearLayout.LayoutParams gripLp=new LinearLayout.LayoutParams(a.dp(38),a.dp(4));
  gripLp.gravity=Gravity.CENTER_HORIZONTAL;
  gripLp.bottomMargin=a.dp(22);
  sheet.addView(grip,gripLp);

  TextView heading=a.text(title,22,a.fg,true);
  heading.setContentDescription(title);
  heading.setLineSpacing(a.dp(2),1f);
  sheet.addView(heading,new LinearLayout.LayoutParams(-1,-2));

  ScrollView scroller=new ScrollView(a);
  scroller.setFillViewport(false);
  scroller.setVerticalScrollBarEnabled(false);
  LinearLayout details=new LinearLayout(a);
  details.setOrientation(LinearLayout.VERTICAL);
  details.setPadding(0,a.dp(18),0,a.dp(12));
  scroller.addView(details,new ScrollView.LayoutParams(-1,-2));
  LinearLayout.LayoutParams scrollLp=new LinearLayout.LayoutParams(-1,-2);
  sheet.addView(scroller,scrollLp);

  if(infoValue!=null){
   LinearLayout info=new LinearLayout(a);
   info.setOrientation(LinearLayout.VERTICAL);
   info.setPadding(a.dp(16),a.dp(14),a.dp(16),a.dp(14));
   info.setBackground(a.shape(a.highlight,0,16));
   TextView eyebrow=a.text(infoLabel==null?"바이크":infoLabel,12,a.muted,true);
   info.addView(eyebrow);
   TextView value=a.text(infoValue,18,a.fg,true);
   LinearLayout.LayoutParams vlp=new LinearLayout.LayoutParams(-1,-2);
   vlp.topMargin=a.dp(6);
   info.addView(value,vlp);
   LinearLayout.LayoutParams ilp=new LinearLayout.LayoutParams(-1,-2);
   ilp.bottomMargin=a.dp(16);
   details.addView(info,ilp);
  }

  for(String fact:facts){
   if(fact==null||fact.trim().isEmpty())continue;
   LinearLayout line=new LinearLayout(a);
   line.setOrientation(LinearLayout.HORIZONTAL);
   line.setGravity(Gravity.TOP);
   TextView dot=a.text("•",17,a.accent,true);
   dot.setContentDescription("");
   dot.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
   LinearLayout.LayoutParams dotLp=new LinearLayout.LayoutParams(a.dp(20),-2);
   line.addView(dot,dotLp);
   TextView copy=a.text(fact,15,a.fg,false);
   copy.setLineSpacing(a.dp(3),1.04f);
   line.addView(copy,new LinearLayout.LayoutParams(0,-2,1));
   LinearLayout.LayoutParams rowLp=new LinearLayout.LayoutParams(-1,-2);
   rowLp.bottomMargin=a.dp(11);
   details.addView(line,rowLp);
  }

  TextView primary=action(a,primaryLabel,true);
  primary.setOnClickListener(v->{dialog.dismiss();if(onConfirm!=null)onConfirm.run();});
  sheet.addView(primary,new LinearLayout.LayoutParams(-1,a.dp(54)));

  TextView secondary=action(a,secondaryLabel,false);
  secondary.setOnClickListener(v->{dialog.dismiss();if(onCancel!=null)onCancel.run();});
  LinearLayout.LayoutParams secondaryLp=new LinearLayout.LayoutParams(-1,a.dp(48));
  secondaryLp.topMargin=a.dp(8);
  sheet.addView(secondary,secondaryLp);

  // Keep the action buttons visible even at enlarged font sizes.
  int maxScroll=(int)(a.getResources().getDisplayMetrics().heightPixels*0.52f);
  scroller.post(()->{
   if(!dialog.isShowing())return;
   if(scroller.getHeight()>maxScroll){
    LinearLayout.LayoutParams limited=(LinearLayout.LayoutParams)scroller.getLayoutParams();
    limited.height=maxScroll;
    scroller.setLayoutParams(limited);
   }
  });
  dialog.setContentView(shell);
  Window window=dialog.getWindow();
  if(window!=null){
   window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
   window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
   window.setDimAmount(.60f);
   window.setGravity(Gravity.BOTTOM);
   window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
  }
  dialog.show();
  if(window!=null)window.setLayout(WindowManager.LayoutParams.MATCH_PARENT,WindowManager.LayoutParams.WRAP_CONTENT);
 }

 private static TextView action(MainActivity a,String text,boolean primary){
  TextView button=a.text(text,16,primary?MotoDesign.onAccent(a.dark):a.fg,true);
  button.setGravity(Gravity.CENTER);
  button.setClickable(true);
  button.setFocusable(true);
  button.setMinHeight(a.dp(48));
  MotoDesign.ripple(a,button,primary?a.accent:a.highlight,primary?0:a.line,16);
  return button;
 }
}
