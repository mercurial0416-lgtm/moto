package com.motohub.next;
import android.content.Context;
import org.json.JSONObject;
import org.json.JSONArray;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Full-file SHA-256 audit of recognized GPS, all track sidecars, GPS trash and garage photos,
 * along with per-collection record counts. No coordinates, file names or image
 * bytes are stored in preferences: only the aggregate digest.
 */
final class UpdateDataFootprint {
 private static final long MAX_HASH_BYTES=1_500_000_000L;
 private UpdateDataFootprint(){}
 static String capture(Context context,JSONObject data)throws Exception{
  JSONObject checked=Store.parse(data.toString());
  StringBuilder value=new StringBuilder("schema4;dataSha256=").append(JsonCanonicalDigest.hash(checked));
  for(String collection:Store.COLLECTIONS){
   JSONArray records=checked.getJSONArray(collection);
   value.append(';').append(collection).append('=').append(records.length());
  }
  File rides=new File(context.getFilesDir(),"rides");
  requireDirectoryIfPresent(rides);
  File[] tracks=rides.isDirectory()?rides.listFiles():null;
  long count=0,bytes=0,complete=0,photoCount=0,photoBytes=0,trashCount=0;
  int sidecarCount=0;
  List<File> toHash=new ArrayList<>();
  if(rides.isDirectory()&&tracks==null)throw new IOException("GPS 폴더를 검사할 수 없습니다.");
  if(tracks!=null)for(File file:tracks){
   if(!file.getName().matches(BackupFormat.TRACK))continue;
   verifyRegular(file);
   if(file.length()>TrackFile.MAX_BYTES)throw new IOException("GPS 파일이 허용 범위를 초과했습니다.");
   count++;bytes+=file.length();toHash.add(file);
   if(Files.exists(new File(file.getPath()+".done").toPath(),LinkOption.NOFOLLOW_LINKS))complete++;
   sidecarCount+=collectSidecars(file,toHash);
  }
  // Quarantined GPS and every backed-up companion marker are also user data.
  // This is a read-only audit: never move or restore files while hashing.
  File trash=new File(context.getFilesDir(),"rides-trash");
  requireDirectoryIfPresent(trash);
  if(trash.isDirectory()){
   File[] folders=trash.listFiles();
   if(folders==null)throw new IOException("GPS 복구함을 검사할 수 없습니다.");
   for(File folder:folders){
    if(Files.isSymbolicLink(folder.toPath())||!Files.isDirectory(folder.toPath(),LinkOption.NOFOLLOW_LINKS)
       ||!folder.getName().matches(BackupFormat.TRACK))
     throw new IOException("GPS 복구함에 잘못된 항목이 있습니다.");
    File source=new File(folder,folder.getName());
    verifyRegular(source);
    if(source.length()>TrackFile.MAX_BYTES)throw new IOException("복구함 GPS 크기 초과");
    toHash.add(source);trashCount++;
    sidecarCount+=collectSidecars(source,toHash);
   }
  }
  File[] photos=context.getFilesDir().listFiles();
  if(photos!=null)for(File file:photos){
   if(!file.getName().matches("garage-photo-[a-fA-F0-9-]{1,64}\\.jpg"))continue;
   verifyRegular(file);
   photoCount++;photoBytes+=file.length();toHash.add(file);
  }
  Collections.sort(toHash,(left,right)->left.getAbsolutePath().compareTo(right.getAbsolutePath()));
  MessageDigest digest=MessageDigest.getInstance("SHA-256");
  long total=0;
  byte[] buffer=new byte[32768];
  for(File file:toHash){
   String path=file.getAbsolutePath();
   // Only digest an allowlisted local file. No external network.
   digest.update(path.substring(context.getFilesDir().getAbsolutePath().length()).getBytes(StandardCharsets.UTF_8));
   digest.update((byte)0);
   try(FileInputStream in=new FileInputStream(file)){
    int n;while((n=in.read(buffer))!=-1){
     total+=n;
     if(total>MAX_HASH_BYTES)throw new IOException("파일 검증 총량이 너무 큽니다. 전체 ZIP 백업 후 확인하세요.");
     digest.update(buffer,0,n);
    }
   }
   digest.update((byte)0);
  }
  StringBuilder hash=new StringBuilder();
  for(byte b:digest.digest())hash.append(String.format(java.util.Locale.ROOT,"%02x",b&255));
  return value.append(";tracks=").append(count).append(";trackBytes=").append(bytes)
   .append(";complete=").append(complete).append(";trashTracks=").append(trashCount)
   .append(";gpsSidecars=").append(sidecarCount).append(";garagePhotos=").append(photoCount)
   .append(";garagePhotoBytes=").append(photoBytes).append(";contentSha256=").append(hash).toString();
 }
 private static final String[] TRACK_SIDECARS={
  ".done",".imported",".recovered",".unmerged",".category",".bike",".favorite",".mergedinto"
 };
 private static int collectSidecars(File source,List<File> toHash)throws IOException{
  int found=0;
  for(String suffix:TRACK_SIDECARS){
   File sidecar=new File(source.getPath()+suffix);
   if(!Files.exists(sidecar.toPath(),LinkOption.NOFOLLOW_LINKS))continue;
   verifyRegular(sidecar);toHash.add(sidecar);found++;
  }
  return found;
 }
 private static void requireDirectoryIfPresent(File dir)throws IOException{
  if(Files.exists(dir.toPath(),LinkOption.NOFOLLOW_LINKS) &&
    (Files.isSymbolicLink(dir.toPath())||!Files.isDirectory(dir.toPath(),LinkOption.NOFOLLOW_LINKS)))
   throw new IOException("안전하지 않은 GPS 검사 폴더입니다.");
 }
 private static void verifyRegular(File file)throws IOException{
  if(Files.isSymbolicLink(file.toPath())||
     !Files.isRegularFile(file.toPath(),LinkOption.NOFOLLOW_LINKS))
   throw new IOException("안전하지 않은 백업 검증 파일입니다.");
 }
}
