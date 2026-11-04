package com.motohub.next;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

/** Private rollback state for multi-section edits; never changes GPS or the DB format.
 * Every old sidecar (including absence) is synced before the first live write.
 * Startup replays pending edits idempotently; committed edits only need cleanup.
 */
final class RideMetadataJournal {
 private static final String DIRECTORY="ride-metadata-journal";
 private static final byte[] COMMITTED="MOTO-RIDE-EDIT-1".getBytes(StandardCharsets.US_ASCII);
 private static final int MAX_ENTRIES=10_000,MAX_SNAPSHOT=2_000_000;
 private final File folder;
 private RideMetadataJournal(File folder){this.folder=folder;}
 private static boolean exists(File f){return Files.exists(f.toPath(),LinkOption.NOFOLLOW_LINKS);}
 private static void regularOrAbsent(File f)throws IOException{
  if(exists(f)&&!Files.isRegularFile(f.toPath(),LinkOption.NOFOLLOW_LINKS))
   throw new IOException("라이딩 변경 복구 경로가 안전하지 않습니다.");
 }
 private static File folder(File files)throws IOException{
  File dir=new File(files,DIRECTORY);
  if(exists(dir)&&!Files.isDirectory(dir.toPath(),LinkOption.NOFOLLOW_LINKS))
   throw new IOException("라이딩 변경 복구 폴더가 안전하지 않습니다.");
  return dir;
 }
 private static boolean validName(String name){
  return name.matches("[0-9]{10,15}-[a-f0-9-]{36}\\.track\\.(bike|category)");
 }
 private static File target(File rides,String name)throws IOException{
  if(!validName(name))throw new IOException("라이딩 변경 복구 파일명이 올바르지 않습니다.");
  File file=new File(rides,name);
  regularOrAbsent(file);regularOrAbsent(new File(file+".writing"));
  String trackName=name.substring(0,name.lastIndexOf('.'));
  if(!Files.isRegularFile(new File(rides,trackName).toPath(),LinkOption.NOFOLLOW_LINKS))
   throw new IOException("변경 전 연결을 복구할 GPS 원본이 없습니다.");
  return file;
 }
 private static byte[] hash(byte[] data)throws IOException{
  try{return MessageDigest.getInstance("SHA-256").digest(data);}
  catch(java.security.NoSuchAlgorithmException e){throw new IOException("라이딩 변경 복구 검증 불가",e);}
 }
 private static byte[] bounded(File file,int max)throws IOException{
  regularOrAbsent(file);
  try(InputStream in=new FileInputStream(file);ByteArrayOutputStream out=new ByteArrayOutputStream()){
   byte[] buffer=new byte[4096];int count;
   while((count=in.read(buffer))!=-1){
    if(out.size()>max-count)throw new IOException("라이딩 변경 복구 파일 크기 초과");
    out.write(buffer,0,count);
   }
   return out.toByteArray();
  }
 }
 private static void atomic(File file,byte[] bytes)throws IOException{
  File temp=new File(file+".writing");regularOrAbsent(file);regularOrAbsent(temp);
  try{
   try(FileOutputStream out=new FileOutputStream(temp)){out.write(bytes);out.getFD().sync();}
   Files.move(temp.toPath(),file.toPath(),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
  }finally{
   try{Files.deleteIfExists(temp.toPath());}catch(IOException ignored){/* Keep pending journal for startup replay. */}
  }
 }
 static RideMetadataJournal begin(File files,Map<File,byte[]> originals,String suffix)throws IOException{
  if(!".bike".equals(suffix)&&!".category".equals(suffix)||originals.isEmpty()||originals.size()>MAX_ENTRIES)
   throw new IOException("라이딩 변경 복구 요청이 올바르지 않습니다.");
  File rides=TrackFile.directory(files).getCanonicalFile();
  ByteArrayOutputStream payload=new ByteArrayOutputStream();
  try(DataOutputStream out=new DataOutputStream(payload)){
   out.writeInt(1);out.writeInt(originals.size());
   for(Map.Entry<File,byte[]> item:originals.entrySet()){
    File track=item.getKey();String name=track.getName()+suffix;
    if(!rides.equals(track.getCanonicalFile().getParentFile()))throw new IOException("로컬 GPS 구간만 변경할 수 있습니다.");
    target(rides,name);
    byte[] before=item.getValue();
    if(before!=null&&before.length>64)throw new IOException("라이딩 변경 전 정보 크기 초과");
    out.writeUTF(name);out.writeInt(before==null?-1:before.length);if(before!=null)out.write(before);
   }
  }
  byte[] body=payload.toByteArray();
  if(body.length+32>MAX_SNAPSHOT)throw new IOException("라이딩 변경 복구 용량 초과");
  payload.write(hash(body));
  File dir=folder(files);
  // A second edit cannot overwrite another live or unresolved transaction.
  try{Files.createDirectory(dir.toPath());}
  catch(FileAlreadyExistsException pending){throw new IOException("이전 라이딩 변경을 복구한 뒤 다시 시도해 주세요.",pending);}
  RideMetadataJournal journal=new RideMetadataJournal(dir);
  atomic(new File(dir,"snapshot"),payload.toByteArray());
  return journal;
 }
 /** Resolve either a successful edit or an already completed ordinary rollback. */
 void finish()throws IOException{
  folder(folder.getParentFile());contents(folder);
  atomic(new File(folder,"committed"),COMMITTED);
  try{clean(folder);}catch(IOException ignored){/* A valid committed flag forbids rollback on restart. */}
 }
 private static Map<String,byte[]> readSnapshot(File dir)throws IOException{
  byte[] packed=bounded(new File(dir,"snapshot"),MAX_SNAPSHOT);
  if(packed.length<40)throw new IOException("라이딩 변경 복구 정보가 잘렸습니다.");
  byte[] body=Arrays.copyOf(packed,packed.length-32),digest=Arrays.copyOfRange(packed,packed.length-32,packed.length);
  if(!MessageDigest.isEqual(hash(body),digest))throw new IOException("라이딩 변경 복구 검증값이 다릅니다.");
  Map<String,byte[]> entries=new LinkedHashMap<>();
  try(DataInputStream in=new DataInputStream(new ByteArrayInputStream(body))){
   if(in.readInt()!=1)throw new IOException("지원하지 않는 라이딩 변경 복구 형식입니다.");
   int count=in.readInt();if(count<1||count>MAX_ENTRIES)throw new IOException("라이딩 변경 복구 개수 오류");
   for(int i=0;i<count;i++){
    String name=in.readUTF();int size=in.readInt();
    if(!validName(name)||entries.containsKey(name)||size< -1||size>64)
     throw new IOException("라이딩 변경 복구 항목 오류");
    byte[] value=size<0?null:new byte[size];if(value!=null)in.readFully(value);entries.put(name,value);
   }
   if(in.available()!=0)throw new IOException("라이딩 변경 복구 정보가 중복됐습니다.");
  }
  return entries;
 }
 private static File[] contents(File dir)throws IOException{
  File[] files=dir.listFiles();if(files==null)throw new IOException("라이딩 변경 복구 폴더를 읽지 못했습니다.");
  Set<String> allowed=new HashSet<>(Arrays.asList("snapshot","snapshot.writing","committed","committed.writing"));
  for(File f:files){if(!allowed.contains(f.getName()))throw new IOException("알 수 없는 라이딩 변경 복구 항목입니다.");regularOrAbsent(f);}
  return files;
 }
 private static void clean(File dir)throws IOException{
  contents(dir);
  // Retain the committed flag until its snapshot is gone, even during cleanup.
  for(String name:new String[]{"snapshot.writing","committed.writing","snapshot","committed"})
   Files.deleteIfExists(new File(dir,name).toPath());
  Files.delete(dir.toPath());
 }
 static int recoverPending(File files)throws IOException{
  File dir=folder(files);if(!exists(dir))return 0;
  contents(dir); // Validate all journal paths before following any of them.
  File committed=new File(dir,"committed"),snapshot=new File(dir,"snapshot");
  if(exists(committed)){
   if(!Arrays.equals(COMMITTED,bounded(committed,COMMITTED.length)))throw new IOException("라이딩 변경 완료 표시가 손상됐습니다.");
   clean(dir);return 0;
  }
  if(!exists(snapshot)){clean(dir);return 0;} // Preparation never reached the first live write.
  Map<String,byte[]> before=readSnapshot(dir);
  File rides=TrackFile.directory(files).getCanonicalFile();
  // An unsafe later target must not cause an earlier target to be restored first.
  for(String name:before.keySet())target(rides,name);
  for(Map.Entry<String,byte[]> item:before.entrySet()){
   File file=target(rides,item.getKey());
   if(item.getValue()==null)Files.deleteIfExists(file.toPath());else atomic(file,item.getValue());
  }
  new RideMetadataJournal(dir).finish();return 1;
 }
}
