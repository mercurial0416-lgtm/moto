package com.motohub.next;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.util.*;
import java.util.zip.*;

/** Plans disjoint archives before touching the selected SAF folder. The same route and its markers stay together. */
final class BatchArchiveExport {
 static final long TARGET_BYTES=175_000_000L;
 static final class Asset {
  final File file;final String name;
  Asset(File f,String n){file=f;name=n;}
 }
 static final class Part {
  final String kind;final List<Asset> assets=new ArrayList<>();
  int index,total;
  long bytes;
  Part(String kind){this.kind=kind;}
  Set<String> expectedEntries(){
   Set<String> names=new HashSet<>(Arrays.asList("MOTO-HUB-data.json","README-KO.txt","MOTO-HUB-batch.txt"));
   for(Asset a:assets)names.add(a.name);
   return names;
  }
 }
 static final class Plan {
  final List<Part> parts;final int assets;
  Plan(List<Part> parts,int assets){this.parts=parts;this.assets=assets;}
 }
 private BatchArchiveExport(){}
 private static void regular(File f)throws IOException{
  if(f==null||Files.isSymbolicLink(f.toPath())||!Files.isRegularFile(f.toPath(),LinkOption.NOFOLLOW_LINKS))
   throw new IOException("백업 대상 파일이 안전하지 않습니다.");
 }
 private static void safeDir(File d)throws IOException{
  if(Files.isSymbolicLink(d.toPath())||(d.exists()&&!Files.isDirectory(d.toPath(),LinkOption.NOFOLLOW_LINKS)))
   throw new IOException("백업 원본 폴더가 안전하지 않습니다.");
 }
 private static void add(List<Asset> into,File f,String zipName)throws IOException{
  regular(f);
  if(!BackupFormat.allowed(zipName,false)||f.length()>BackupFormat.entryLimit(zipName))
   throw new IOException("백업 항목 크기 또는 이름이 유효하지 않습니다: "+zipName);
  into.add(new Asset(f,zipName));
 }
 private static List<Asset> ride(File f,String prefix)throws IOException{
  List<Asset> items=new ArrayList<>();
  add(items,f,prefix+f.getName());
  for(String suffix:RideTrash.SIDECARS){
   File marker=new File(f.getPath()+suffix);
   if(Files.exists(marker.toPath(),LinkOption.NOFOLLOW_LINKS))
    add(items,marker,prefix+marker.getName());
  }
  return items;
 }
 private static String find(Map<String,String> parent,String id){
  String now=id;while(!parent.get(now).equals(now))now=parent.get(now);
  return now;
 }
 static Plan plan(File root)throws IOException{
  safeDir(root);
  File rides=new File(root,"rides");safeDir(rides);
  File[] candidates=rides.listFiles((d,n)->n.matches(BackupFormat.TRACK));
  List<File> tracks=new ArrayList<>();
  if(candidates!=null)tracks.addAll(Arrays.asList(candidates));
  tracks.sort(Comparator.comparing(File::getName));
  Map<String,String> parents=new HashMap<>();
  for(File f:tracks){regular(f);parents.put(f.getName(),f.getName());}
  for(File f:tracks){
   File link=new File(f.getPath()+".mergedinto");
   if(!Files.exists(link.toPath(),LinkOption.NOFOLLOW_LINKS))continue;
   regular(link);
   if(link.length()>100)throw new IOException("GPS 통합 대상 정보 크기 초과");
   String destination=new String(Files.readAllBytes(link.toPath()),StandardCharsets.US_ASCII);
   if(!parents.containsKey(destination))throw new IOException("통합 GPS 대상 누락: "+destination);
   String one=find(parents,f.getName()),two=find(parents,destination);
   if(!one.equals(two))parents.put(one,two);
  }
  Map<String,List<Asset>> gpsGroups=new TreeMap<>();
  for(File f:tracks)gpsGroups.computeIfAbsent(find(parents,f.getName()),k->new ArrayList<>())
   .addAll(ride(f,"rides/"));
  List<File> trashed=RideTrash.entries(root);
  for(File f:trashed)gpsGroups.put("trash-"+f.getName(),ride(f,"rides-trash/"+f.getName()+"/"));
  List<List<Asset>> gps=new ArrayList<>(gpsGroups.values()),media=new ArrayList<>();
  File[] photos=root.listFiles((d,n)->n.matches(BackupFormat.PHOTO));
  if(photos!=null)for(File f:photos){
   List<Asset> group=new ArrayList<>();add(group,f,"photos/"+f.getName());media.add(group);
  }
  File[] receipts=root.listFiles((d,n)->n.matches(BackupFormat.RECEIPT));
  if(receipts!=null)for(File f:receipts){
   List<Asset> group=new ArrayList<>();add(group,f,"receipts/"+f.getName());media.add(group);
  }
  File models=new File(root,"moto-year-photos");safeDir(models);
  File[] modelImages=models.listFiles((d,n)->n.matches(BackupFormat.MODEL_PHOTO));
  if(modelImages!=null)for(File image:modelImages){
   File metadata=new File(models,image.getName()+".json");
   if(!Files.exists(metadata.toPath(),LinkOption.NOFOLLOW_LINKS))continue;
   regular(metadata);
   if(metadata.length()>8192)throw new IOException("모델 사진 출처 파일 크기 초과");
   if(!BackupFormat.selectedModelMetadata(new String(Files.readAllBytes(metadata.toPath()),StandardCharsets.UTF_8)))continue;
   List<Asset> group=new ArrayList<>();
   add(group,image,"model-photos/"+image.getName());
   add(group,metadata,"model-photos/"+metadata.getName());
   media.add(group);
  }
  Comparator<List<Asset>> groupOrder=Comparator.comparing(x->x.get(0).name);
  gps.sort(groupOrder);media.sort(groupOrder);
  List<Part> parts=new ArrayList<>();
  int count=0;
  count+=pack(parts,"GPS",gps);
  count+=pack(parts,"MEDIA",media);
  Set<String> unique=new HashSet<>();
  for(Part part:parts)for(Asset a:part.assets)if(!unique.add(a.name))
   throw new IOException("중복 백업 항목: "+a.name);
  if(unique.size()!=count)throw new IOException("백업 분할 항목 누락");
  int index=0;
  for(Part part:parts){part.index=++index;part.total=parts.size();}
  return new Plan(Collections.unmodifiableList(parts),count);
 }
 private static int pack(List<Part> all,String kind,List<List<Asset>> groups)throws IOException{
  Part current=null;int count=0;
  for(List<Asset> group:groups){
   long bytes=0;
   for(Asset asset:group){bytes=Math.addExact(bytes,asset.file.length());}
   if(bytes>TARGET_BYTES)throw new IOException("한 그룹의 백업 자료가 분할 한도를 넘습니다: "+group.get(0).name);
   if(current==null||current.bytes+bytes>TARGET_BYTES){
    current=new Part(kind);all.add(current);
   }
   current.assets.addAll(group);current.bytes+=bytes;count+=group.size();
  }
  return count;
 }
 static String batchRecord(String id,Part part){
  return "MOTO-HUB-BATCH:v1:"+id+":"+part.kind+":"+part.index+"/"+part.total+"\n";
 }
 static void write(Part part,String json,String id,OutputStream target)throws IOException{
  byte[] body=json.getBytes(StandardCharsets.UTF_8);
  if(body.length>BackupFormat.MAX_JSON_BYTES)throw new IOException("데이터 JSON 크기 초과");
  try(ZipOutputStream zip=new ZipOutputStream(new BufferedOutputStream(target),StandardCharsets.UTF_8)){
   put(zip,"MOTO-HUB-data.json",body);
   put(zip,"README-KO.txt",("MOTO-HUB-PART:"+part.kind+
    "\n자동 분할 백업 "+part.index+"/"+part.total+
    " · 다른 분할 파일도 모두 보관해야 전체 자료를 복구할 수 있습니다.\n").getBytes(StandardCharsets.UTF_8));
   put(zip,"MOTO-HUB-batch.txt",batchRecord(id,part).getBytes(StandardCharsets.US_ASCII));
   byte[] buffer=new byte[32768];
   long total=body.length;
   for(Asset item:part.assets){
    regular(item.file);
    if(item.file.length()>BackupFormat.entryLimit(item.name))
     throw new IOException("백업 항목이 변경되어 용량 검증 실패");
    total+=item.file.length();
    if(total>BackupFormat.MAX_TOTAL_BYTES)throw new IOException("ZIP 최대 용량 초과");
    zip.putNextEntry(new ZipEntry(item.name));
    try(InputStream in=new BufferedInputStream(new FileInputStream(item.file))){
     int n;long written=0;while((n=in.read(buffer))!=-1){
      written+=n;
      if(written>BackupFormat.entryLimit(item.name)||total-item.file.length()+written>BackupFormat.MAX_TOTAL_BYTES)
       throw new IOException("파일이 백업 중 변경됐습니다.");
      zip.write(buffer,0,n);
     }
    }
    zip.closeEntry();
   }
   zip.finish();
  }
 }
 private static void put(ZipOutputStream z,String name,byte[] data)throws IOException{
  if(data.length>BackupFormat.entryLimit(name))throw new IOException("분할 ZIP 메타정보 크기 제한");
  z.putNextEntry(new ZipEntry(name));z.write(data);z.closeEntry();
 }
 static String validateBatchRecord(String record)throws IOException{
  if(record==null||!record.matches("MOTO-HUB-BATCH:v1:[a-f0-9-]{36}:(GPS|MEDIA):[0-9]{1,5}/[0-9]{1,5}\\n"))
   throw new IOException("분할 ZIP 식별 정보가 유효하지 않습니다.");
  return record;
 }
}
