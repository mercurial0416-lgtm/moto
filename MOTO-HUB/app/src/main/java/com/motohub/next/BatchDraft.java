package com.motohub.next;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** Bounded, versioned local form snapshot; never modifies stored service records. */
final class BatchDraft {
 final String bike,date,mileage;
 final boolean intervals;
 final boolean[] selected;
 final String[] costs;
 private BatchDraft(String bike,String date,String mileage,boolean intervals,boolean[] selected,String[] costs){
  this.bike=bike;this.date=date;this.mileage=mileage;this.intervals=intervals;this.selected=selected;this.costs=costs;
 }
 static String encode(String bike,String date,String mileage,boolean intervals,boolean[] selected,String[] costs){
  if(!ReceiptFiles.validId(bike)||selected==null||costs==null||selected.length!=ServiceBatch.MAX_ITEMS||costs.length!=selected.length)throw new IllegalArgumentException("묶음 정비 양식 오류");
  StringBuilder s=new StringBuilder("MHB1\n").append(bike).append('\n').append(date).append('\n').append(mileage).append('\n').append(intervals).append('\n');
  for(int i=0;i<selected.length;i++){
   String cost=costs[i]==null?"":costs[i].trim();if(cost.length()>16)throw new IllegalArgumentException("금액 길이 오류");
   s.append(selected[i]).append(',').append(Base64.getEncoder().encodeToString(cost.getBytes(StandardCharsets.UTF_8))).append('\n');
  }
  return s.toString();
 }
 static BatchDraft decode(String s,String currentBike){
  if(s==null||s.length()>2048||!ReceiptFiles.validId(currentBike))return null;
  String[] parts=s.split("\n",-1);if(parts.length!=14||!"MHB1".equals(parts[0])||!currentBike.equals(parts[1]))return null;
  if(parts[2].length()>16||parts[3].length()>20||!parts[4].matches("true|false"))return null;
  boolean[] selected=new boolean[ServiceBatch.MAX_ITEMS];String[] costs=new String[selected.length];
  for(int i=0;i<selected.length;i++){
   String[] pair=parts[i+5].split(",",2);if(pair.length!=2||!pair[0].matches("true|false"))return null;
   selected[i]=Boolean.parseBoolean(pair[0]);
   try{costs[i]=new String(Base64.getDecoder().decode(pair[1]),StandardCharsets.UTF_8);}
   catch(IllegalArgumentException invalid){return null;}
   if(costs[i].length()>16)return null;
  }
  if(!parts[13].isEmpty())return null;
  return new BatchDraft(currentBike,parts[2],parts[3],Boolean.parseBoolean(parts[4]),selected,costs);
 }
}
