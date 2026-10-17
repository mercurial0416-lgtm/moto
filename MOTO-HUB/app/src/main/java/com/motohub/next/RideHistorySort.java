package com.motohub.next;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** Local-only ride chronology. Imported GPX is ordered by its actual ride time. */
final class RideHistorySort {
 static final String NEWEST="최신순", OLDEST="오래된순";
 static final String[] OPTIONS={NEWEST,OLDEST};
 private RideHistorySort(){}

 static String normalize(String value){return OLDEST.equals(value)?OLDEST:NEWEST;}

 /** Prefer a live rotation snapshot; otherwise use the persisted local UI setting. */
 static String restore(String persisted,String savedState){
  return normalize(savedState!=null?savedState:persisted);
 }

 static List<File> order(List<File> rides,String preference){
  final boolean ascending=OLDEST.equals(normalize(preference));
  class Entry {
   final File file;
   final Long started;
   Entry(File file){this.file=file;this.started=RideHistoryLabel.startTime(file);}
  }
  List<Entry> entries=new ArrayList<>();
  for(File file:rides)entries.add(new Entry(file));
  // Missing/corrupt timestamps stay at the bottom in both directions.
  // Java's stable sort preserves the original list order for equal times.
  entries.sort((a,b)->{
   if(a.started==null)return b.started==null?0:1;
   if(b.started==null)return -1;
   int cmp=Long.compare(a.started,b.started);
   return ascending?cmp:-cmp;
  });
  List<File> sorted=new ArrayList<>();
  for(Entry entry:entries)sorted.add(entry.file);
  return sorted;
 }
}
