package com.motohub.next;
import android.content.Context;import android.view.View;import android.graphics.*;
/** Small stroke icons: no bitmap assets, network or font dependency. */
final class NavIcon extends View {
 private final int kind,color;private final Paint p=new Paint(3);private final Path path=new Path();
 public NavIcon(Context c){this(c,0,Color.BLACK);}
 NavIcon(Context c,int kind,int color){super(c);this.kind=kind;this.color=color;setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
 protected void onDraw(Canvas c){super.onDraw(c);c.save();c.scale(getWidth()/24f,getHeight()/24f);p.setColor(color);p.setStrokeWidth(1.85f);p.setStyle(Paint.Style.STROKE);p.setStrokeJoin(Paint.Join.ROUND);p.setStrokeCap(Paint.Cap.ROUND);
 switch(kind){case 0:Path h=path;h.reset();h.moveTo(3,10);h.lineTo(12,3);h.lineTo(21,10);h.moveTo(5,9);h.lineTo(5,21);h.lineTo(19,21);h.lineTo(19,9);h.moveTo(10,21);h.lineTo(10,14);h.lineTo(14,14);h.lineTo(14,21);c.drawPath(h,p);break;
 case 1: // Listings: price tag rather than a shopping bag.
  Path tag=path;tag.reset();tag.moveTo(3,5);tag.lineTo(13,5);tag.lineTo(21,13);
  tag.lineTo(13,21);tag.lineTo(3,11);tag.close();
  c.drawPath(tag,p);c.drawCircle(9,10,1.35f,p);break;
 case 2:c.drawRoundRect(5,3,20,21,2,2,p);for(int y=7;y<=17;y+=5)c.drawLine(10,y,16,y,p);c.drawLine(3,8,7,8,p);c.drawLine(3,16,7,16,p);break;
 case 3:Path m=path;m.reset();m.moveTo(12,22);m.cubicTo(9,17,4,13,4,9);m.cubicTo(4,-1,20,-1,20,9);m.cubicTo(20,13,15,17,12,22);c.drawPath(m,p);c.drawCircle(12,9,2.7f,p);break;
 default: // Settings: eight-tooth gear, replacing the misleading people icon.
  Path gear=path;gear.reset();
  for(int i=0;i<24;i++){
   double radians=i*Math.PI/12-Math.PI/2;
   float radius=i%3==1?10f:7.8f;
   float x=12+(float)Math.cos(radians)*radius;
   float y=12+(float)Math.sin(radians)*radius;
   if(i==0)gear.moveTo(x,y);else gear.lineTo(x,y);
  }
  gear.close();c.drawPath(gear,p);c.drawCircle(12,12,3.0f,p);
 }c.restore();}
}
