package com.motohub.next;
import android.content.Context;
import org.json.JSONObject;
import org.json.JSONArray;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.LinkOption;
/** Counts and byte totals for post-update audit, never a substitute for full ZIP backup. */
final class UpdateDataFootprint {
 private UpdateDataFootprint(){}
 static String capture(Context context,JSONObject data)throws Exception{
  JSONObject checked=Store.parse(data.toString());
  StringBuilder value=new StringBuilder("schema3");
  for(String collection:Store.COLLECTIONS){
   JSONArray records=checked.getJSONArray(collection);
   value.append(';').append(collection).append('=').append(records.length());
  }
  File rides=new File(context.getFilesDir(),"rides");
  File[] files=rides.isDirectory()?rides.listFiles():null;
  long count=0,bytes=0,complete=0;
  if(files!=null)for(File file:files){
   if(!file.getName().matches("[0-9]{10,15}-[a-f0-9-]{36}\\.track"))continue;
   if(Files.isSymbolicLink(file.toPath())||!Files.isRegularFile(file.toPath(),LinkOption.NOFOLLOW_LINKS))continue;
   count++;bytes+=file.length();
   if(new File(file.getPath()+".done").isFile())complete++;
  }
  value.append(";tracks=").append(count).append(";trackBytes=").append(bytes)
   .append(";complete=").append(complete);
  File[] photos=context.getFilesDir().listFiles();
  long n=0,size=0;
  if(photos!=null)for(File file:photos){
   if(!file.getName().matches("garage-photo-[a-fA-F0-9-]{1,64}\\.jpg"))continue;
   if(Files.isSymbolicLink(file.toPath())||!Files.isRegularFile(file.toPath(),LinkOption.NOFOLLOW_LINKS))continue;
   n++;size+=file.length();
  }
  return value.append(";garagePhotos=").append(n).append(";garagePhotoBytes=").append(size).toString();
 }
}
