package com.motohub.next;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/** Presentation-only grouping: never merge or mutate original GPS tracks. */
final class RideHistorySessionRows {
 private RideHistorySessionRows(){}

 static final class Row<T> {
  final String key;
  final List<T> segments=new ArrayList<>();
  Row(String key){this.key=key;}
  boolean grouped(){return !key.isEmpty()&&segments.size()>1;}
 }

 /** Preserve the latest item position; files without verified identity stay independent. */
 static <T> List<Row<T>> group(List<T> records,Function<T,String> stableKey){
  List<Row<T>> rows=new ArrayList<>();
  Map<String,Row<T>> indexed=new LinkedHashMap<>();
  if(records==null)return rows;
  for(T item:records){
   String key=item==null?"":stableKey.apply(item);
   if(key==null||key.isEmpty()){
    Row<T> single=new Row<>("");
    single.segments.add(item);
    rows.add(single);
    continue;
   }
   Row<T> row=indexed.get(key);
   if(row==null){
    row=new Row<>(key);
    indexed.put(key,row);
    rows.add(row);
   }
   row.segments.add(item);
  }
  return rows;
 }
}
