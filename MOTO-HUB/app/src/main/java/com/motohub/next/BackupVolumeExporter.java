package com.motohub.next;

import android.content.Context;
import android.content.ContentResolver;
import android.net.Uri;
import android.provider.DocumentsContract;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.*;
import org.json.JSONObject;

/** Bounded, individually verified ZIP volumes in a user-chosen SAF folder.
 * A completed INDEX is written last; without it a partial backup is not complete. */
final class BackupVolumeExporter {
 private static final long MAX_VOLUME_BYTES=230_000_000L;
 private BackupVolumeExporter(){}
 static final class Volume {
  final Set<String> entries=new LinkedHashSet<>();
  long size;
 }
 static final class Group {
  final Set<String> entries=new LinkedHashSet<>();
  long bytes;
 }
 private static boolean regular(File f)throws IOException{
  if(Files.isSymbolicLink(f.toPath())||
     !Files.isRegularFile(f.toPath(),LinkOption.NOFOLLOW_LINKS))throw new IOException("백업에 안전하지 않은 파일: "+f.getName());
  return true;
 }
 private static void add(Group group,String name,File file)throws IOException{
  regular(file);
  if(!BackupFormat.allowed(name,false))throw new IOException("지원하지 않는 백업 원본 "+name);
  if(file.length()>BackupFormat.entryLimit(name))throw new IOException("파일 크기 초과: "+name);
  if(group.entries.add(name))group.bytes+=file.length();
 }
 private static void addSidecars(Group group,File folder,String prefix,String id)throws IOException{
  for(String suffix:RideTrash.SIDECARS){
   File file=new File(folder,id+suffix);
   if(!Files.exists(file.toPath(),LinkOption.NOFOLLOW_LINKS))continue;
   add(group,prefix+file.getName(),file);
  }
 }
 private static String root(Map<String,String> parent,String key){
  String value=parent.get(key);
  if(value==null){parent.put(key,key);return key;}
  if(value.equals(key))return key;
  String top=root(parent,value);parent.put(key,top);return top;
 }
 private static void join(Map<String,String> parent,String a,String b){parent.put(root(parent,a),root(parent,b));}
 static List<Volume> plan(File internal,String part,long jsonBytes,long prefsBytes)throws IOException{
  return plan(internal,part,jsonBytes,prefsBytes,MAX_VOLUME_BYTES);
 }
 /** Exposed to tests with a small synthetic limit; production always uses a lower-than-ZIP cap. */
 static List<Volume> plan(File internal,String part,long jsonBytes,long prefsBytes,long limit)throws IOException{
  if(!"GPS".equals(part)&&!"MEDIA".equals(part))throw new IOException("분할 유형 오류");
  long overhead=jsonBytes+prefsBytes+32_768;
  if(overhead>=limit)throw new IOException("데이터 JSON이 한 ZIP에 담기지 않습니다.");
  List<Group> grouped=new ArrayList<>();
  if("GPS".equals(part)){
   File rides=TrackFile.directory(internal);
   Map<String,Group> groups=new LinkedHashMap<>();
   Map<String,String> parent=new HashMap<>();
   for(File track:TrackFile.tracks(internal)){
    String name=track.getName();
    if(track.length()==0)continue; // Same skipped uncommitted empty session as exporter.
    Group group=new Group();
    add(group,"rides/"+name,track);
    addSidecars(group,rides,"rides/",name);
    groups.put(name,group);parent.put(name,name);
   }
   for(Map.Entry<String,Group> item:groups.entrySet()){
    File link=new File(rides,item.getKey()+".mergedinto");
    if(!link.exists())continue;
    regular(link);
    String target=new String(Files.readAllBytes(link.toPath()),StandardCharsets.US_ASCII);
    if(!groups.containsKey(target))throw new IOException("통합 GPS 대상이 누락됐습니다.");
    join(parent,item.getKey(),target);
   }
   Map<String,Group> connected=new LinkedHashMap<>();
   for(Map.Entry<String,Group> item:groups.entrySet()){
    Group group=connected.computeIfAbsent(root(parent,item.getKey()),x->new Group());
    group.entries.addAll(item.getValue().entries);group.bytes+=item.getValue().bytes;
   }
   grouped.addAll(connected.values());
   for(File track:RideTrash.entries(internal)){
    String id=track.getName(),prefix="rides-trash/"+id+"/";
    Group group=new Group();
    add(group,prefix+id,track);
    addSidecars(group,track.getParentFile(),prefix,id);
    grouped.add(group);
   }
  }else{
   File[] photos=internal.listFiles((dir,name)->name.matches(BackupFormat.PHOTO));
   if(photos!=null)for(File file:photos){
    Group group=new Group();add(group,"photos/"+file.getName(),file);grouped.add(group);
   }
   File[] receipts=internal.listFiles((dir,name)->name.matches(BackupFormat.RECEIPT));
   if(receipts!=null)for(File file:receipts){
    Group group=new Group();add(group,"receipts/"+file.getName(),file);grouped.add(group);
   }
   File modelFolder=new File(internal,"moto-year-photos");
   if(Files.isSymbolicLink(modelFolder.toPath()))throw new IOException("모델 사진 폴더가 안전하지 않습니다.");
   File[] models=modelFolder.listFiles((dir,name)->name.matches(BackupFormat.MODEL_PHOTO));
   if(models!=null)for(File image:models){
    File meta=new File(modelFolder,image.getName()+".json");
    if(!Files.exists(meta.toPath(),LinkOption.NOFOLLOW_LINKS))continue;
    regular(meta);
    if(meta.length()>8192||!BackupFormat.selectedModelMetadata(new String(Files.readAllBytes(meta.toPath()),StandardCharsets.UTF_8)))continue;
    Group group=new Group();
    add(group,"model-photos/"+image.getName(),image);
    add(group,"model-photos/"+meta.getName(),meta);
    grouped.add(group);
   }
  }
  grouped.sort((a,b)->a.entries.iterator().next().compareTo(b.entries.iterator().next()));
  List<Volume> result=new ArrayList<>();Volume current=new Volume();result.add(current);
  for(Group group:grouped){
   if(group.bytes>limit-overhead)throw new IOException("한 묶음이 단일 ZIP 한도를 넘습니다.");
   if(current.size+group.bytes>limit-overhead&&!current.entries.isEmpty()){
    current=new Volume();result.add(current);
   }
   current.entries.addAll(group.entries);
   current.size+=group.bytes;
  }
  if(result.size()>1000)throw new IOException("분할 ZIP이 1000개를 넘어 백업할 수 없습니다.");
  return result;
 }
 private static String hex(byte[] bytes){
  StringBuilder out=new StringBuilder();
  for(byte b:bytes)out.append(String.format(Locale.ROOT,"%02x",b&255));
  return out.toString();
 }
 static int export(Context context,Uri folder,File internal,String json,String preferences,String part)throws Exception{
  List<Volume> volumes=plan(internal,part,json.getBytes(StandardCharsets.UTF_8).length,
   preferences.getBytes(StandardCharsets.UTF_8).length);
  ContentResolver resolver=context.getContentResolver();
  Uri parent=DocumentsContract.buildDocumentUriUsingTree(folder,DocumentsContract.getTreeDocumentId(folder));
  StringBuilder index=new StringBuilder("MOTO-HUB-VOLUMES-v1\t").append(part).append('\t').append(volumes.size()).append('\n');
  int completed=0;
  for(int i=0;i<volumes.size();i++){
   Volume volume=volumes.get(i);
   String filename=String.format(Locale.ROOT,"MOTO-HUB-%s-%03d-of-%03d.zip",part,i+1,volumes.size());
   Uri document=DocumentsContract.createDocument(resolver,parent,"application/zip",filename);
   if(document==null)throw new IOException("ZIP 파일 생성 실패: "+filename);
   try{
    try(OutputStream out=resolver.openOutputStream(document,"wt")){
     if(out==null)throw new IOException("ZIP 파일을 쓸 수 없습니다.");
     BackupBundle.export(json,internal,out,part,preferences,volume.entries);
    }
    MessageDigest digest=MessageDigest.getInstance("SHA-256");
    BackupIntegrity.Result readback;
    try(InputStream in=resolver.openInputStream(document)){
     if(in==null)throw new IOException("생성한 ZIP을 다시 읽지 못했습니다.");
     readback=BackupIntegrity.verify(new DigestInputStream(in,digest));
    }
    Set<String> expected=new HashSet<>(volume.entries);
    expected.add("MOTO-HUB-data.json");expected.add("MOTO-HUB-preferences.json");expected.add("README-KO.txt");
    if(!readback.entryNames.equals(expected))
     throw new IOException("ZIP 내용 검증에서 파일 누락 또는 추가 항목을 발견했습니다.");
    index.append(filename).append('\t').append(hex(digest.digest())).append('\t')
      .append(volume.entries.size()).append('\n');
    completed++;
   }catch(Exception failure){
    try{DocumentsContract.deleteDocument(resolver,document);}catch(Exception cleanup){failure.addSuppressed(cleanup);}
    throw new IOException(completed+"개 ZIP은 검증됐으나 백업 전체가 미완료입니다. INDEX가 없으면 완료된 백업으로 취급하지 마세요.",failure);
   }
  }
  Uri manifest=DocumentsContract.createDocument(resolver,parent,"text/plain",
    "MOTO-HUB-"+part+"-VOLUMES-INDEX.txt");
  if(manifest==null)throw new IOException("모든 ZIP 검증은 성공했지만 전체 목록 파일을 생성할 수 없습니다.");
  try{
   try(OutputStream out=resolver.openOutputStream(manifest,"wt")){
    if(out==null)throw new IOException("INDEX 파일을 쓸 수 없습니다.");
    out.write(index.toString().getBytes(StandardCharsets.UTF_8));
   }
   try(InputStream in=resolver.openInputStream(manifest)){
    if(in==null)throw new IOException("INDEX 재검증 불가");
    byte[] actual=new byte[index.length()*4+200];
    int count=0,n;while((n=in.read(actual,count,actual.length-count))>0){count+=n;if(count==actual.length)throw new IOException("INDEX 길이 초과");}
    if(!index.toString().equals(new String(actual,0,count,StandardCharsets.UTF_8)))
     throw new IOException("INDEX 저장 내용이 다릅니다.");
   }
  }catch(Exception failure){
   try{DocumentsContract.deleteDocument(resolver,manifest);}catch(Exception ignored){}
   throw new IOException("ZIP은 만들어졌지만 INDEX 검증이 실패해 전체 백업 완료로 표시하지 않습니다.",failure);
  }
  return volumes.size();
 }
}
