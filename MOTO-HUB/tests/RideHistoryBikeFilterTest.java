package com.motohub.next;

import java.io.File;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public final class RideHistoryBikeFilterTest {
 static int assertions;
 static void check(boolean ok,String message){assertions++;if(!ok)throw new AssertionError(message);}
 static File make(File root,String id)throws Exception{
  File track=new File(TrackFile.directory(root),"1700000000000-"+UUID.randomUUID()+".track");
  Files.write(track.toPath(),TrackFile.point(37.0,127.0,4.0,1700000000000L).getBytes(StandardCharsets.UTF_8));
  if(id!=null)RideBikeBinding.write(track,id);
  return track;
 }
 static void clean(File file){File[] children=file.listFiles();if(children!=null)for(File child:children)clean(child);file.delete();}
 public static void main(String[] args)throws Exception{
  File root=Files.createTempDirectory("moto-history-bike-filter-").toFile();
  try{
   String duke=UUID.randomUUID().toString(),cub=UUID.randomUUID().toString();
   File a=make(root,duke),b=make(root,cub),old=make(root,null),bad=make(root,null);
   Files.write(new File(bad.getPath()+".bike").toPath(),"broken-id".getBytes(StandardCharsets.US_ASCII));
   List<File> tracks=Arrays.asList(a,b,old,bad);
   byte[] original=Files.readAllBytes(a.toPath());
   check(RideHistoryBikeFilter.ALL.equals(RideHistoryBikeFilter.normalize("invalid")),"unknown fallback");
   check(RideHistoryBikeFilter.ALL.equals(RideHistoryBikeFilter.normalize(null)),"null fallback");
   check(RideHistoryBikeFilter.UNASSIGNED.equals(RideHistoryBikeFilter.normalize(RideHistoryBikeFilter.UNASSIGNED)),"unassigned scope");
   check(duke.equals(RideHistoryBikeFilter.normalize(duke)),"UUID preserved");
   check(duke.equals(RideHistoryBikeFilter.restore(cub,duke)),"rotation state wins");
   check(cub.equals(RideHistoryBikeFilter.restore(cub,null)),"persisted state used");
   check(RideHistoryBikeFilter.ALL.equals(RideHistoryBikeFilter.restore("bad",null)),"corrupt preference defaults to all");
   check(RideHistoryBikeFilter.select(tracks,RideHistoryBikeFilter.ALL).equals(tracks),"all rides visible including corrupt");
   check(RideHistoryBikeFilter.select(tracks,duke).equals(Arrays.asList(a)),"duke-only rides");
   check(RideHistoryBikeFilter.select(tracks,cub).equals(Arrays.asList(b)),"cub-only rides");
   check(RideHistoryBikeFilter.select(tracks,RideHistoryBikeFilter.UNASSIGNED).equals(Arrays.asList(old)),"corrupt is not unassigned");
   check(RideHistoryBikeFilter.damaged(tracks)==1,"one damaged sidecar flagged");
   check(RideHistoryBikeFilter.select(tracks,UUID.randomUUID().toString()).isEmpty(),"missing bike shows none");
   check(Arrays.equals(original,Files.readAllBytes(a.toPath())),"filter never changes GPS bytes");
   check(Files.exists(new File(bad.getPath()+".bike").toPath()),"corrupt metadata retained for backup/recovery");
   System.out.println("PASS: "+assertions+" ride-bike filter assertions");
  }finally{clean(root);}
 }
}
