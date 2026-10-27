package com.motohub.next;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;

/** Launcher-owned pin flow, invoked only after explicit user tap. */
final class MotoWidgetPin {
 private MotoWidgetPin(){}
 static void request(MainActivity a,boolean quick){
  AppWidgetManager widgets=AppWidgetManager.getInstance(a);
  if(!widgets.isRequestPinAppWidgetSupported()){
   a.toast("기존 위젯은 홈에서 길게 눌러 제거하고, 빈 곳 길게 누르기 → 위젯 → MOTO HUB에서 새 위젯을 추가하세요.");
   return;
  }
  Class<?> provider=quick?MotoQuickWidgetProvider.class:MotoWidgetProvider.class;
  boolean requested=widgets.requestPinAppWidget(new ComponentName(a,provider),null,null);
  a.toast(requested?(quick?"초소형 2×1":"기본 4×1")+" 위젯 추가 요청을 보냈어요. 홈 화면에서 승인해 주세요.":
   "홈 화면에 위젯 추가 요청을 보낼 수 없어요. 홈 화면 위젯 목록에서 추가해 주세요.");
 }
}
