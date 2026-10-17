package com.motohub.next;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

/** Restore MOTO HUB's exported ZIP without following archive paths or trusting external file sizes. */
public final class RestoreArchive {
 private RestoreArchive() {}
 private static final long MAX_TOTAL=250_000_000L, MAX_ENTRY=25_000_000L;
 private static final int MAX_ENTRIES=6000;
 private static final String PHOTO="garage-photo-[a-fA-F0-9-]{36}\\.jpg";
 private static final String RIDE="[0-9]{10,15}-[a-f0-9-]{36}\\.track";
 public interface DataWriter {void write(String json)throws Exception;}
 public static final class Prepared implements AutoCloseable {
  private final File staging; public final String json; public final int photos,rides;
  private Prepared(File dir,String data,int p,int r){staging=dir;json=data;photos=p;rides=r;}
  /** Replaces backed-up local assets and invokes caller's atomic JSON writer LAST. */
  public void install(File internal,DataWriter writer)throws Exception {
   File backup=new File(internal,"moto-rollback-"+UUID.randomUUID());
   if(!backup.mkdirs())throw new IOException("복원 안전 사본 폴더 생성 실패");
   List<String> old=existingAssets(internal);
   List<String> fresh=existingAssets(staging);
   boolean retainBackup=false;
   boolean installationStarted=false;
   try {
    for(String name:old)copy(new File(internal,name),new File(backup,name));
    installationStarted=true;
    for(String name:fresh)copy(new File(staging,name),new File(internal,name));
    writer.write(json);
    // JSON saved successfully. Remove only recognized assets not present in the archive.
    for(String name:old)if(!fresh.contains(name))new File(internal,name).delete();
   }catch(Exception failure){
    if(installationStarted){
     for(String name:fresh)if(!old.contains(name)){
      File added=new File(internal,name);
      if(added.exists()&&!added.delete()){retainBackup=true;failure.addSuppressed(new IOException("새 파일 제거 실패: "+name));}
     }
     for(String name:old)try{copy(new File(backup,name),new File(internal,name));}catch(Exception e){retainBackup=true;failure.addSuppressed(e);}
    }
    if(retainBackup)throw new IOException("복원 및 원상복구에 실패했어요. 안전 사본을 보존했습니다: "+backup.getName(),failure);
    throw failure;
   }finally{if(!retainBackup)clean(backup);}
  }
  @Override public void close(){clean(staging);}
 }
 public static Prepared prepare(InputStream source,File internal)throws Exception{
  File stage=new File(internal,"moto-stage-"+UUID.randomUUID());if(!stage.mkdirs())throw new IOException("임시 폴더를 생성하지 못했어요.");
  String json=null;int photos=0,rides=0,entries=0;long total=0;
  Set<String> seen=new HashSet<>();byte[] buffer=new byte[32768];
  try(ZipInputStream zip=new ZipInputStream(new BufferedInputStream(source),StandardCharsets.UTF_8)){
   ZipEntry entry;
   while((entry=zip.getNextEntry())!=null){
    String name=entry.getName();if(++entries>MAX_ENTRIES)throw new IOException("백업 항목이 너무 많아요.");
    if(!seen.add(name))throw new IOException("중복 파일: "+name);
    if(entry.isDirectory()){
     if(!name.equals("rides/")&&!name.equals("photos/")&&!name.equals("gpx/"))throw new IOException("지원하지 않는 폴더입니다.");
     zip.closeEntry();continue;
    }
    if(name.contains("..")||name.contains("\\")||name.startsWith("/")||name.indexOf('\0')>=0)throw new IOException("안전하지 않은 ZIP 경로입니다.");
    boolean data=name.equals("MOTO-HUB-data.json"),
       photo=name.startsWith("photos/")&&name.substring(7).matches(PHOTO),
       track=name.startsWith("rides/")&&name.substring(6).matches(RIDE),
       marker=name.startsWith("rides/")&&(name.substring(6).matches(RIDE+"\\.done")||name.substring(6).matches(RIDE+"\\.imported")||name.substring(6).matches(RIDE+"\\.recovered")),
       gpx=name.startsWith("gpx/")&&name.substring(4).matches("[0-9]{10,15}-[a-f0-9-]{36}\\.gpx"),
       readme=name.equals("README-KO.txt");
    if(!data&&!photo&&!track&&!marker&&!gpx&&!readme)throw new IOException("지원하지 않는 ZIP 항목: "+name);
    long limit=data?8_000_000L:(marker?0L:(gpx?50_000_000L:MAX_ENTRY));long size=0;
    ByteArrayOutputStream jsonBytes=data?new ByteArrayOutputStream():null;
    File destination=(photo||track||marker)?new File(stage,photo?name.substring(7):name):null;
    if(destination!=null&&!destination.getParentFile().isDirectory()&&!destination.getParentFile().mkdirs())throw new IOException("임시 파일 저장 실패");
    try(OutputStream out=destination==null?(jsonBytes==null?new OutputStream(){public void write(int b){}public void write(byte[] b,int off,int len){}}:jsonBytes):new BufferedOutputStream(new FileOutputStream(destination))){
     int n;while((n=zip.read(buffer))!=-1){size+=n;total+=n;if(size>limit||total>MAX_TOTAL)throw new IOException("백업 ZIP 용량 제한을 초과했어요.");out.write(buffer,0,n);}
    }
    if(data){json=new String(jsonBytes.toByteArray(),StandardCharsets.UTF_8);if(json.isEmpty())throw new IOException("JSON 기록이 비어 있어요.");}
    if(photo){if(size<4||!jpeg(destination))throw new IOException("잘못된 바이크 JPG 사진입니다.");photos++;}
    if(track){if(TrackFile.read(destination,(a,b,c,d)->{})<1)throw new IOException("비어 있는 GPS 기록입니다.");rides++;}
    zip.closeEntry();
   }
   if(json==null)throw new IOException("MOTO-HUB-data.json이 들어 있지 않아요.");
   for(String name:seen)if(name.startsWith("rides/")&&(name.endsWith(".done")||name.endsWith(".imported")||name.endsWith(".recovered")) &&!seen.contains(name.substring(0,name.lastIndexOf('.'))))throw new IOException("원본 GPS가 없는 기록 표시입니다.");
   return new Prepared(stage,json,photos,rides);
  }catch(Exception e){clean(stage);throw e;}
 }
 private static boolean jpeg(File f)throws IOException{
  try(RandomAccessFile in=new RandomAccessFile(f,"r")){if(in.readUnsignedByte()!=0xff||in.readUnsignedByte()!=0xd8)return false;in.seek(in.length()-2);return in.readUnsignedByte()==0xff&&in.readUnsignedByte()==0xd9;}
 }
 private static List<String> existingAssets(File root){
  List<String> out=new ArrayList<>();File[] photos=root.listFiles((d,n)->n.matches(PHOTO));if(photos!=null)for(File f:photos)if(f.isFile())out.add(f.getName());
  File rides=new File(root,"rides");File[] files=rides.listFiles();if(files!=null)for(File f:files){String n=f.getName();if(f.isFile()&&(n.matches(RIDE)||n.matches(RIDE+"\\.done")||n.matches(RIDE+"\\.imported")||n.matches(RIDE+"\\.recovered")))out.add("rides/"+n);}return out;
 }
 private static void copy(File source,File dest)throws IOException{
  if(!dest.getParentFile().isDirectory()&&!dest.getParentFile().mkdirs())throw new IOException("복원 폴더를 만들지 못했어요.");
  File temp=new File(dest.getParentFile(),dest.getName()+".restoring");
  try{Files.copy(source.toPath(),temp.toPath(),StandardCopyOption.REPLACE_EXISTING);Files.move(temp.toPath(),dest.toPath(),StandardCopyOption.REPLACE_EXISTING);}finally{temp.delete();}
 }
 private static void clean(File path){if(path.isDirectory()){File[] children=path.listFiles();if(children!=null)for(File child:children)clean(child);}path.delete();}
}
