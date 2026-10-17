package com.motohub.next;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;

/** Privacy-sensitive offline ZIP export. Includes database, routes, and garage photos, NOT credentials. */
public final class BackupBundle {
 private BackupBundle() {}
 static final long MAX_TOTAL=250_000_000L;
 private static long writeFile(ZipOutputStream zip,File file,String name,long total)throws IOException{
  if(!file.isFile())throw new IOException("백업 파일을 찾지 못했어요: "+file.getName());
  if(file.length()>TrackFile.MAX_BYTES)throw new IOException("백업 항목이 25MB를 넘었어요: "+file.getName());
  if(total+file.length()>MAX_TOTAL)throw new IOException("백업 최대 크기(250MB)를 넘었어요. GPX를 개별로 내보내 주세요.");
  zip.putNextEntry(new ZipEntry(name));byte[] buffer=new byte[32768];long done=0;
  try(InputStream in=new BufferedInputStream(new FileInputStream(file))){int n;while((n=in.read(buffer))!=-1){done+=n;if(done>TrackFile.MAX_BYTES||total+done>MAX_TOTAL)throw new IOException("백업 항목 크기가 너무 큽니다.");zip.write(buffer,0,n);}}
  zip.closeEntry();return total+done;
 }
 public static int export(String json,File internal,OutputStream target)throws Exception{
  byte[] data=json.getBytes(StandardCharsets.UTF_8);if(data.length>8_000_000)throw new IOException("JSON 데이터가 8MB를 넘었어요.");
  long total=data.length;int count=0;
  ZipOutputStream zip=new ZipOutputStream(new BufferedOutputStream(target),StandardCharsets.UTF_8);
  zip.putNextEntry(new ZipEntry("MOTO-HUB-data.json"));zip.write(data);zip.closeEntry();count++;
  String readme="MOTO HUB 전체 백업 - 개인 위치정보 포함 가능\n"+
   "MOTO-HUB-data.json은 앱 설정 > JSON 백업 가져오기로 복원합니다.\n"+
   "rides 폴더는 원본 기록, gpx 폴더는 가져오기로 복원 가능한 GPX 경로입니다.\n"+
   "photos에는 등록한 바이크 사진이 들어 있습니다.\n"+
   "현재 버전에서는 설정 > 백업·복원 > 전체 ZIP 백업 복원으로 사진과 GPS도 한 번에 불러올 수 있습니다.\n"+
   "친구 로그인 토큰/인증키는 포함되지 않습니다. 위치가 포함될 수 있으니 공유하지 마세요.\n";
  zip.putNextEntry(new ZipEntry("README-KO.txt"));zip.write(readme.getBytes(StandardCharsets.UTF_8));zip.closeEntry();
  File photos=internal;File[] files=photos.listFiles((dir,name)->name.matches("garage-photo-[a-fA-F0-9-]{36}\\.jpg"));
  if(files!=null)for(File f:files){total=writeFile(zip,f,"photos/"+f.getName(),total);count++;}
  File rides=new File(internal,"rides");File[] tracks=rides.listFiles((dir,name)->name.matches("[0-9]{10,15}-[a-f0-9-]{36}\\.track"));
  if(tracks!=null){if(tracks.length>2500)throw new IOException("주행 기록이 너무 많습니다.");
   for(File file:tracks){total=writeFile(zip,file,"rides/"+file.getName(),total);count++;
    if(new File(file.getPath()+".done").isFile()||new File(file.getPath()+".recovered").isFile())try{
     if(TrackFile.read(file,(x,y,z,t)->{})>=2){
      // Add an interoperable GPX file for recovery on another device.
      if(total+file.length()*2<MAX_TOTAL){
       zip.putNextEntry(new ZipEntry("gpx/"+file.getName().replace(".track",".gpx")));
       TrackFile.gpx(file,zip);zip.closeEntry();total+=file.length()*2;
      }
     }
    }catch(Exception ignored){ /* Keep the original track even if its GPX conversion is corrupt. */ }
    
    for(String suffix:new String[]{".done",".imported",".recovered"}){
     File marker=new File(file.getPath()+suffix);if(marker.isFile()){
      zip.putNextEntry(new ZipEntry("rides/"+marker.getName()));zip.closeEntry();
     }
    }
   }
  }
  zip.finish();zip.flush();return count;
 }
}
