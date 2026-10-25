package com.motohub.next;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.UUID;

final class RideCategoryAndUndoTest {
 static int checks;
 static void ok(boolean test,String message){if(!test)throw new AssertionError(message);checks++;}
 static File make(File root,long start)throws Exception {
  File track=new File(TrackFile.directory(root),start+"-"+UUID.randomUUID()+".track");
  try(FileOutputStream out=new FileOutputStream(track)){
   out.write(TrackFile.point(37,127,2,start).getBytes(StandardCharsets.UTF_8));
   out.write(TrackFile.point(37.005,127.005,2,start+10_000).getBytes(StandardCharsets.UTF_8));
  }
  TrackCompletion.mark(track,2);
  return track;
 }
 static void fail(RunnableWithException action,String name)throws Exception {
  boolean rejected=false;
  try{action.run();}catch(IOException e){rejected=true;}
  ok(rejected,name);
 }
 interface RunnableWithException{void run()throws Exception;}
 public static void main(String[] args)throws Exception {
  File home=Files.createTempDirectory("moto-cs-category").toFile();
  File restored=Files.createTempDirectory("moto-cs-restore").toFile();
  try{
   File a=make(home,1_700_000_000_000L), b=make(home,1_700_000_030_000L);
   ok(RideCategory.AUTO.equals(RideCategory.read(a)),"automatic category by default");
   ok(RideCategory.LONG.equals(RideCategory.suggested(180,7200000,90)),"long trip heuristic");
   ok(RideCategory.DAILY.equals(RideCategory.suggested(7,1800000,14)),"short ride not falsely commute");
   ok(RideCategory.suggested(35,3600000,35).contains("추정"),"winding only suggested");
   fail(()->RideCategory.write(a,"other"),"invalid category rejected");
   RideCategory.write(a,RideCategory.COMMUTE);
   ok(RideCategory.COMMUTE.equals(RideCategory.read(a)),"stored commute label");
   RideTrackMerge.Result result=RideTrackMerge.merge(home,a,b);
   ok(RideTrackMerge.canUndo(home,result.combined),"merge can be undone");
   ok(RideTrackMerge.mergedSource(a)&&RideTrackMerge.mergedSource(b),"sources excluded");
   ok(!RideTrackMerge.canDelete(home,a)&&!RideTrackMerge.canDelete(home,result.combined),"active merge cannot be deleted");
   byte[] mergedBytes=Files.readAllBytes(result.combined.toPath());
   ok(RideTrackMerge.undo(home,result.combined)==2,"undo succeeds");
   ok(RideTrackMerge.archived(result.combined),"combined archive flag");
   ok(!RideTrackMerge.mergedSource(a)&&!RideTrackMerge.mergedSource(b),"sources restored to stats");
   ok(RideTrackMerge.canDelete(home,a)&&RideTrackMerge.canDelete(home,result.combined),"after undo files are independent");
   ok(java.util.Arrays.equals(mergedBytes,Files.readAllBytes(result.combined.toPath())),"combined GPS preserved");
   fail(()->RideTrackMerge.merge(home,a,result.combined),"archived merge cannot be merged again");
   fail(()->RideTrackMerge.undo(home,result.combined),"double undo rejected");
   RideCategory.write(result.combined,RideCategory.TOUR);
   ByteArrayOutputStream bytes=new ByteArrayOutputStream();
   BackupBundle.export("{\"version\":3}",home,bytes);
   try(RestoreArchive.Prepared prepared=RestoreArchive.prepare(new ByteArrayInputStream(bytes.toByteArray()),restored)){
    ok(prepared.rides==3,"all three GPS files in backup");
    prepared.install(restored,j->{});
   }
   File copyA=new File(restored,"rides/"+a.getName());
   File copyB=new File(restored,"rides/"+b.getName());
   File combined=new File(restored,"rides/"+result.combined.getName());
   ok(!RideTrackMerge.mergedSource(copyA)&&!RideTrackMerge.mergedSource(copyB),"originals restored without double-count marker");
   ok(RideTrackMerge.archived(combined),"archive status roundtrip");
   ok(RideCategory.TOUR.equals(RideCategory.read(combined)),"combined category roundtrip");
   ok(RideCategory.COMMUTE.equals(RideCategory.read(copyA)),"source category roundtrip");
   RideCategory.write(copyA,RideCategory.AUTO);
   ok(RideCategory.AUTO.equals(RideCategory.read(copyA)),"category reset");
   System.out.println("CS ride categorization/undo/backup tests passed: "+checks);
  }finally{delete(home);delete(restored);}
 }
 static void delete(File dir){File[] all=dir.listFiles();if(all!=null)for(File f:all)delete(f);dir.delete();}
}
