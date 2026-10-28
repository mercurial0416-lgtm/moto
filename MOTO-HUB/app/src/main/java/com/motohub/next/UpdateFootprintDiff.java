package com.motohub.next;
import java.util.*;
/** Private, read-only comparison; deliberately hides file hashes and identifiers. */
final class UpdateFootprintDiff {
 private UpdateFootprintDiff(){}
 static Map<String,String> parts(String input){
  Map<String,String> values=new LinkedHashMap<>();
  if(input!=null)for(String p:input.split(";")){
   int i=p.indexOf('=');if(i>0)values.put(p.substring(0,i),p.substring(i+1));
  }
  return values;
 }
 static String describe(String before,String after){
  Map<String,String> a=parts(before),b=parts(after);
  List<String> changed=new ArrayList<>();
  for(String key:a.keySet())if(!Objects.equals(a.get(key),b.get(key))){
   if("contentSha256".equals(key))changed.add("GPS·사진 파일 내용");
   else if("tracks".equals(key)||"trackBytes".equals(key)||"complete".equals(key))changed.add("GPS 파일");
   else if(key.startsWith("garagePhoto"))changed.add("바이크 사진");
   else changed.add("저장 기록 "+key);
  }
  if(changed.isEmpty())return "비교 데이터 형식 차이";
  return String.join(", ",new LinkedHashSet<>(changed))+" 변경 감지";
 }
}
