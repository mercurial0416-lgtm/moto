package com.motohub.next;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.UUID;
final class RideTrackMergeTest {
 static int checks;
 static void ok(boolean v,String detail){if(!v)throw new AssertionError(detail);checks++;}
 static File track(File dir,long start,long gap,int count)throws Exception {
  File file=new File(TrackFile.directory(dir),start+"-"+UUID.randomUUID()+".track");
  try(FileOutputStream out=new FileOutputStream(file)){
   for(int i=0;i<count;i++)out.write(TrackFile.point(37.0+i*.001,127.0,.7,start+i*gap).getBytes(StandardCharsets.UTF_8));
  }
  TrackCompletion.mark(file,count);return file;
 }
 static void fail(File home,File a,File b)throws Exception {
  boolean rejected=false;try{RideTrackMerge.merge(home,a,b);}catch(IOException ex){rejected=true;}
  ok(rejected,"invalid merge accepted");
 }
 public static void main(String[] args)throws Exception {
  File dir=Files.createTempDirectory("moto-merge").toFile();
  try{
   File first=track(dir,1_700_000_000_000L,10_000,4);
   File second=track(dir,1_700_000_050_000L,10_000,3);
   byte[] original=Files.readAllBytes(first.toPath());
   RideTrackMerge.Result merged=RideTrackMerge.merge(dir,second,first);
   ok(merged.points==7,"point count");
   ok(merged.gapMs==20_000,"gap calculation");
   ok(TrackFile.read(merged.combined,(a,b,c,d)->{})==7,"merged points valid");
   ok(new File(merged.combined+".done").isFile(),"completion marker");
   ok(RideTrackMerge.mergedSource(first)&&RideTrackMerge.mergedSource(second),"source statistical marks");
   ok(java.util.Arrays.equals(original,Files.readAllBytes(first.toPath())),"source unchanged");
   fail(dir,first,second); // prevents duplicate merges
   File far=track(dir,1_700_010_000_000L,10_000,2);
   File near=track(dir,1_700_010_100_000L,10_000,2);
   fail(dir,far,first); // distant from original
   File overlapping=track(dir,1_700_010_105_000L,10_000,2);
   fail(dir,near,overlapping); // overlap rejected
   RideTrackMerge.Result m2=RideTrackMerge.merge(dir,far,near);
   ok(m2.points==4,"second merge success");
   ok(!RideTrackMerge.mergedSource(overlapping),"unselected record unmodified");
   File unfinished=new File(TrackFile.directory(dir),System.currentTimeMillis()+"-"+UUID.randomUUID()+".track");
   Files.write(unfinished.toPath(),TrackFile.point(37,127,1,System.currentTimeMillis()).getBytes(StandardCharsets.UTF_8));
   fail(dir,unfinished,overlapping);
   ok(!new File(unfinished+".mergedinto").exists(),"unfinished source protected");
   System.out.println("Ride merge tests passed: "+checks);
  }finally{delete(dir);}
 }
 static void delete(File file){File[] children=file.listFiles();if(children!=null)for(File child:children)delete(child);file.delete();}
}
