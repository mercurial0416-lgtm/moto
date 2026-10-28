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
 * Full-file SHA-256 audit of every recognized GPS, GPS bike-binding and garage photo,
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
  File[] tracks=rides.isDirectory()?rides.listFiles():null;
  long count=0,bytes=0,complete=0,photoCount=0,photoBytes=0;
  List<File> toHash=new ArrayList<>();
  if(tracks!=null)for(File file:tracks){
   if(!file.getName().matches("[0-9]{10,15}-[a-f0-9-]{36}\\.track"))continue;
   verifyRegular(file);
   if(file.length()>TrackFile.MAX_BYTES)throw new IOException("GPS 파일이 허용 범위를 초과했습니다.");
   count++;bytes+=file.length();toHash.add(file);
   File done=new File(file.getPath()+".done");
   if(done.exists()){verifyRegular(done);complete++;toHash.add(done);}
   File bike=new File(file.getPath()+".bike");
   if(bike.exists()){verifyRegular(bike);toHash.add(bike);}
   File recovered=new File(file.getPath()+".recovered");
   if(recovered.exists()){verifyRegular(recovered);toHash.add(recovered);}
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
   .append(";complete=").append(complete).append(";garagePhotos=").append(photoCount)
   .append(";garagePhotoBytes=").append(photoBytes).append(";contentSha256=").append(hash).toString();
 }
 private static void verifyRegular(File file)throws IOException{
  if(Files.isSymbolicLink(file.toPath())||
     !Files.isRegularFile(file.toPath(),LinkOption.NOFOLLOW_LINKS))
   throw new IOException("안전하지 않은 백업 검증 파일입니다.");
 }
}
