package com.motohub.next;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Offline-only cache housekeeping. Never traverses symlinks, touches ride files or network. */
final class MapCachePolicy {
 private MapCachePolicy(){}
 static final int MAX_ENTRIES=12000;
 static final long WARN_BYTES=150L*1024*1024;
 static File target(File appCache){return new File(new File(appCache,"map"),"tiles");}
 static final class Result{
  final int files;final long bytes;
  Result(int f,long b){files=f;bytes=b;}
  String text(){return files+"개 파일 · "+(bytes/(1024*1024))+"MB"+(bytes>WARN_BYTES?" · 150MB 권장량 초과":"");}
 }
 static Result scan(File folder,boolean delete)throws IOException {
  if(folder==null||!folder.exists())return new Result(0,0);
  if(Files.isSymbolicLink(folder.toPath())||!folder.isDirectory())throw new IOException("안전하지 않은 지도 캐시 폴더");
  Deque<File> queue=new ArrayDeque<>();queue.add(folder);
  ArrayList<File> dirs=new ArrayList<>();int files=0,checked=0;long bytes=0;
  while(!queue.isEmpty()){
   File dir=queue.removeFirst();
   if(++checked>MAX_ENTRIES)throw new IOException("지도 캐시 항목이 너무 많습니다");
   if(Files.isSymbolicLink(dir.toPath())||!dir.isDirectory())throw new IOException("안전하지 않은 캐시 경로");
   dirs.add(dir);
   File[] entries=dir.listFiles();if(entries==null)throw new IOException("지도 캐시 목록 오류");
   for(File file:entries){
    if(Files.isSymbolicLink(file.toPath()))throw new IOException("지도 캐시에 링크가 있습니다");
    if(file.isDirectory())queue.addLast(file);
    else if(file.isFile()){
     if(++checked>MAX_ENTRIES)throw new IOException("지도 캐시 항목이 너무 많습니다");
     files++;long size=file.length();
     bytes=size>Long.MAX_VALUE-bytes?Long.MAX_VALUE:bytes+size;
     if(delete&&!file.delete())throw new IOException("지도 캐시 삭제 실패");
    }else throw new IOException("안전하지 않은 캐시 파일");
   }
  }
  if(delete)for(int i=dirs.size()-1;i>0;i--)dirs.get(i).delete();
  return new Result(files,bytes);
 }
}
