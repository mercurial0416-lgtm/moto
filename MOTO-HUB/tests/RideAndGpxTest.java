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
   System.out.println("PASS ride & GPX: "+checks+" assertions");
  }finally{for(File file:Objects.requireNonNull(folder.listFiles()))file.delete();folder.delete();}
 }
}
