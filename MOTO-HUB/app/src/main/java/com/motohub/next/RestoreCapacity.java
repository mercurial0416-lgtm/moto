package com.motohub.next;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.nio.file.Files;
import java.nio.file.LinkOption;

/** Conservatively reserve storage for both rollback copies and restored assets. */
final class RestoreCapacity {
 private RestoreCapacity() {}
 static final long RESERVE_BYTES=8L*1024*1024;
 static long required(long oldBytes,long freshBytes,long jsonBytes) throws IOException {
  if(oldBytes<0||freshBytes<0||jsonBytes<0)throw new IOException("복원 파일 크기가 올바르지 않습니다.");
  try {
   return Math.addExact(RESERVE_BYTES,Math.addExact(jsonBytes,Math.addExact(oldBytes,freshBytes)));
  }catch(ArithmeticException e){throw new IOException("복원 용량 계산이 범위를 초과했습니다.",e);}
 }
 static long sum(File root,List<String> paths) throws IOException {
  long bytes=0;
  for(String path:paths){
   File file=new File(root,path);
   if(Files.isSymbolicLink(file.toPath())||!Files.isRegularFile(file.toPath(),LinkOption.NOFOLLOW_LINKS))throw new IOException("복원 용량 점검 중 안전하지 않은 파일입니다: "+path);
   try {bytes=Math.addExact(bytes,file.length());}
   catch(ArithmeticException e){throw new IOException("복원 데이터 크기가 너무 큽니다.",e);}
  }
  return bytes;
 }
 static long available(File internal) throws IOException {
  if(internal==null||!Files.isDirectory(internal.toPath(),LinkOption.NOFOLLOW_LINKS))throw new IOException("앱 저장 폴더를 확인할 수 없습니다.");
  return internal.getUsableSpace();
 }
 static void ensure(long available,long needed)throws IOException {
  if(available<=0)throw new IOException("복원 전 남은 저장공간을 확인하지 못했습니다. 여유 공간을 확보한 뒤 다시 시도해 주세요.");
  if(available<needed)throw new IOException("복원에 필요한 여유 공간이 부족합니다. 최소 "+mb(needed)+"MB, 현재 약 "+mb(available)+"MB입니다. 기존 기록 보호를 위해 복원을 시작하지 않았습니다.");
 }
 static long mb(long bytes){return bytes<=0?0:1+(bytes-1)/1_000_000L;}
}
