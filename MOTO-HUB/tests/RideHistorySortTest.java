package com.motohub.next;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

public final class RideHistorySortTest {
 static int assertions;
 static void check(boolean condition,String message){assertions++;if(!condition)throw new AssertionError(message);}
 static File make(File dir,String name,String contents)throws Exception{
  File file=new File(dir,name);Files.writeString(file.toPath(),contents,StandardCharsets.UTF_8);return file;
 }
 public static void main(String[] args)throws Exception{
  File dir=Files.createTempDirectory("moto-ride-sort-").toFile();
  try{
   long y2024=Instant.parse("2024-05-12T01:23:00Z").toEpochMilli();
   long y2025=Instant.parse("2025-08-01T00:00:00Z").toEpochMilli();
   long y2026=Instant.parse("2026-10-17T07:00:00Z").toEpochMilli();
   File native2025=make(dir,y2025+"-native.track","");
   File native2026=make(dir,y2026+"-native.track","");
   File imported2024=make(dir,(y2026+1000)+"-import.track","37.1,127.2,4.0,"+y2024+"\n");
   check(new File(imported2024.getPath()+".imported").createNewFile(),"import marker");
   File missing=make(dir,"no-date.track","");
   File missingImport=make(dir,(y2026+2000)+"-empty-import.track","");
   check(new File(missingImport.getPath()+".imported").createNewFile(),"empty import marker");
   File sameTime=make(dir,y2025+"-same.track","");
   List<File> input=Arrays.asList(missing,native2025,imported2024,native2026,missingImport,sameTime);
   check(RideHistorySort.normalize(null).equals("최신순"),"null default");
   check(RideHistorySort.normalize("bogus").equals("최신순"),"invalid default");
   check(RideHistorySort.normalize("오래된순").equals("오래된순"),"oldest accepted");
   check(RideHistorySort.restore("오래된순",null).equals("오래된순"),"process restart restores persisted sort");
   check(RideHistorySort.restore("최신순","오래된순").equals("오래된순"),"rotation state overrides stored sort");
   check(RideHistorySort.restore("오래된순","최신순").equals("최신순"),"rotation state can restore newest");
   check(RideHistorySort.restore("corrupted",null).equals("최신순"),"invalid preference safe default");
   check(RideHistorySort.restore("오래된순","corrupted").equals("최신순"),"invalid rotation state safe default");
   check(RideHistorySort.restore(null,null).equals("최신순"),"fresh install newest default");
   check(RideHistorySort.order(input,"최신순").equals(Arrays.asList(native2026,native2025,sameTime,imported2024,missing,missingImport)),"newest uses actual GPX time, unknown last, stable tie");
   check(RideHistorySort.order(input,"오래된순").equals(Arrays.asList(imported2024,native2025,sameTime,native2026,missing,missingImport)),"oldest chronology, unknown last, stable tie");
   check(RideHistorySort.order(input,"bogus").equals(RideHistorySort.order(input,"최신순")),"unknown preference safe");
   check(input.get(0)==missing&&input.get(2)==imported2024,"input not mutated");
   check(RideHistorySort.order(List.of(),"최신순").isEmpty(),"empty list");
   check(RideHistorySort.order(List.of(native2025),"오래된순").equals(List.of(native2025)),"single ride");
   check(Files.readString(imported2024.toPath()).equals("37.1,127.2,4.0,"+y2024+"\n"),"track bytes untouched");
   check(RideHistoryFilter.select(RideHistorySort.order(input,"최신순"),"GPX","",false).equals(Arrays.asList(imported2024,missingImport)),"filter composition");
   System.out.println("PASS: "+assertions+" ride-history sorting assertions");
  }finally{File[] files=dir.listFiles();if(files!=null)for(File f:files)f.delete();dir.delete();}
 }
}
