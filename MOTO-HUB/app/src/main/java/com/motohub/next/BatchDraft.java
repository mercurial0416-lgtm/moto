package com.motohub.next;
import java.util.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
/** Small, strictly bounded local draft. No images, auth tokens or GPS are stored. */
final class BatchDraft {
 static final int MAX_BYTES=8192;
 private BatchDraft(){}
 static String encode(String bike,String date,String odo,Map<String,String> prices,boolean reuse){return encode(bike,date,odo,prices,reuse,false);}
 static String encode(String bike,String date,String odo,Map<String,String> prices,boolean reuse,boolean manual){
  if(!ReceiptSlots.validId(bike)||date==null||date.length()>20||odo==null||odo.length()>24||prices==null||prices.size()>8)return "";
  Properties p=new Properties();p.setProperty("bike",bike);p.setProperty("date",date);p.setProperty("odo",odo);p.setProperty("reuse",Boolean.toString(reuse));p.setProperty("manual",Boolean.toString(manual));
  for(Map.Entry<String,String> e:prices.entrySet()){
   if(!Arrays.asList(ServiceBatch.COMMON).contains(e.getKey())||e.getValue()==null||!e.getValue().matches("[0-9]{1,10}"))return "";
   p.setProperty("cost."+e.getKey(),e.getValue());
  }
  try{StringWriter out=new StringWriter();p.store(out,"batch draft");String raw=out.toString();return raw.getBytes(StandardCharsets.UTF_8).length<=MAX_BYTES?raw:"";}catch(Exception e){return "";}
 }
 static Properties decode(String raw,String bike){
  Properties p=new Properties();if(raw==null||raw.getBytes(StandardCharsets.UTF_8).length>MAX_BYTES||!ReceiptSlots.validId(bike))return p;
  try{p.load(new StringReader(raw));if(!bike.equals(p.getProperty("bike")))p.clear();for(String key:p.stringPropertyNames())if(!key.equals("bike")&&!key.equals("date")&&!key.equals("odo")&&!key.equals("reuse")&&!key.equals("manual")&&!key.startsWith("cost."))p.remove(key);}
  catch(Exception e){p.clear();}return p;
 }
}
