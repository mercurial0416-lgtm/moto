package com.motohub.next;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/** Offline 1,200-ride regression for date-scoped counts, imported dates and fast search. */
public final class RideHistoryScopeCountsTest {
 private static int checks;
 private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
 public static void main(String[] args)throws Exception{
  File root=Files.createTempDirectory("moto-cz-1200").toFile();
  File dir=TrackFile.directory(root);
  String bike="a231c9d0-2f0b-4d91-a73b-12034f4e20bc";
  long now=System.currentTimeMillis();
  Map<File,byte[]> original=new LinkedHashMap<>();
  try{
   for(int i=0;i<1200;i++){
    // 400 recent and 800 old rides, with 1 GPX whose filename looks recent.
    long recorded=i<400?now-(i+1)*60_000L:now-(40L+i)*86_400_000L;
    long named=i==400?now-1000:recorded;
    File track=new File(dir,named+"-"+String.format(Locale.ROOT,"00000000-0000-4000-8000-%012d",i)+".track");
    byte[] bytes=TrackFile.point(37.0,127.0,6.0,recorded).getBytes(StandardCharsets.UTF_8);
    Files.write(track.toPath(),bytes);original.put(track,bytes);
    if(i==400)Files.createFile(new File(track.getPath()+".imported").toPath());
    else if(i%3==0)Files.createFile(new File(track.getPath()+".recovered").toPath());
    else Files.createFile(new File(track.getPath()+".done").toPath());
    if(i%2==0)RideBikeBinding.write(track,bike);
   }
   List<File> rides=TrackFile.tracks(root);
   check(rides.size()==1200,"all 1200 rides retained");
   List<File> scoped=RideHistoryBikeFilter.select(rides,bike);
   check(scoped.size()==600,"600 bike-associated rides");
   scoped.removeIf(file->!RideHistoryPeriod.include(file,now,RideHistoryPeriod.WEEK));
   check(scoped.size()==200,"date scope applied before status chips");
   Map<String,Integer> counts=RideHistoryFilter.counts(scoped,"",false);
   check(counts.get(RideHistoryFilter.ALL)==200,"chip all count uses date and bike scope");
   int totalStatuses=0;
   for(String status:RideHistoryFilter.OPTIONS)if(!RideHistoryFilter.ALL.equals(status))totalStatuses+=counts.get(status);
   check(totalStatuses==200,"status counts partition filtered rides");
   check(counts.get(RideHistoryFilter.IMPORTED)==0,"old GPX import excluded from recent view");
   check(counts.get(RideHistoryFilter.INTERRUPTED)==0,"no false interruption");
   List<File> sorted=RideHistorySort.order(scoped,RideHistorySort.NEWEST);
   check(sorted.size()==200,"sorting preserves scoped records");
   String[] terms=RideHistorySearch.terms("10/19 GPX");
   check(terms.length==2,"date and type terms parsed");
   check(RideHistorySearch.matchesIndex(RideHistorySearch.index("2026.10.19 08:00 · 듀크","GPX"),terms),"two-term search");
   check(!RideHistorySearch.matchesIndex(RideHistorySearch.index("2026.10.19 08:00 · 듀크","완료"),terms),"AND search excludes wrong type");
   for(Map.Entry<File,byte[]> entry:original.entrySet())check(Arrays.equals(entry.getValue(),Files.readAllBytes(entry.getKey().toPath())),"GPS source unchanged");
   System.out.println("PASS: "+checks+" assertions across 1,200 local rides; source GPS bytes preserved");
  }finally{
   File[] files=dir.listFiles();if(files!=null)for(File file:files)if(!file.delete())file.deleteOnExit();
   dir.delete();root.delete();
  }
 }
}
