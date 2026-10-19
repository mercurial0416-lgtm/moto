package com.motohub.next;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Regression for automatic bike-to-track binding without modifying GPS coordinates. */
final class RideBikeBindingTest {
 static int assertions;
 static void check(boolean result,String message){if(!result)throw new AssertionError(message);assertions++;}
 static File track(File root,long timestamp)throws Exception{
  File f=new File(TrackFile.directory(root),timestamp+"-"+UUID.randomUUID()+".track");
  try(FileOutputStream output=new FileOutputStream(f)){
   output.write(TrackFile.point(37.0,127.0,4,timestamp).getBytes(StandardCharsets.UTF_8));
   output.write(TrackFile.point(37.001,127.001,4,timestamp+10000).getBytes(StandardCharsets.UTF_8));
  }
  TrackCompletion.mark(f,2);return f;
 }
 static void rejection(Run action,String label)throws Exception{
  boolean failed=false;try{action.go();}catch(IOException expected){failed=true;}
  check(failed,label);
 }
 interface Run{void go()throws Exception;}
 static void clean(File f){File[] children=f.listFiles();if(children!=null)for(File c:children)clean(c);f.delete();}
 public static void main(String[] args)throws Exception{
  File base=Files.createTempDirectory("moto-ride-binding").toFile();
  File restored=Files.createTempDirectory("moto-ride-binding-restore").toFile();
  try{
   String duke=UUID.randomUUID().toString(),cub=UUID.randomUUID().toString();
   File a=track(base,1700000000000L),b=track(base,1700000030000L);
   check(RideBikeBinding.read(a).isEmpty(),"old tracks remain unbound");
   rejection(()->RideBikeBinding.write(a,"\n"+duke),"malformed ID rejected");
   rejection(()->RideBikeBinding.write(a,"not-a-uuid"),"non UUID rejected");
   byte[] gps=Files.readAllBytes(a.toPath());
   RideBikeBinding.write(a,duke);RideBikeBinding.write(b,cub);
   check(duke.equals(RideBikeBinding.read(a)),"bike A associated");
   check(cub.equals(RideBikeBinding.read(b)),"bike B associated");
   check(java.util.Arrays.equals(gps,Files.readAllBytes(a.toPath())),"GPS bytes unchanged");
   rejection(()->RideTrackMerge.merge(base,a,b),"different-bike merge rejected");
   check(!RideTrackMerge.mergedSource(a)&&!RideTrackMerge.mergedSource(b),"merge rejection preserves both originals");
   RideBikeBinding.write(b,duke);
   RideTrackMerge.Result merged=RideTrackMerge.merge(base,a,b);
   check(duke.equals(RideBikeBinding.read(merged.combined)),"merge target inherits bike ID");
   ByteArrayOutputStream buffer=new ByteArrayOutputStream();
   BackupBundle.export("{\"version\":3}",base,buffer);
   try(RestoreArchive.Prepared prep=RestoreArchive.prepare(new ByteArrayInputStream(buffer.toByteArray()),restored)){
    prep.install(restored,j->{});
   }
   File restoredA=new File(restored,"rides/"+a.getName());
   File restoredMerge=new File(restored,"rides/"+merged.combined.getName());
   check(duke.equals(RideBikeBinding.read(restoredA)),"backup restores source association");
   check(duke.equals(RideBikeBinding.read(restoredMerge)),"backup restores merged association");
   check(java.util.Arrays.equals(gps,Files.readAllBytes(restoredA.toPath())),"GPS coordinates preserved through backup");
   // Corrupt a bike ID: fail the whole backup instead of silently dropping association.
   Files.write(new File(a.getPath()+".bike").toPath(),"INVALID".getBytes(StandardCharsets.UTF_8));
   rejection(()->BackupBundle.export("{\"version\":3}",base,new ByteArrayOutputStream()),"corrupt association blocks backup");
   rejection(()->RideBikeBinding.readStrict(a),"corrupt association blocks ODO application");
   rejection(()->RideTrackMerge.merge(base,a,b),"corrupt bike association blocks merge");
   // Reject a tampered backup before its data writer is ever invoked.
   ByteArrayOutputStream tampered=new ByteArrayOutputStream();
   try(ZipOutputStream zip=new ZipOutputStream(tampered)){
    zip.putNextEntry(new ZipEntry("MOTO-HUB-data.json"));
    zip.write("{\"version\":3}".getBytes(StandardCharsets.UTF_8));zip.closeEntry();
    zip.putNextEntry(new ZipEntry("rides/"+a.getName()));
    zip.write(gps);zip.closeEntry();
    zip.putNextEntry(new ZipEntry("rides/"+a.getName()+".bike"));
    zip.write("FORGED".getBytes(StandardCharsets.UTF_8));zip.closeEntry();
   }
   rejection(()->{try(RestoreArchive.Prepared ignored=RestoreArchive.prepare(
       new ByteArrayInputStream(tampered.toByteArray()),restored)){}},"tampered bike association rejected before restore");
   System.out.println("Ride bike association assertions passed: "+assertions);
  }finally{clean(base);clean(restored);}
 }
}
