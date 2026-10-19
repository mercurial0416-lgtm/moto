package com.motohub.next;
import java.io.*;import java.nio.charset.StandardCharsets;import java.time.Instant;import java.util.*;
/** Append-only local tracks. Never synthesizes or uploads coordinates. */
final class TrackFile {
 static final long MAX_BYTES=25_000_000;
 static boolean valid(double lat,double lon,double accuracy,long time){return Double.isFinite(lat)&&Double.isFinite(lon)&&Double.isFinite(accuracy)&&lat>=-90&&lat<=90&&lon>=-180&&lon<=180&&accuracy>=0&&accuracy<=100&&time>0;}
 static String point(double lat,double lon,double accuracy,long time){if(!valid(lat,lon,accuracy,time))throw new IllegalArgumentException("유효하지 않은 GPS 좌표");return lat+","+lon+","+accuracy+","+time+"\n";}
 static File directory(File files)throws IOException{File dir=new File(files,"rides");if(!dir.isDirectory()&&!dir.mkdirs())throw new IOException("경로 폴더 생성 실패");return dir;}
 /** Delete only a zero-byte uncommitted session; preserve any partial GPS data for recovery. */
 static boolean discardEmptySession(File track)throws IOException{
  if(track==null||!track.isFile()||track.length()!=0)return false;
  if(new File(track.getPath()+".done").exists()||new File(track.getPath()+".recovered").exists())return false;
  if(!track.delete())throw new IOException("빈 GPS 파일 정리 실패");
  File bike=new File(track.getPath()+".bike");
  if(bike.exists()&&!bike.delete())throw new IOException("빈 GPS 파일의 바이크 연결 정보 정리 실패");
  return true;
 }
 static List<File> tracks(File files)throws IOException{File[] a=directory(files).listFiles((d,n)->n.matches("[0-9]{10,15}-[a-f0-9-]{36}\\.track"));List<File> list=new ArrayList<>(Arrays.asList(a==null?new File[0]:a));list.sort((x,y)->y.getName().compareTo(x.getName()));return list;}
 interface Consumer{void point(double lat,double lon,double accuracy,long time)throws Exception;}
 static int read(File f,Consumer consumer)throws Exception{
  if(f.length()>MAX_BYTES)throw new IOException("경로 파일 크기 초과");int count=0;long previous=0;
  try(BufferedInputStream in=new BufferedInputStream(new FileInputStream(f))){ByteArrayOutputStream line=new ByteArrayOutputStream();int b;while((b=in.read())!=-1){if(b!='\n'){if(line.size()>256)throw new IOException("손상된 경로 행");line.write(b);continue;}String[] s=line.toString("UTF-8").split(",",-1);line.reset();if(s.length!=4)throw new IOException("손상된 경로");double lat=Double.parseDouble(s[0]),lon=Double.parseDouble(s[1]),accuracy=Double.parseDouble(s[2]);long time=Long.parseLong(s[3]);if(!valid(lat,lon,accuracy,time)||time<=previous)throw new IOException("유효하지 않은 경로 순서");consumer.point(lat,lon,accuracy,time);previous=time;count++;}/* A crash may leave an incomplete final row. Only newline-committed points are exported. */}return count;
 }
 static int gpx(File source,OutputStream output)throws Exception{
  // Validate first so corrupted records never become a seemingly complete GPX.
  int count=read(source,(a,b,c,d)->{});if(count==0)throw new IOException("저장된 GPS 좌표가 없습니다.");
  Writer out=new OutputStreamWriter(output,StandardCharsets.UTF_8);out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<gpx version=\"1.1\" creator=\"MOTO HUB\" xmlns=\"http://www.topografix.com/GPX/1/1\"><trk><name>MOTO HUB ride</name><trkseg>\n");
  read(source,(lat,lon,accuracy,time)->out.write("<trkpt lat=\""+lat+"\" lon=\""+lon+"\"><time>"+Instant.ofEpochMilli(time)+"</time></trkpt>\n"));out.write("</trkseg></trk></gpx>\n");out.flush();return count;
 }
}
