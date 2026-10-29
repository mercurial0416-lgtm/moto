package com.motohub.next;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.util.*;
import java.util.zip.*;

/** Privacy-sensitive offline ZIP export. Includes database, routes, and garage photos, NOT credentials. */
public final class BackupBundle {
 private BackupBundle() {}
 static final long MAX_TOTAL=BackupFormat.MAX_TOTAL_BYTES;
 /** Follow no links: backup ZIP must never read an unrelated file through an app-data symlink. */
 private static boolean regular(File file){
  return file!=null&&Files.isRegularFile(file.toPath(),LinkOption.NOFOLLOW_LINKS)
   &&!Files.isSymbolicLink(file.toPath());
 }
 private static void requireRegular(File file)throws IOException{
  if(!regular(file))throw new IOException("백업 대상이 일반 파일이 아닙니다: "+(file==null?"unknown":file.getName()));
 }
 private static boolean regularDirectory(File folder){
  return Files.isDirectory(folder.toPath(),LinkOption.NOFOLLOW_LINKS)&&!Files.isSymbolicLink(folder.toPath());
 }
 private static void requireDirectoryIfPresent(File dir)throws IOException{
  if(Files.exists(dir.toPath(),LinkOption.NOFOLLOW_LINKS)&&
    (Files.isSymbolicLink(dir.toPath())||!Files.isDirectory(dir.toPath(),LinkOption.NOFOLLOW_LINKS)))
   throw new IOException("백업 폴더가 안전하지 않습니다: "+dir.getName());
 }
 private static final String[] TRASH_SIDECARS={".done",".imported",".recovered",".unmerged",".category",".bike",".favorite",".mergedinto"};
 /** Explicitly enumerate quarantined sessions without Android dependencies; incomplete folders block export. */
 private static List<File> trashTracks(File internal)throws IOException{
  File root=new File(internal,"rides-trash");
  requireDirectoryIfPresent(root);
  List<File> list=new ArrayList<>();
  File[] folders=root.listFiles();
  if(folders==null)return list;
  for(File folder:folders){
   if(!folder.getName().matches(BackupFormat.TRACK)||
      !Files.isDirectory(folder.toPath(),LinkOption.NOFOLLOW_LINKS)||
      Files.isSymbolicLink(folder.toPath()))
    throw new IOException("GPS 복구함 폴더를 확인할 수 없습니다.");
   File track=new File(folder,folder.getName());
   requireRegular(track);
   list.add(track);
  }
  list.sort(Comparator.comparing(File::getName));
  return list;
 }
 private static byte[] readSidecar(File file,long maxBytes)throws IOException{
  requireRegular(file);
  if(file.length()>maxBytes)throw new IOException("라이딩 부속 파일 크기가 너무 큽니다: "+file.getName());
  return Files.readAllBytes(file.toPath());
 }
 /** Check the same JPEG envelope as the import path BEFORE publishing ZIP bytes. */
 private static void requireJpeg(File file)throws IOException{
  requireRegular(file);
  if(file.length()<4)throw new IOException("백업 이미지가 손상됐습니다: "+file.getName());
  try(RandomAccessFile jpeg=new RandomAccessFile(file,"r")){
   if(jpeg.readUnsignedByte()!=0xff||jpeg.readUnsignedByte()!=0xd8)
    throw new IOException("백업 이미지가 손상됐습니다: "+file.getName());
   jpeg.seek(file.length()-2);
   if(jpeg.readUnsignedByte()!=0xff||jpeg.readUnsignedByte()!=0xd9)
    throw new IOException("백업 이미지가 손상됐습니다: "+file.getName());
  }
 }
 private static long writeFile(ZipOutputStream zip,File file,String name,long total)throws IOException{
  requireRegular(file);
  if(BackupFormat.isPhoto(name)||BackupFormat.isReceipt(name)||BackupFormat.isModelPhoto(name))
   requireJpeg(file);
  if(file.length()>TrackFile.MAX_BYTES)throw new IOException("백업 항목이 25MB를 넘었어요: "+file.getName());
  if(total+file.length()>MAX_TOTAL)throw new IOException("백업 최대 크기(250MB)를 넘었어요. GPX를 개별로 내보내 주세요.");
  zip.putNextEntry(new ZipEntry(name));byte[] buffer=new byte[32768];long done=0;
  try(InputStream in=new BufferedInputStream(new FileInputStream(file))){int n;while((n=in.read(buffer))!=-1){done+=n;if(done>TrackFile.MAX_BYTES||total+done>MAX_TOTAL)throw new IOException("백업 항목 크기가 너무 큽니다.");zip.write(buffer,0,n);}}
  zip.closeEntry();return total+done;
 }
 /** Count complete GPX bytes without holding a possibly huge route in memory. */
 private static final class BoundedCounter extends OutputStream {
  private long count;
  @Override public void write(int b)throws IOException {advance(1);}
  @Override public void write(byte[] b,int off,int len)throws IOException {
   if(off<0||len<0||off>b.length-len)throw new IndexOutOfBoundsException();
   advance(len);
  }
  private void advance(int amount)throws IOException {
   if(amount<0||count>50_000_000L-amount)throw new IOException("GPX 용량 제한(50MB)을 초과했습니다.");
   count+=amount;
  }
 }
 /** Only committed, parseable GPS data is eligible for an optional GPX export. */
 private static long gpxBytes(File track)throws Exception {
  BoundedCounter counter=new BoundedCounter();TrackFile.gpx(track,counter);return counter.count;
 }
 public static int export(String json,File internal,OutputStream target)throws Exception{
  return export(json,internal,target,"FULL");
 }
 /** Archive-only GPS or media partitions. Each has a full JSON snapshot for manual recovery. */
 static int export(String json,File internal,OutputStream target,String part)throws Exception{
  return export(json,internal,target,part,"");
 }
 static int export(String json,File internal,OutputStream target,String part,String uiPreferences)throws Exception{
  if(!Arrays.asList("FULL","GPS","MEDIA").contains(part))throw new IOException("알 수 없는 백업 분할");
  boolean gps=!"MEDIA".equals(part),media=!"GPS".equals(part);
  if(internal==null||!regularDirectory(internal))throw new IOException("앱 백업 폴더를 확인하지 못했어요.");
  requireDirectoryIfPresent(new File(internal,"rides"));
  byte[] data=json.getBytes(StandardCharsets.UTF_8);if(data.length>8_000_000)throw new IOException("JSON 데이터가 8MB를 넘었어요.");
  long total=data.length;int count=0;
  ZipOutputStream zip=new ZipOutputStream(new BufferedOutputStream(target),StandardCharsets.UTF_8);
  zip.putNextEntry(new ZipEntry("MOTO-HUB-data.json"));zip.write(data);zip.closeEntry();count++;
  if(uiPreferences!=null&&!uiPreferences.isEmpty()&&"FULL".equals(part)){
   byte[] ui=uiPreferences.getBytes(StandardCharsets.UTF_8);
   if(ui.length>4096)throw new IOException("설정 백업 크기 제한");
   zip.putNextEntry(new ZipEntry("MOTO-HUB-preferences.json"));zip.write(ui);zip.closeEntry();
   total+=ui.length;count++;
  }
  String readme=("FULL".equals(part)?"MOTO HUB 전체 백업 - 개인 위치정보 포함 가능\n":"MOTO-HUB-PART:"+part+"\n분할 복구 파일: 전체 복원으로 다른 자료를 덮어쓰지 마세요. 분할 ZIP 추가 복원 메뉴를 사용하세요.\n")+
   "MOTO-HUB-data.json은 앱 설정 > JSON 백업 가져오기로 복원합니다.\n"+
   "rides 폴더는 원본 기록, gpx 폴더는 가져오기로 복원 가능한 GPX 경로입니다.\n"+
   "photos에는 바이크 사진, receipts에는 정비·주유 영수증 사진이 있습니다.\n"+
   "model-photos에는 온라인 갤러리에서 직접 고른 모델 사진과 출처 정보가 포함됩니다.\n"+
   "현재 버전에서는 설정 > 백업·복원 > 전체 ZIP 백업 복원으로 사진과 GPS도 한 번에 불러올 수 있습니다.\n"+
   "친구 로그인 토큰/인증키는 포함되지 않습니다. 위치가 포함될 수 있으니 공유하지 마세요.\n";
  zip.putNextEntry(new ZipEntry("README-KO.txt"));zip.write(readme.getBytes(StandardCharsets.UTF_8));zip.closeEntry();
  File photos=internal;File[] files=photos.listFiles((dir,name)->name.matches("garage-photo-[a-fA-F0-9-]{36}\\.jpg"));
  if(media&&files!=null){
   if(files.length>BackupFormat.MAX_PHOTOS)throw new IOException("사진이 백업 한도(1,000장)를 초과했습니다.");
   Arrays.sort(files,Comparator.comparing(File::getName));
   for(File f:files){total=writeFile(zip,f,"photos/"+f.getName(),total);count++;}
  }
  File[] receipts=internal.listFiles((dir,name)->name.matches(BackupFormat.RECEIPT));
  if(media&&receipts!=null){
   if(receipts.length>BackupFormat.MAX_RECEIPTS)throw new IOException("영수증·작업 사진이 4,000장을 초과했습니다.");
   Arrays.sort(receipts,Comparator.comparing(File::getName));
   for(File file:receipts){total=writeFile(zip,file,"receipts/"+file.getName(),total);count++;}
  }
  // Save user-selected online gallery images, not auto-recoverable downloaded thumbnails.
  File modelFolder=new File(internal,"moto-year-photos");
  requireDirectoryIfPresent(modelFolder);
  File[] selectedModels=modelFolder.listFiles((dir,name)->name.matches(BackupFormat.MODEL_PHOTO));
  if(media&&selectedModels!=null){
   Arrays.sort(selectedModels,Comparator.comparing(File::getName));
   int selectedCount=0;
   for(File image:selectedModels){
    requireRegular(image);
    File metadata=new File(modelFolder,image.getName()+".json");
    if(!metadata.exists()&&!Files.isSymbolicLink(metadata.toPath()))continue; // no manual selection
    byte[] attribution=readSidecar(metadata,8192);
    if(!BackupFormat.selectedModelMetadata(new String(attribution,StandardCharsets.UTF_8)))continue;
    if(++selectedCount>BackupFormat.MAX_PHOTOS)throw new IOException("직접 고른 모델 사진이 너무 많습니다.");
    total=writeFile(zip,image,"model-photos/"+image.getName(),total);count++;
    total=writeFile(zip,metadata,"model-photos/"+metadata.getName(),total);count++;
   }
  }
  File rides=new File(internal,"rides");File[] tracks=rides.listFiles((dir,name)->name.matches("[0-9]{10,15}-[a-f0-9-]{36}\\.track"));
  if(gps&&tracks!=null){if(tracks.length>BackupFormat.MAX_TRACKS)throw new IOException("주행 기록 10,000건 백업 한도를 초과했습니다.");
   Arrays.sort(tracks,Comparator.comparing(File::getName));
   for(File file:tracks){
    // A completely empty crashed session has no location bytes worth archiving.
    // Preserve non-empty interrupted tracks, including their unfinished final row.
    requireRegular(file);
    if(file.length()==0){
     if(new File(file.getPath()+".done").exists()||new File(file.getPath()+".imported").exists()||new File(file.getPath()+".recovered").exists())
      throw new IOException("완료 표시가 있는 빈 GPS 기록을 백업할 수 없습니다: "+file.getName());
     continue;
    }
    total=writeFile(zip,file,"rides/"+file.getName(),total);count++;
    if(new File(file.getPath()+".done").isFile()||new File(file.getPath()+".recovered").isFile()){
     // Preflight validates both the GPS source and the exact generated size.
     // NEVER suppress a failure after opening a ZIP entry: that would silently
     // ship a truncated GPX and still report backup success.
     long estimatedGpx=0;
     try{if(TrackFile.read(file,(x,y,z,t)->{})>=2)estimatedGpx=gpxBytes(file);}
     catch(Exception invalidTrack){
      // The original track is always included; damaged/interrupted tracks
      // cannot be represented as a trustworthy GPX route.
     }
     if(estimatedGpx>0&&total<=MAX_TOTAL-estimatedGpx){
      zip.putNextEntry(new ZipEntry("gpx/"+file.getName().replace(".track",".gpx")));
      TrackFile.gpx(file,zip); // Any mid-write error aborts the full export.
      zip.closeEntry();total+=estimatedGpx;
     }
    }
    
    // Merged source metadata must round-trip. Without it restored monthly
    // statistics would count both originals plus the combined ride.
    File merged=new File(file.getPath()+".mergedinto");
    if(merged.isFile()||Files.isSymbolicLink(merged.toPath())){
     byte[] markerBytes=readSidecar(merged,100);
     String destination=new String(markerBytes,StandardCharsets.US_ASCII);
     if(markerBytes.length>100||!destination.matches("[0-9]{10,15}-[a-f0-9-]{36}\\.track")||
       !new File(rides,destination+".done").isFile())
      throw new IOException("통합 라이딩 연결 정보가 손상됐습니다: "+file.getName());
     total=writeFile(zip,merged,"rides/"+merged.getName(),total);
    }
    File rideBike=new File(file.getPath()+".bike");
    if(rideBike.isFile()||Files.isSymbolicLink(rideBike.toPath())){
     if(!new String(readSidecar(rideBike,64),StandardCharsets.US_ASCII)
       .matches("[0-9a-fA-F]{8}(-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}"))
      throw new IOException("라이딩 바이크 연결 정보가 손상됐습니다: "+file.getName());
     total=writeFile(zip,rideBike,"rides/"+rideBike.getName(),total);
    }
    File category=new File(file.getPath()+".category");
    if(category.isFile()||Files.isSymbolicLink(category.toPath())){
     if(!java.util.Arrays.asList("자동 분류","일상","출퇴근","와인딩","장거리","투어").contains(new String(readSidecar(category,64),StandardCharsets.UTF_8)))
      throw new IOException("라이딩 분류가 손상됐습니다: "+file.getName());
     total=writeFile(zip,category,"rides/"+category.getName(),total);
    }
    for(String suffix:new String[]{".done",".imported",".recovered",".unmerged",".favorite"}){
     File marker=new File(file.getPath()+suffix);if(marker.isFile()||Files.isSymbolicLink(marker.toPath())){
      requireRegular(marker);
      if(marker.length()!=0)throw new IOException("라이딩 상태 표시가 손상됐습니다: "+marker.getName());
      zip.putNextEntry(new ZipEntry("rides/"+marker.getName()));zip.closeEntry();
     }
    }
   }
  }
  // Preserve quarantined GPS inside its own namespace; refuse incomplete trash structures.
  if(gps){
   List<File> retired=trashTracks(internal);
   if(retired.size()+(tracks==null?0:tracks.length)>BackupFormat.MAX_TRACKS)
    throw new IOException("GPS와 복구함 기록 합계가 백업 한도를 넘었어요.");
   for(File track:retired){
    String name=track.getName(),prefix="rides-trash/"+name+"/";
    if(track.length()==0)throw new IOException("복구함 GPS가 비어 있습니다: "+name);
    total=writeFile(zip,track,prefix+name,total);count++;
    for(String suffix:TRASH_SIDECARS){
     File marker=new File(track.getPath()+suffix);
     if(Files.exists(marker.toPath(),LinkOption.NOFOLLOW_LINKS)){
      requireRegular(marker);
      if(marker.length()>BackupFormat.entryLimit(prefix+name+suffix))
       throw new IOException("복구함 GPS 메타데이터 크기 초과");
      total=writeFile(zip,marker,prefix+name+suffix,total);count++;
     }
    }
   }
  }
  zip.finish();zip.flush();return count;
 }
}
