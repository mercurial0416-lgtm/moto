package com.motohub.next;
import java.util.*;
/** Reversible display-only order; no changes to stored motorcycle data. */
final class HomeShortcutOrder {
 private static final String[] DEFAULT={"ride","nav","fuel","service"};
 private HomeShortcutOrder(){}
 static String[] parse(String input){
  LinkedHashSet<String> ordered=new LinkedHashSet<>();
  if(input!=null)for(String key:input.split(","))for(String allowed:DEFAULT)
   if(allowed.equals(key.trim()))ordered.add(allowed);
  for(String key:DEFAULT)ordered.add(key);
  return ordered.toArray(new String[0]);
 }
 static String move(String stored,String key,int direction){
  List<String> order=new ArrayList<>(Arrays.asList(parse(stored)));
  int index=order.indexOf(key),other=index+direction;
  if(index>=0&&other>=0&&other<order.size())Collections.swap(order,index,other);
  return String.join(",",order);
 }
 static String label(String key){
  switch(key){
   case "ride":return "GPS 출발·종료";
   case "nav":return "내비 실행";
   case "fuel":return "주유 기록";
   case "service":return "정비주기";
   default:return "";
  }
 }
}
