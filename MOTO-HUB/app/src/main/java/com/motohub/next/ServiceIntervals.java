package com.motohub.next;

import java.util.LinkedHashMap;
import java.util.Map;

/** Editable starting suggestions, NOT official maintenance intervals. */
final class ServiceIntervals {
 private ServiceIntervals(){}
 static Map<String,Integer> starter(){
  Map<String,Integer> result=new LinkedHashMap<>();
  result.put("엔진오일",3000);result.put("오일필터",6000);result.put("에어필터",12000);
  result.put("체인",1000);result.put("브레이크 패드",8000);result.put("타이어",10000);
  return result;
 }
 static int validated(String name,int interval){return name!=null&&name.length()<=60&&interval>=100&&interval<=100000?interval:0;}
}
