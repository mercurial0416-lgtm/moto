package com.motohub.next;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
public class RideAndGpxTest {
 static int checks=0;
 static void ok(boolean v,String why){checks++;if(!v)throw new AssertionError(why);}
 static void failXml(String xml,File dir){boolean fail=false;try{GpxImporter.importTrack(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)),dir);}catch(Exception e){fail=true;}ok(fail,"invalid GPX accepted");}
 public static void main(String[] args)throws Exception{
  File folder=Files.createTempDirectory("motohub-gpx").toFile();
  try{
   String head="<?xml version='1.0'?><gpx xmlns='http://www.topografix.com/GPX/1/1'><trk><trkseg>";
   String tail="</trkseg></trk></gpx>";
   String p1="<trkpt lat='37.0' lon='127.0'><time>2026-10-16T05:00:00Z</time></trkpt>";
   String p2="<trkpt lat='37.001' lon='127.0'><time>2026-10-16T05:00:10Z</time></trkpt>";
   GpxImporter.Imported ride=GpxImporter.importTrack(new ByteArrayInputStream((head+p1+p2+tail).getBytes(StandardCharsets.UTF_8)),folder);
   File f=new File(folder,ride.name);ok(f.isFile(),"not saved");ok(ride.count==2,"count");
   ok(new File(f.getPath()+".done").isFile(),"import completion marker");
   ok(new File(f.getPath()+".imported").isFile(),"historical GPX timestamp marker");
   RideInsights.Result summary=RideInsights.analyze(f);
   ok(summary.points==2,"stats count");ok(summary.distanceKm>.10&&summary.distanceKm<.12,"distance");
   ok(summary.durationMillis==10000,"duration");ok(summary.averageKmh>39&&summary.averageKmh<41,"average");
   ByteArrayOutputStream gpx=new ByteArrayOutputStream();ok(TrackFile.gpx(f,gpx)==2,"roundtrip export");
   String text=gpx.toString("UTF-8");ok(text.contains("trkpt")&&text.contains("2026-10-16"),"roundtrip xml");
   failXml("<!DOCTYPE gpx [ <!ENTITY xxe SYSTEM 'file:///etc/hosts'> ]><gpx>&xxe;</gpx>",folder);
   failXml(head+p1+p1+tail,folder); // duplicate timestamp
   failXml(head+"<trkpt lat='100' lon='127'><time>2026-10-16T05:00:00Z</time></trkpt>"+p2+tail,folder);
   failXml(head+"<trkpt lat='37' lon='127'/>"+p2+tail,folder);
   failXml(head+p1+tail,folder);
   ok(RideInsights.meters(0,0,0,0)==0,"zero meters");
   ok(RideInsights.meters(0,0,0,1)>100000,"haversine");
   ok(Arrays.stream(Objects.requireNonNull(folder.listFiles())).filter(file->file.getName().endsWith(".track")).count()==1,"failed import temp files leaked");
   // The new track must never be visible without both metadata sidecars.
   File collision=new File(folder,"collision.track");Files.writeString(collision.toPath(),"original");
   File staged=File.createTempFile("new-", ".part",folder);Files.writeString(staged.toPath(),"new");
   boolean rejected=false;try{GpxImporter.commitImport(staged,collision);}catch(IOException expected){rejected=true;}
   ok(rejected,"existing ride collision rejected");
   ok(Files.readString(collision.toPath()).equals("original"),"existing ride not overwritten");
   ok(staged.exists(),"staged ride remains on name collision");staged.delete();collision.delete();
   File markerCollision=new File(folder,"marker-collision.track");
   File importedMarker=new File(markerCollision.getPath()+".imported");
   Files.writeString(importedMarker.toPath(),"keep");
   staged=File.createTempFile("new-", ".part",folder);Files.writeString(staged.toPath(),"new");
   rejected=false;try{GpxImporter.commitImport(staged,markerCollision);}catch(IOException expected){rejected=true;}
   ok(rejected,"preexisting metadata marker rejected");
   ok(!markerCollision.exists(),"partial track rolled back");
   ok(!new File(markerCollision.getPath()+".done").exists(),"created done marker rolled back");
   ok(Files.readString(importedMarker.toPath()).equals("keep"),"preexisting marker preserved");
   importedMarker.delete();
   System.out.println("PASS ride & GPX: "+checks+" assertions");
  }finally{for(File file:Objects.requireNonNull(folder.listFiles()))file.delete();folder.delete();}
 }
}
