package com.motohub.next;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

/** Restore MOTO HUB's exported ZIP without following archive paths or trusting external file sizes. */
public final class RestoreArchive {
 private RestoreArchive() {}
 private static final long MAX_TOTAL=BackupFormat.MAX_TOTAL_BYTES, MAX_ENTRY=BackupFormat.MAX_TRACK_BYTES;
 private static final int MAX_ENTRIES=BackupFormat.MAX_ENTRIES;
 private static final String PHOTO=BackupFormat.PHOTO;
 private static final String RIDE=BackupFormat.TRACK;
 public interface DataWriter {void write(String json)throws Exception;}
 public static final class Prepared implements AutoCloseable {
  private final File staging; public final String json; public final int photos,rides;
  private Prepared(File dir,String data,int p,int r){staging=dir;json=data;photos=p;rides=r;}
  /** Snapshot disk-space requirement before replacing any of the current assets. */
  public long requiredSpace(File internal)throws IOException {
   return RestoreCapacity.required(
    RestoreCapacity.sum(internal,existingAssets(internal)),
    RestoreCapacity.sum(staging,existingAssets(staging)),
    json.getBytes(StandardCharsets.UTF_8).length);
  }
  /** Replaces backed-up local assets and invokes caller's atomic JSON writer LAST. */
  public void install(File internal,DataWriter writer)throws Exception {
   // Existing assets are copied into a rollback directory while staged assets
   // are copied into place. Reserve space for BOTH copies before mutation.
   RestoreCapacity.ensure(RestoreCapacity.available(internal),requiredSpace(internal));
   File backup=new File(internal,"moto-rollback-"+UUID.randomUUID());
   if(!backup.mkdirs())throw new IOException("복원 안전 사본 폴더 생성 실패");
   List<String> old=existingAssets(internal);
   Set<String> existing=new HashSet<>(old);
   List<String> fresh=existingAssets(staging);
   boolean preserveRollback=false;
   try {
    for(String name:old)copy(new File(internal,name),new File(backup,name));
    for(String name:fresh){
     File staged=new File(staging,name),installed=new File(internal,name);
     copy(staged,installed);
     if(!sameContent(staged,installed))throw new IOException("복원된 파일 검증 실패: "+name);
    }
    // Remove obsolete recognized assets BEFORE committing JSON. A failed delete
    // must abort the restore, not leave a mixed old/new garage after success.
    // Rollback copies above already protect every original asset.
    Set<String> restoredNames=new HashSet<>(fresh);
    for(String name:old)if(!restoredNames.contains(name))
     Files.deleteIfExists(new File(internal,name).toPath());
    // The caller's atomic JSON write is the last committing operation.
    writer.write(json);
   }catch(Exception failure){
    for(String name:fresh)if(!existing.contains(name))new File(internal,name).delete();
    for(String name:old)try{
     File saved=new File(backup,name);
     if(saved.isFile())copy(saved,new File(internal,name));
     else {preserveRollback=true;failure.addSuppressed(new IOException("복구용 안전 사본 누락: "+name));}
    }catch(Exception e){preserveRollback=true;failure.addSuppressed(e);}
    throw failure;
   }finally{if(!preserveRollback)clean(backup);}
  }
  @Override public void close(){clean(staging);}
 }
 public static Prepared prepare(InputStream source,File internal)throws Exception{
  File stage=new File(internal,"moto-stage-"+UUID.randomUUID());if(!stage.mkdirs())throw new IOException("임시 폴더를 생성하지 못했어요.");
  String json=null;int photos=0,rides=0,entries=0;long total=0;
  Set<String> incomplete=new HashSet<>();
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
       marker=name.startsWith("rides/")&&(name.substring(6).matches(RIDE+"\\.done")||name.substring(6).matches(RIDE+"\\.imported")||name.substring(6).matches(RIDE+"\\.recovered")||name.substring(6).matches(RIDE+"\\.mergedinto")||name.substring(6).matches(RIDE+"\\.unmerged")||name.substring(6).matches(RIDE+"\\.category")||name.substring(6).matches(RIDE+"\\.bike")),
       gpx=name.startsWith("gpx/")&&name.substring(4).matches("[0-9]{10,15}-[a-f0-9-]{36}\\.gpx"),
       readme=name.equals("README-KO.txt");
    if(!data&&!photo&&!track&&!marker&&!gpx&&!readme)throw new IOException("지원하지 않는 ZIP 항목: "+name);
    long limit=data?8_000_000L:(marker?(name.endsWith(".mergedinto")?100L:(name.endsWith(".category")||name.endsWith(".bike")?64L:0L)):(gpx?50_000_000L:MAX_ENTRY));long size=0;
    ByteArrayOutputStream jsonBytes=data?new ByteArrayOutputStream():null;
    File destination=(photo||track||marker)?new File(stage,photo?name.substring(7):name):null;
    if(destination!=null&&!destination.getParentFile().isDirectory()&&!destination.getParentFile().mkdirs())throw new IOException("임시 파일 저장 실패");
    try(OutputStream out=destination==null?(jsonBytes==null?new OutputStream(){public void write(int b){}public void write(byte[] b,int off,int len){}}:jsonBytes):new BufferedOutputStream(new FileOutputStream(destination))){
     int n;while((n=zip.read(buffer))!=-1){size+=n;total+=n;if(size>limit||total>MAX_TOTAL)throw new IOException("백업 ZIP 용량 제한을 초과했어요.");out.write(buffer,0,n);}
    }
    if(data){json=new String(jsonBytes.toByteArray(),StandardCharsets.UTF_8);if(json.isEmpty())throw new IOException("JSON 기록이 비어 있어요.");}
    if(photo){if(size<4||!jpeg(destination))throw new IOException("잘못된 바이크 JPG 사진입니다.");if(++photos>BackupFormat.MAX_PHOTOS)throw new IOException("사진이 너무 많습니다.");}
    if(track){
     int points=TrackFile.read(destination,(a,b,c,d)->{});
     if(points<1){
      // An interrupted append can contain only one unfinished row. Preserve
      // those exact bytes for manual diagnostics, not fabricated GPX points.
      byte[] raw=Files.readAllBytes(destination.toPath());
      String partial=new String(raw,StandardCharsets.UTF_8);
      if(raw.length==0||raw.length>256||partial.indexOf('\n')>=0||!partial.matches("[0-9eE+.,-]+"))
       throw new IOException("복원할 수 없는 GPS 기록입니다.");
      incomplete.add(name);
     }
     if(++rides>BackupFormat.MAX_TRACKS)throw new IOException("주행 기록이 10,000건을 초과합니다.");
    }
    zip.closeEntry();
   }
   if(json==null)throw new IOException("MOTO-HUB-data.json이 들어 있지 않아요.");
   for(String name:incomplete)if(seen.contains(name+".done")||seen.contains(name+".imported")||seen.contains(name+".recovered")||seen.contains(name+".mergedinto"))
    throw new IOException("완료 처리된 GPS 기록은 좌표가 있어야 합니다.");
   for(String name:seen)if(name.startsWith("rides/")&&name.endsWith(".mergedinto")){
    File link=new File(stage,name);
    String target=new String(Files.readAllBytes(link.toPath()),StandardCharsets.US_ASCII);
    if(!target.matches(RIDE)||!seen.contains("rides/"+target)||!seen.contains("rides/"+target+".done"))
     throw new IOException("통합 라이딩 대상이 누락되거나 손상됐어요.");
   }
   for(String name:seen)if(name.startsWith("rides/")&&(name.endsWith(".done")||name.endsWith(".imported")||name.endsWith(".recovered")||name.endsWith(".mergedinto")||name.endsWith(".unmerged")||name.endsWith(".category")||name.endsWith(".bike")) &&!seen.contains(name.substring(0,name.lastIndexOf('.'))))throw new IOException("원본 GPS가 없는 기록 표시입니다.");
   for(String name:seen)if(name.startsWith("rides/")&&name.endsWith(".bike")){
    String id=new String(Files.readAllBytes(new File(stage,name).toPath()),StandardCharsets.US_ASCII);
    if(!id.matches("[0-9a-fA-F]{8}(-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}"))throw new IOException("잘못된 바이크 연결 정보입니다.");
   }
   for(String name:seen)if(name.startsWith("rides/")&&name.endsWith(".category")){
    File label=new File(stage,name);
    if(!java.util.Arrays.asList("자동 분류","일상","출퇴근","와인딩","장거리","투어").contains(new String(Files.readAllBytes(label.toPath()),StandardCharsets.UTF_8)))throw new IOException("지원하지 않는 라이딩 분류입니다.");
   }
   for(String name:seen)if(name.startsWith("rides/")&&name.endsWith(".unmerged")&&!seen.contains(name.substring(0,name.length()-9)+".done"))
    throw new IOException("완료되지 않은 통합 기록입니다.");
   return new Prepared(stage,json,photos,rides);
  }catch(Exception e){clean(stage);throw e;}
 }
 private static boolean jpeg(File f)throws IOException{
  try(RandomAccessFile in=new RandomAccessFile(f,"r")){if(in.readUnsignedByte()!=0xff||in.readUnsignedByte()!=0xd8)return false;in.seek(in.length()-2);return in.readUnsignedByte()==0xff&&in.readUnsignedByte()==0xd9;}
 }
 private static List<String> existingAssets(File root){
  List<String> out=new ArrayList<>();File[] photos=root.listFiles((d,n)->n.matches(PHOTO));if(photos!=null)for(File f:photos)if(f.isFile())out.add(f.getName());
  File rides=new File(root,"rides");File[] files=rides.listFiles();if(files!=null)for(File f:files){String n=f.getName();if(f.isFile()&&(n.matches(RIDE)||n.matches(RIDE+"\\.done")||n.matches(RIDE+"\\.imported")||n.matches(RIDE+"\\.recovered")||n.matches(RIDE+"\\.mergedinto")||n.matches(RIDE+"\\.unmerged")||n.matches(RIDE+"\\.category")||n.matches(RIDE+"\\.bike")))out.add("rides/"+n);}return out;
 }
 private static boolean sameContent(File a,File b)throws IOException{
  if(a.length()!=b.length())return false;
  try(InputStream x=new BufferedInputStream(new FileInputStream(a));InputStream y=new BufferedInputStream(new FileInputStream(b))){
   byte[] left=new byte[16384],right=new byte[16384];int n;
   while((n=x.read(left))!=-1){int done=0;while(done<n){int read=y.read(right,done,n-done);if(read<0)return false;done+=read;}
    for(int i=0;i<n;i++)if(left[i]!=right[i])return false;
   }
   return y.read()==-1;
  }
 }
 private static void copy(File source,File dest)throws IOException{
  if(!dest.getParentFile().isDirectory()&&!dest.getParentFile().mkdirs())throw new IOException("복원 폴더를 만들지 못했어요.");
  File temp=new File(dest.getParentFile(),dest.getName()+".restoring");
  try{Files.copy(source.toPath(),temp.toPath(),StandardCopyOption.REPLACE_EXISTING);Files.move(temp.toPath(),dest.toPath(),StandardCopyOption.REPLACE_EXISTING);}finally{temp.delete();}
 }
 private static void clean(File path){if(path.isDirectory()){File[] children=path.listFiles();if(children!=null)for(File child:children)clean(child);}path.delete();}
}
