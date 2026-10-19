package com.motohub.next;

import java.io.*;
import java.util.*;
import java.util.zip.*;

/** Read-after-write integrity of a user-selected backup, without extracting data. */
final class BackupIntegrity {
 private BackupIntegrity(){}
 static final class Result {
  final int tracks,photos;
  Result(int tracks,int photos){this.tracks=tracks;this.photos=photos;}
 }
 static Result verify(InputStream source)throws IOException {
  if(source==null)throw new IOException("백업 파일을 열 수 없어요.");
  Set<String> names=new HashSet<>();byte[] buffer=new byte[32768];
  int entries=0,tracks=0,photos=0;long total=0;boolean hasData=false;
  try(ZipInputStream zip=new ZipInputStream(new BufferedInputStream(source))){
   ZipEntry entry;
   while((entry=zip.getNextEntry())!=null){
    if(++entries>6000)throw new IOException("백업 파일 항목 수 초과");
    String name=entry.getName();
    if(name==null||!names.add(name)||name.startsWith("/")||name.contains("..")||name.contains("\\")||name.indexOf('\0')>=0)
     throw new IOException("백업 ZIP 항목이 유효하지 않습니다.");
    if(name.equals("MOTO-HUB-data.json"))hasData=true;
    if(name.startsWith("rides/")&&name.endsWith(".track"))tracks++;
    if(name.startsWith("photos/")&&name.endsWith(".jpg"))photos++;
    int n;long size=0;
    while((n=zip.read(buffer))!=-1){size+=n;total+=n;
     if(size>50_000_000L||total>250_000_000L)throw new IOException("백업 크기 제한을 초과했습니다.");
    }
    if(name.equals("MOTO-HUB-data.json")&&size==0)throw new IOException("백업 기록이 비어 있습니다.");
    zip.closeEntry(); // ZipInputStream checks per-entry CRC here.
   }
  }
  if(!hasData)throw new IOException("백업에서 데이터 파일을 찾지 못했어요.");
  return new Result(tracks,photos);
 }
}
