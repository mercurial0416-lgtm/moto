package com.motohub.next;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/** Offline filesystem regression: metadata snapshots must not modify the GPS source. */
public final class RideHistoryAsyncDataTest {
 private static int assertions;
 private static void check(boolean condition,String message){assertions++;if(!condition)throw new AssertionError(message);}
 private static File create(File dir,long nameTime,long recordedTime,String uuid,boolean imported)throws Exception{
  File file=new File(dir,nameTime+"-"+uuid+".track");
  Files.write(file.toPath(),TrackFile.point(37.0,127.0,8,recordedTime).getBytes(StandardCharsets.UTF_8));
  Files.createFile(new File(file.getPath()+(imported?".imported":".done")).toPath());
  return file;
 }
 public static void main(String[] args)throws Exception{
  File root=Files.createTempDirectory("moto-history-async-test").toFile();
  File rides=TrackFile.directory(root);
  String bike="a231c9d0-2f0b-4d91-a73b-12034f4e20bc";
  long now=System.currentTimeMillis();
  Map<File,byte[]> originals=new LinkedHashMap<>();
  try{
   for(int i=0;i<120;i++){
    long nameTime=now-i*60_000L;
    long recordedTime=i==0?now-100L*86_400_000L:nameTime;
    String uuid=String.format(Locale.ROOT,"00000000-0000-4000-8000-%012d",i);
    File file=create(rides,nameTime,recordedTime,uuid,i==0);
    if(i%2==0)RideBikeBinding.write(file,bike);
    originals.put(file,Files.readAllBytes(file.toPath()));
   }
   File bad=create(rides,now-200_000L,now-200_000L,"f0000000-0000-4000-8000-000000000001",false);
   Files.write(new File(bad.getPath()+".bike").toPath(),"bad-data".getBytes(StandardCharsets.US_ASCII));
   originals.put(bad,Files.readAllBytes(bad.toPath()));
   List<File> all=TrackFile.tracks(root);
   check(all.size()==121,"track count");
   check(RideHistoryBikeFilter.damaged(all)==1,"corrupt binding count");
   check(RideHistoryBikeFilter.select(all,bike).size()==60,"bike filter");
   check(RideHistoryBikeFilter.select(all,RideHistoryBikeFilter.UNASSIGNED).size()==60,"corrupt not unassigned");
   List<File> ordered=RideHistorySort.order(all,RideHistorySort.NEWEST);
   check(!ordered.get(0).getName().equals(now+"-00000000-0000-4000-8000-000000000000.track"),"old imported GPX not newest");
   check(!RideHistoryPeriod.include(new File(rides,now+"-00000000-0000-4000-8000-000000000000.track"),now,RideHistoryPeriod.MONTH),"old GPX time excluded");
   int recent=0;for(File f:ordered)if(RideHistoryPeriod.include(f,now,RideHistoryPeriod.MONTH))recent++;
   check(recent==120,"recent count excludes 100-day-old GPX");
   for(Map.Entry<File,byte[]> e:originals.entrySet())check(Arrays.equals(e.getValue(),Files.readAllBytes(e.getKey().toPath())),"source mutated: "+e.getKey());
   check(new File(rides,now+"-00000000-0000-4000-8000-000000000000.track").exists(),"import retained");
   System.out.println("PASS: "+assertions+" assertions, 121 ride files, bike/date/status and byte-for-byte preservation");
  }finally{
   File[] all=rides.listFiles();if(all!=null)for(File f:all)f.delete();rides.delete();root.delete();
  }
 }
}
