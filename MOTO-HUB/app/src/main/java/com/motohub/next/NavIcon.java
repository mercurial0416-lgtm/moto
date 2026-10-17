package com.motohub.next;
import android.content.Context;import android.view.View;import android.graphics.*;
/** Small stroke icons: no bitmap assets, network or font dependency. */
final class NavIcon extends View {
 private final int kind,color;private final Paint p=new Paint(3);private final Path path=new Path();
 public NavIcon(Context c){this(c,0,Color.BLACK);}
 NavIcon(Context c,int kind,int color){super(c);this.kind=kind;this.color=color;setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
 protected void onDraw(Canvas c){super.onDraw(c);c.save();c.scale(getWidth()/24f,getHeight()/24f);p.setColor(color);p.setStrokeWidth(1.7f);p.setStyle(Paint.Style.STROKE);p.setStrokeJoin(Paint.Join.ROUND);p.setStrokeCap(Paint.Cap.ROUND);
 switch(kind){case 0:Path h=path;h.reset();h.moveTo(3,10);h.lineTo(12,3);h.lineTo(21,10);h.moveTo(5,9);h.lineTo(5,21);h.lineTo(19,21);h.lineTo(19,9);h.moveTo(10,21);h.lineTo(10,14);h.lineTo(14,14);h.lineTo(14,21);c.drawPath(h,p);break;
 case 1:c.drawRoundRect(4,6,20,21,2,2,p);c.drawArc(8,2,16,12,180,180,false,p);break;
 case 2:c.drawRoundRect(5,3,20,21,2,2,p);for(int y=7;y<=17;y+=5)c.drawLine(10,y,16,y,p);c.drawLine(3,8,7,8,p);c.drawLine(3,16,7,16,p);break;
 case 3:Path m=path;m.reset();m.moveTo(12,22);m.cubicTo(9,17,4,13,4,9);m.cubicTo(4,-1,20,-1,20,9);m.cubicTo(20,13,15,17,12,22);c.drawPath(m,p);c.drawCircle(12,9,2.7f,p);break;
 default:c.drawCircle(8,7,3,p);c.drawCircle(17,9,2.5f,p);c.drawArc(2,12,14,24,180,180,false,p);c.drawArc(13,14,23,23,200,140,false,p);
 }c.restore();}
}
