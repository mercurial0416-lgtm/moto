package com.motohub.next;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.time.Instant;
import java.util.UUID;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

/** Bounded GPX 1.0/1.1 importer. Rejects DTD/entities, out-of-order and fabricated timestamps. */
public final class GpxImporter {
 private GpxImporter(){}
 static final long MAX_INPUT_BYTES=TrackFile.MAX_BYTES;
 static final int MAX_IMPORT_POINTS=200_000;
 public static final class Imported {
  public final String name;public final int count;
  Imported(String n,int c){name=n;count=c;}
 }
 public static Imported importTrack(InputStream stream,File rides)throws Exception{
  if(rides==null||Files.isSymbolicLink(rides.toPath()))throw new IOException("안전하지 않은 GPS 저장 폴더입니다.");
  if(!Files.isDirectory(rides.toPath(),LinkOption.NOFOLLOW_LINKS)&&!rides.mkdirs())throw new IOException("GPS 저장 폴더 생성 실패");
  File temp=File.createTempFile("import-", ".part",rides);
  try{
   final int[] count={0};final long[] last={0},written={0};
   try(OutputStream out=new BufferedOutputStream(new FileOutputStream(temp))){
    SAXParserFactory factory=SAXParserFactory.newInstance();factory.setNamespaceAware(true);factory.setXIncludeAware(false);
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);
    factory.setFeature("http://xml.org/sax/features/external-general-entities",false);
    factory.setFeature("http://xml.org/sax/features/external-parameter-entities",false);
    factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd",false);
    factory.newSAXParser().parse(new BoundedInputStream(stream,MAX_INPUT_BYTES),new DefaultHandler(){
     boolean inPoint=false,inTime=false,seenTime=false;double lat,lon;StringBuilder text=new StringBuilder();long stamp=0;
     String namespace;int pointDepth=0;
     final List<String> path=new ArrayList<>();
     final List<String> trackPath=Arrays.asList("gpx","trk","trkseg"),routePath=Arrays.asList("gpx","rte");
     @Override public void startElement(String uri,String local,String qName,Attributes attrs)throws SAXException{
      String tag=local.isEmpty()?qName:local;
      String elementNamespace=uri==null?"":uri;
      if(path.isEmpty()){
       if(!tag.equals("gpx")||!(elementNamespace.isEmpty()||elementNamespace.equals("http://www.topografix.com/GPX/1/0")||elementNamespace.equals("http://www.topografix.com/GPX/1/1")))
        throw new SAXException("GPX 문서 형식이 아닙니다");
       namespace=elementNamespace;
      }
      if(path.size()>=64)throw new SAXException("GPX 문서 구조가 너무 깊습니다");
      boolean nativeElement=elementNamespace.equals(namespace);
      if(inTime)throw new SAXException("GPX 시간 안에는 다른 요소를 넣을 수 없습니다");
      boolean point=nativeElement&&((tag.equals("trkpt")&&path.equals(trackPath))||(tag.equals("rtept")&&path.equals(routePath)));
      if(point){
       if(inPoint)throw new SAXException("GPX 좌표 구조 오류");
       inPoint=true;stamp=0;seenTime=false;pointDepth=path.size()+1;
       try{lat=Double.parseDouble(attrs.getValue("lat"));lon=Double.parseDouble(attrs.getValue("lon"));}
       catch(Exception e){throw new SAXException("GPX 좌표 오류");}
       if(!TrackFile.valid(lat,lon,0,1))throw new SAXException("GPX 좌표 범위 오류");
      }else if(nativeElement&&inPoint&&tag.equals("time")&&path.size()==pointDepth){
       if(seenTime)throw new SAXException("GPX 좌표 시간이 중복됐습니다");
       seenTime=true;inTime=true;text.setLength(0);
      }
      // Foreign extension elements can never masquerade as GPX containers/time.
      path.add(nativeElement?tag:"");
     }
     @Override public void characters(char[] ch,int st,int n)throws SAXException{
      if(inTime){if(text.length()+n>100)throw new SAXException("GPX 시간 문자열이 너무 깁니다");text.append(ch,st,n);}
     }
     @Override public void endElement(String uri,String local,String qName)throws SAXException{
      String tag=local.isEmpty()?qName:local;
      boolean nativeElement=(uri==null?"":uri).equals(namespace);
      if(inTime&&nativeElement&&tag.equals("time")&&path.size()==pointDepth+1){
       try{stamp=Instant.parse(text.toString().trim()).toEpochMilli();}
       catch(Exception e){throw new SAXException("GPX 시간 정보가 잘못됐어요");}inTime=false;
      }
      if(inPoint&&nativeElement&&path.size()==pointDepth&&(tag.equals("trkpt")||tag.equals("rtept"))){
       if(stamp==0)throw new SAXException("시간 없는 GPX 코스는 라이딩 기록으로 가져올 수 없어요");
       if(stamp<=last[0])throw new SAXException("시간 역순 또는 중복 GPX 기록입니다");
       if(++count[0]>MAX_IMPORT_POINTS)throw new SAXException("GPX 좌표 최대 20만 개까지 지원합니다");
       try{
        byte[] point=TrackFile.point(lat,lon,0,stamp).getBytes(StandardCharsets.UTF_8);
        if(point.length>TrackFile.MAX_BYTES-written[0])throw new IOException("변환된 GPS 원본 25MB 한도 초과");
        out.write(point);written[0]+=point.length;
       }catch(Exception e){throw new SAXException("GPS 기록 저장 실패",e);}
       last[0]=stamp;inPoint=false;
      }
      path.remove(path.size()-1);
     }
    });out.flush();
   }
   if(count[0]<2)throw new IOException("GPX에 시간이 기록된 좌표가 2개 이상 필요합니다");
   String name=System.currentTimeMillis()+"-"+UUID.randomUUID()+".track";
   File dest=new File(rides,name);
   commitImport(temp,dest);
   return new Imported(name,count[0]);
  }finally{temp.delete();}
 }
 /** Package-private so collision/rollback behavior can be tested without guessing UUIDs. */
 static void commitImport(File temp,File dest)throws IOException{
   // Flush the GPS bytes before creating a completed/imported representation.
   try(FileOutputStream durable=new FileOutputStream(temp,true)){durable.getFD().sync();}
   // Never replace an existing ride, even in the unlikely event of a UUID collision.
   Files.move(temp.toPath(),dest.toPath());
   File done=new File(dest.getPath()+".done"),imported=new File(dest.getPath()+".imported");
   boolean createdDone=false,createdImported=false;
   try{
    // These sidecars are part of the import transaction: without .imported,
    // historical GPX is incorrectly attributed to today's ride statistics.
    // Classify a historical import durably before making it completed/eligible.
    createdImported=imported.createNewFile();
    if(!createdImported)throw new IOException("GPX 가져오기 상태 파일이 이미 존재합니다");
    try(FileOutputStream durable=new FileOutputStream(imported,true)){durable.getFD().sync();}
    createdDone=done.createNewFile();
    if(!createdDone)throw new IOException("GPX 완료 상태 파일이 이미 존재합니다");
    try(FileOutputStream durable=new FileOutputStream(done,true)){durable.getFD().sync();}
   }catch(IOException failure){
    if(createdImported)try{Files.deleteIfExists(imported.toPath());}catch(IOException cleanup){failure.addSuppressed(cleanup);}
    if(createdDone)try{Files.deleteIfExists(done.toPath());}catch(IOException cleanup){failure.addSuppressed(cleanup);}
    try{Files.deleteIfExists(dest.toPath());}catch(IOException cleanup){failure.addSuppressed(cleanup);}
    throw new IOException("GPX 가져오기 상태 저장 실패 · 원본 파일은 유지됩니다",failure);
   }
 }
 private static final class BoundedInputStream extends FilterInputStream {
  long count=0;final long max;
  BoundedInputStream(InputStream in,long m){super(in);max=m;}
  @Override public int read()throws IOException{int v=super.read();if(v!=-1&&++count>max)throw new IOException("GPX 파일은 25MB 이하만 지원해요");return v;}
  @Override public int read(byte[] b,int off,int len)throws IOException{int n=super.read(b,off,(int)Math.min(len,Math.max(1,max-count+1)));if(n>0&&(count+=n)>max)throw new IOException("GPX 파일은 25MB 이하만 지원해요");return n;}
 }
}
