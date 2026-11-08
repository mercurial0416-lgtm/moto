package com.motohub.next;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class RideHistorySessionRowsTest {
 private static void check(boolean yes,String message){if(!yes)throw new AssertionError(message);}
 private static final class Entry {
  final String key,name;
  Entry(String key,String name){this.key=key;this.name=name;}
 }
 public static void main(String[] args){
  List<Entry> entries=Arrays.asList(
   new Entry("session1|bikeA|일반","new segment"),
   new Entry("","incomplete (must remain separate)"),
   new Entry("session2|bikeA|일반","other ride"),
   new Entry("session1|bikeA|일반","older segment"),
   new Entry("session1|bikeB|일반","same session but different bike"),
   new Entry("session1|bikeA|투어","same session but different ride category"),
   new Entry("","another incomplete (must remain separate)")
  );
  List<RideHistorySessionRows.Row<Entry>> rows=RideHistorySessionRows.group(entries,e->e.key);
  check(rows.size()==6,"only same-session/same-bike/same-category records may group");
  check(rows.get(0).grouped(),"two nonadjacent legitimate sections must become one display ride");
  check(rows.get(0).segments.size()==2,"group retains original segment count");
  check(rows.get(0).segments.get(0)==entries.get(0)&&rows.get(0).segments.get(1)==entries.get(3),
   "group preserves newest-first segment order and identity");
  check(rows.get(1).segments.get(0)==entries.get(1)&&!rows.get(1).grouped(),
   "an incomplete GPS record must remain independent");
  check(rows.get(3).segments.get(0)==entries.get(4)&&!rows.get(3).grouped(),
   "other bike cannot share displayed ride");
  check(rows.get(4).segments.get(0)==entries.get(5)&&!rows.get(4).grouped(),
   "different categories cannot silently merge");
  check(RideHistorySessionRows.group(Collections.<Entry>emptyList(),e->e.key).isEmpty(),
   "empty history returns no groups");
  check(entries.size()==7,"source list remains untouched");
  System.out.println("PASS RideHistorySessionRowsTest: 9 assertions");
 }
}
