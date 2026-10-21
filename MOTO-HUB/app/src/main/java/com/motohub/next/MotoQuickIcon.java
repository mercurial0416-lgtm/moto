package com.motohub.next;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

/** Four small hand-drawn native line icons: no emoji, webfonts or bitmap assets. */
final class MotoQuickIcon extends View {
 private final int kind,color;
 private final Paint pen=new Paint(Paint.ANTI_ALIAS_FLAG);
 MotoQuickIcon(Context context,int kind,int color){
  super(context);this.kind=kind;this.color=color;setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
  pen.setColor(color);pen.setStyle(Paint.Style.STROKE);
  pen.setStrokeWidth(1.8f);pen.setStrokeCap(Paint.Cap.ROUND);pen.setStrokeJoin(Paint.Join.ROUND);
 }
 @Override protected void onDraw(Canvas c){
  super.onDraw(c);
  float unit=Math.min(getWidth(),getHeight())/24f;
  c.save();c.translate((getWidth()-24*unit)/2f,(getHeight()-24*unit)/2f);c.scale(unit,unit);
  switch(kind){
   case 0: // Fuel pump and nozzle
    c.drawRoundRect(5,4,14,21,1.8f,1.8f,pen);
    c.drawRect(7.5f,7,11.5f,11,pen);
    c.drawLine(14,9,18,11,pen);c.drawLine(18,11,18,17,pen);c.drawLine(18,17,20,18,pen);
    c.drawLine(3.5f,21,16,21,pen);
    break;
   case 1: // Open-end wrench
    c.drawLine(8,15,17.7f,5.3f,pen);
    c.drawLine(10.5f,17.5f,20.1f,7.9f,pen);
    c.drawArc(15,2,22,9,32,226,false,pen);
    c.drawCircle(7.2f,18.6f,3.2f,pen);
    break;
   case 2: // Two stops connected by a route
    c.drawCircle(6,18,2.6f,pen);c.drawCircle(18,6,2.6f,pen);
    c.drawLine(6,15,6,11,pen);c.drawLine(6,11,9,8,pen);c.drawLine(9,8,11,8,pen);
    c.drawLine(11,8,13,13,pen);c.drawLine(13,13,18,13,pen);c.drawLine(18,13,18,9,pen);
    break;
   default: // Motorcycle in profile
    c.drawCircle(5.5f,18,3.2f,pen);c.drawCircle(18.5f,18,3.2f,pen);
    c.drawLine(5.5f,18,11,11,pen);c.drawLine(11,11,15,18,pen);
    c.drawLine(15,18,18.5f,18,pen);c.drawLine(11,11,9,18,pen);
    c.drawLine(9,18,15,18,pen);c.drawLine(12,9,17,9,pen);
    c.drawLine(17,9,19,15,pen);c.drawLine(11,9,12,9,pen);
    break;
  }
  c.restore();
 }
}
