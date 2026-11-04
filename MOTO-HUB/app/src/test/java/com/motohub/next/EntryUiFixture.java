package com.motohub.next;

import android.graphics.*;
import android.view.*;
import android.widget.*;
import android.os.Looper;
import java.io.*;
import org.robolectric.Shadows;
import static org.junit.Assert.*;

final class EntryUiFixture {
 static View tag(View root,String value){return ExperienceStructureTest.tagged(root,value);}
 static boolean contains(View root,String value){if(root instanceof TextView&&((TextView)root).getText().toString().contains(value))return true;if(root instanceof ViewGroup){ViewGroup group=(ViewGroup)root;for(int i=0;i<group.getChildCount();i++)if(contains(group.getChildAt(i),value))return true;}return false;}
 static EditText field(MainActivity a,String label){return (EditText)Ux106NativeUiTest.desc(a.formBody,label);}
 static Spinner spinner(MainActivity a,String label){return (Spinner)Ux106NativeUiTest.desc(a.formBody,label);}
 static void select(MainActivity a,String label,int position){spinner(a,label).setSelection(position);Shadows.shadowOf(Looper.getMainLooper()).idle();}
 static TextView text(View root,String label){TextView found=Ux106NativeUiTest.text(root,label);assertNotNull(label,found);return found;}
 static boolean shown(View view){for(View v=view;v!=null;){if(v.getVisibility()!=View.VISIBLE)return false;android.view.ViewParent p=v.getParent();v=p instanceof View?(View)p:null;}return true;}
 static void capture(MainActivity a,String name,int width)throws Exception{
  View root=a.dialog.getWindow().getDecorView();int w=a.dp(width),h=a.dp(852);root.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY));root.layout(0,0,w,h);
  Bitmap bitmap=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);root.draw(new Canvas(bitmap));File dir=new File("build/reports/entry-review");dir.mkdirs();try(FileOutputStream out=new FileOutputStream(new File(dir,name+".png"))){assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,out));}bitmap.recycle();
 }
}
