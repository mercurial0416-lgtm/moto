package com.motohub.next;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

/** Small, deterministic candidate de-duplication shared by the online gallery and JVM tests. */
final class PhotoGalleryChoices {
 private PhotoGalleryChoices(){}
 static <T> void append(List<T> out,Set<String> seen,List<T> incoming,Function<T,String> key,int max){
  if(max<=0||out.size()>=max||incoming==null)return;
  for(T item:incoming){
   if(item==null)continue;
   String value=key.apply(item);
   if(value!=null&&!value.isEmpty()&&seen.add(value))out.add(item);
   if(out.size()>=max)break;
  }
 }
}
