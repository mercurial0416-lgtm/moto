package com.motohub.next;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Strict readback of the chosen ZIP; use the same format limits as import. */
final class BackupIntegrity {
 private BackupIntegrity(){}
 static final class Result {
  final int tracks,photos;
  final Map<String,String> trackSha256;
  Result(int tracks,int photos,Map<String,String> hashes){
   this.tracks=tracks;this.photos=photos;
   this.trackSha256=Collections.unmodifiableMap(new HashMap<>(hashes));
  }
 }
 static Result verify(InputStream source)throws IOException {
  if(source==null)throw new IOException("백업 파일을 열 수 없어요.");
  Set<String> names=new HashSet<>();Map<String,String> small=new HashMap<>();byte[] buffer=new byte[32768];
  int entries=0,tracks=0,photos=0,receipts=0;long total=0;boolean hasData=false;
  Map<String,String> trackHashes=new HashMap<>();
  try(ZipInputStream zip=new ZipInputStream(new BufferedInputStream(source),StandardCharsets.UTF_8)){
   ZipEntry entry;
   while((entry=zip.getNextEntry())!=null){
    if(++entries>BackupFormat.MAX_ENTRIES)throw new IOException("백업 파일 항목 수 초과");
    String name=entry.getName();
    if(!BackupFormat.allowed(name,entry.isDirectory())||!names.add(name))
     throw new IOException("백업 ZIP 항목이 유효하지 않습니다.");
    if((BackupFormat.isTrack(name)||BackupFormat.isTrashTrack(name))&&++tracks>BackupFormat.MAX_TRACKSthrow new IOException("GPS 기록이 너무 많습니다.");
    if((BackupFormat.isPhoto(name)||BackupFormat.isModelPhoto(name))&&++photos>BackupFormat.MAX_PHOTOS*2)throw new IOException("사진이 너무 많습니다.");
    if(BackupFormat.isReceipt(name)&&++receipts>BackupFormat.MAX_RECEIPTS)throw new IOException("영수증 사진이 너무 많습니다.");
    if("MOTO-HUB-data.json".equals(name))hasData=true;
    long size=0,limit=BackupFormat.entryLimit(name);
    MessageDigest digest=null;
    if(BackupFormat.isTrack(name)){
     try{digest=MessageDigest.getInstance("SHA-256");}
     catch(NoSuchAlgorithmException unavailable){throw new IOException("GPS SHA-256 검증을 실행할 수 없습니다.",unavailable);}
    }
    ByteArrayOutputStream value=((BackupFormat.isSidecar(name)||BackupFormat.isTrashSidecar(name))&&limit>0)||BackupFormat.isModelMetadata(name)?new ByteArrayOutputStream():null;
    int n;byte[] header=new byte[2],trailer=new byte[2];
    while((n=zip.read(buffer))!=-1){
     if(BackupFormat.isReceipt(name)||BackupFormat.isPhoto(name)||BackupFormat.isModelPhoto(name)){
      for(int ix=0;ix<n;ix++){long pos=size+ix;if(pos<2)header[(int)pos]=buffer[ix];trailer[0]=trailer[1];trailer[1]=buffer[ix];}
     }
     size+=n;total+=n;
     if(size>limit||total>BackupFormat.MAX_TOTAL_BYTES)throw new IOException("백업 크기 제한을 초과했습니다.");
     if(value!=null)value.write(buffer,0,n);
     if(digest!=null)digest.update(buffer,0,n);
    }
    if((BackupFormat.isReceipt(name)||BackupFormat.isPhoto(name)||BackupFormat.isModelPhoto(name))&&(size<4||(header[0]&255)!=255||(header[1]&255)!=216||(trailer[0]&255)!=255||(trailer[1]&255)!=217))throw new IOException("손상된 사진 파일입니다.");
    if(digest!=null){
     StringBuilder hex=new StringBuilder();
     for(byte part:digest.digest())hex.append(String.format(Locale.ROOT,"%02x",part&255));
     trackHashes.put(name.substring("rides/".length()),hex.toString());
    }
    if(name.equals("MOTO-HUB-data.json")&&size==0)throw new IOException("백업 기록이 비어 있습니다.");
    if(value!=null)small.put(name,value.toString("UTF-8"));
    zip.closeEntry(); // Checks the entry CRC before a success message is shown.
   }
  }
  if(!hasData)throw new IOException("백업에서 데이터 파일을 찾지 못했어요.");
  for(String name:names){
   if(BackupFormat.isModelPhoto(name)&&!names.contains(name+".json"))
    throw new IOException("선택한 모델 사진 출처 정보가 누락됐습니다.");
   if(BackupFormat.isModelMetadata(name)){
    if(!names.contains(name.substring(0,name.length()-5))||
       !BackupFormat.selectedModelMetadata(small.get(name)))
     throw new IOException("선택한 모델 사진 출처 정보가 손상됐습니다.");
   }
   if(!BackupFormat.isSidecar(name)&&!BackupFormat.isTrashSidecar(name))continue;
   String suffix=BackupFormat.sidecarSuffix(name);
   String origin=name.substring(0,name.length()-suffix.length());
   if(!names.contains(origin))throw new IOException("원본 GPS 없이 표시만 남은 기록: "+name);
   if(suffix.equals(".unmerged")&&!names.contains(origin+".done"))
    throw new IOException("완료되지 않은 통합 GPS 기록입니다.");
   if(suffix.equals(".mergedinto")){
    String target=small.get(name);
    if(target==null||!target.matches(BackupFormat.TRACK)||
       !names.contains("rides/"+target)||!names.contains("rides/"+target+".done"))
     throw new IOException("통합 기록 연결 정보가 손상됐습니다.");
   }
   if(suffix.equals(".bike")&&
      !small.getOrDefault(name,"").matches("[0-9a-fA-F]{8}(-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}"))
    throw new IOException("바이크 연결 정보가 손상됐습니다.");
   if(suffix.equals(".category")&&
      !Arrays.asList("자동 분류","일상","출퇴근","와인딩","장거리","투어").contains(small.get(name)))
    throw new IOException("라이딩 분류가 손상됐습니다.");
  }
  return new Result(tracks,photos,trackHashes);
 }
}
