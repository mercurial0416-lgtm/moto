package com.motohub.next;

import android.text.InputType;
import android.widget.*;

/** Functional task forms inherit the app's existing components; no inferred reference styling. */
final class FeatureUi {
 private FeatureUi(){}
 static EditText input(MainActivity a,LinearLayout host,String label,String value,boolean multiline){
  a.label(host,label,14,a.fg,true);EditText field=new EditText(a);field.setTextColor(a.fg);field.setHintTextColor(a.muted);field.setContentDescription(label);field.setSingleLine(!multiline);field.setInputType(InputType.TYPE_CLASS_TEXT|(multiline?InputType.TYPE_TEXT_FLAG_MULTI_LINE:0));field.setText(value);host.addView(field,new LinearLayout.LayoutParams(-1,-2));a.gap(host,10);return field;
 }
 static EditText day(MainActivity a,LinearLayout host,String label,String value){EditText field=input(a,host,label,value,false);field.setFocusable(false);field.setOnClickListener(v->a.pickDate(field));return field;}
 static String money(java.math.BigDecimal value){java.text.NumberFormat format=java.text.NumberFormat.getNumberInstance(java.util.Locale.KOREA);format.setMaximumFractionDigits(3);return format.format(value)+"원";}
}
