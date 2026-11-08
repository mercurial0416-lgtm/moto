package com.motohub.next;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.DoublePredicate;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;

/** Distance filter matches complete display sessions, never half of a GPS rollover. */
final class RideHistorySessionDistance {
 private RideHistorySessionDistance(){}
 static <T> List<T> select(List<T> records,Function<T,String> groupKey,
                           ToDoubleFunction<T> distanceKm,DoublePredicate accepts){
  List<T> selected=new ArrayList<>();
  if(records==null||records.isEmpty())return selected;
  final class Sum {
   double km=0;boolean valid=true;
  }
  Map<String,Sum> sums=new HashMap<>();
  for(T item:records){
   String key=item==null?"":groupKey.apply(item);
   if(key==null||key.isEmpty())continue; // Independent files are checked one-by-one.
   Sum sum=sums.computeIfAbsent(key,k->new Sum());
   double km=distanceKm.applyAsDouble(item);
   if(!Double.isFinite(km)||km<0||km>500)sum.valid=false;
   else sum.km+=km;
  }
  for(T item:records){
   String key=item==null?"":groupKey.apply(item);
   if(key==null||key.isEmpty()){
    double km=distanceKm.applyAsDouble(item);
    if(Double.isFinite(km)&&km>=0&&km<=500&&accepts.test(km))selected.add(item);
   }else{
    Sum group=sums.get(key);
    // One unreadable section prevents an incomplete total from qualifying.
    if(group!=null&&group.valid&&Double.isFinite(group.km)&&accepts.test(group.km))
     selected.add(item);
   }
  }
  return selected;
 }
}
