package com.motohub.next;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

/** Export a GPX without samples near either endpoint. Original .track is never changed. */
final class TrackPrivacy {
 private TrackPrivacy() {}
 static final int[] CHOICES_METERS = {300, 500, 1000};
 static boolean supported(int radius) {
  if (radius == 0) return true;
  for (int choice : CHOICES_METERS) if (choice == radius) return true;
  return false;
 }
 static final class Bounds { double firstLat, firstLon, lastLat, lastLon; int count; }
 static boolean show(double lat,double lon,Bounds b,int metres) {
  return RideInsights.meters(lat,lon,b.firstLat,b.firstLon)>metres &&
         RideInsights.meters(lat,lon,b.lastLat,b.lastLon)>metres;
 }
 static int gpx(File source,OutputStream output,int radiusMeters) throws Exception {
  if (!supported(radiusMeters)) throw new IllegalArgumentException("지원하지 않는 위치 보호 거리");
  if (radiusMeters == 0) return TrackFile.gpx(source,output);
  if (source == null || !source.isFile()) throw new IOException("GPS 원본 파일을 찾을 수 없어요");
  final long originalBytes = source.length(), originalModified = source.lastModified();
  final Bounds b=new Bounds();
  TrackFile.read(source,(lat,lon,accuracy,time)->{
   if(b.count==0){b.firstLat=lat;b.firstLon=lon;}
   b.lastLat=lat;b.lastLon=lon;b.count++;
  });
  if(b.count<3) throw new IOException("공유할 만큼 긴 라이딩 기록이 아닙니다");
  final int[] retained={0};
  TrackFile.read(source,(lat,lon,accuracy,time)->{if(show(lat,lon,b,radiusMeters))retained[0]++;});
  if(retained[0]<2)throw new IOException("보호 거리를 적용하면 남는 경로가 2점 미만입니다. 더 긴 라이딩만 공유하세요.");
  if(source.length()!=originalBytes||source.lastModified()!=originalModified)
   throw new IOException("파일이 변경되어 GPX 내보내기를 중단했습니다");
  Writer out=new OutputStreamWriter(output,StandardCharsets.UTF_8);
  out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<gpx version=\"1.1\" creator=\"MOTO HUB privacy export\" xmlns=\"http://www.topografix.com/GPX/1/1\"><trk><name>Shared ride</name>\n");
  final boolean[] opened={false};final int[] emitted={0};
  TrackFile.read(source,(lat,lon,accuracy,time)->{
   if(!show(lat,lon,b,radiusMeters)){
    if(opened[0]){out.write("</trkseg>\n");opened[0]=false;}
    return;
   }
   if(!opened[0]){out.write("<trkseg>\n");opened[0]=true;}
   out.write("<trkpt lat=\""+lat+"\" lon=\""+lon+"\"><time>"+Instant.ofEpochMilli(time)+"</time></trkpt>\n");emitted[0]++;
  });
  if(opened[0])out.write("</trkseg>\n");
  out.write("</trk></gpx>\n");out.flush();
  return emitted[0];
 }
}
