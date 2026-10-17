package com.motohub.next;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.widget.ImageView;
import android.widget.TextView;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=35)
public class MotorcyclePhotoStateTest {
 @Test public void emptySelectionClearsPreviousPhotoAndLink(){
  MainActivity a=Robolectric.buildActivity(MainActivity.class).setup().get();
  try {
   ImageView image=new ImageView(a);image.setImageDrawable(new ColorDrawable(Color.RED));
   TextView credit=new TextView(a);credit.setOnClickListener(v->fail("stale link"));
   MotorcyclePhoto.show(a,image,credit,"","",2026);
   assertNull(image.getDrawable());assertFalse(credit.hasOnClickListeners());
   assertTrue(credit.getText().toString().contains("모델 정보"));
  }finally{a.finish();}
 }
}
