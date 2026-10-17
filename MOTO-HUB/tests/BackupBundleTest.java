package com.motohub.next;
import java.io.*;import java.nio.charset.StandardCharsets;import java.nio.file.*;import java.util.*;import java.util.zip.*;
public class BackupBundleTest {
 static void ok(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[] args)throws Exception{
  File root=Files.createTempDirectory("motohub-backup").toFile(),rides=new File(root,"rides");rides.mkdirs();
  File track=new File(rides,"1792122693079-12345678-1234-1234-1234-123456789abc.track");
  try{
   Files.writeString(track.toPath(),TrackFile.point(37,127,5,1792120000000L)+TrackFile.point(37.001,127,5,1792120010000L));
   new File(track.getPath()+".recovered").createNewFile();
   File photo=new File(root,"garage-photo-12345678-1234-1234-1234-123456789abc.jpg");Files.write(photo.toPath(),new byte[]{1,2,3});
   ByteArrayOutputStream out=new ByteArrayOutputStream();int files=BackupBundle.export("{\"version\":3}",root,out);
   ok(files==3,"file count");
   Set<String> entries=new HashSet<>();try(ZipInputStream zip=new ZipInputStream(new ByteArrayInputStream(out.toByteArray()))){ZipEntry e;while((e=zip.getNextEntry())!=null)entries.add(e.getName());}
   for(String n:new String[]{"README-KO.txt","MOTO-HUB-data.json","rides/"+track.getName(),"photos/"+photo.getName(),"gpx/"+track.getName().replace(".track",".gpx"),"rides/"+track.getName()+".recovered"})ok(entries.contains(n),"missing "+n);
   ok(entries.stream().noneMatch(x->x.contains("..")),"zip traversal");
   System.out.println("PASS full ZIP backup: file, photo, ride, GPX, privacy README");
  }finally{for(File f:Objects.requireNonNull(rides.listFiles()))f.delete();rides.delete();for(File f:Objects.requireNonNull(root.listFiles()))f.delete();root.delete();}
 }
}
