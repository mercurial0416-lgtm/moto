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
   byte[] jpg={(byte)255,(byte)216,1,2,(byte)255,(byte)217};
   File photo=new File(root,"garage-photo-12345678-1234-1234-1234-123456789abc.jpg");Files.write(photo.toPath(),jpg);
   File galleryDir=new File(root,"moto-year-photos");galleryDir.mkdirs();
   File selected=new File(galleryDir,"abcdef0123456789abcdef01.jpg");Files.write(selected.toPath(),jpg);
   File attribution=new File(galleryDir,selected.getName()+".json");
   Files.writeString(attribution.toPath(),"{\"userSelected\":true,\"label\":\"Honda Super Cub\"}");
   ByteArrayOutputStream out=new ByteArrayOutputStream();int files=BackupBundle.export("{\"version\":3}",root,out);
   ok(files==5,"selected gallery image + attribution included");
   Set<String> entries=new HashSet<>();try(ZipInputStream zip=new ZipInputStream(new ByteArrayInputStream(out.toByteArray()))){ZipEntry e;while((e=zip.getNextEntry())!=null)entries.add(e.getName());}
   for(String n:new String[]{"README-KO.txt","MOTO-HUB-data.json","rides/"+track.getName(),"photos/"+photo.getName(),"gpx/"+track.getName().replace(".track",".gpx"),"rides/"+track.getName()+".recovered",
    "model-photos/"+selected.getName(),"model-photos/"+attribution.getName()})ok(entries.contains(n),"missing "+n);
   BackupIntegrity.Result verified=BackupIntegrity.verify(new ByteArrayInputStream(out.toByteArray()));
   ok(verified.photos==2,"selected gallery photo verified by ZIP readback");
   ok(entries.stream().noneMatch(x->x.contains("..")),"zip traversal");
   // Broken images must fail *before* a ZIP can be mistaken for a usable backup.
   Files.write(selected.toPath(),new byte[]{1,2,3,4,5,6});
   try{
    BackupBundle.export("{\"version\":3}",root,new ByteArrayOutputStream());
    throw new AssertionError("corrupted selected photo was archived");
   }catch(IOException expected){ok(expected.getMessage().contains("손상"),"corrupted gallery JPEG rejected");}
   Files.write(selected.toPath(),jpg);
   Files.write(photo.toPath(),new byte[]{1,2,3,4,5,6});
   try{
    BackupBundle.export("{\"version\":3}",root,new ByteArrayOutputStream());
    throw new AssertionError("corrupted garage photo was archived");
   }catch(IOException expected){ok(expected.getMessage().contains("손상"),"corrupted garage JPEG rejected");}
   Files.write(photo.toPath(),jpg);
   // Reject symlinks even when their file names match our normal backup patterns.
   File outside=File.createTempFile("moto-external-secret",".jpg");
   try{
    Files.write(outside.toPath(),new byte[]{42,42,42});
    File linkedPhoto=new File(root,"garage-photo-87654321-4321-4321-4321-abcdef012345.jpg");
    Files.createSymbolicLink(linkedPhoto.toPath(),outside.toPath());
    try{BackupBundle.export("{\"version\":3}",root,new ByteArrayOutputStream());throw new AssertionError("symlink photo exported");}
    catch(IOException expected){ok(expected.getMessage().contains("일반 파일"),"photo symlink blocked");}
    Files.delete(linkedPhoto.toPath());
    File sidecar=new File(track.getPath()+".bike");
    Files.createSymbolicLink(sidecar.toPath(),outside.toPath());
    try{BackupBundle.export("{\"version\":3}",root,new ByteArrayOutputStream());throw new AssertionError("symlink bike marker exported");}
    catch(IOException expected){ok(expected.getMessage().contains("일반 파일"),"GPS sidecar symlink blocked");}
    Files.delete(sidecar.toPath());
   }finally{Files.deleteIfExists(outside.toPath());}
   File linkedRides=Files.createTempDirectory("moto-external-rides").toFile();
   try{
    // An attacker-controlled rides directory must not become part of the export.
    for(File f:Objects.requireNonNull(rides.listFiles()))f.delete();
    rides.delete();
    Files.createSymbolicLink(rides.toPath(),linkedRides.toPath());
    try{BackupBundle.export("{\"version\":3}",root,new ByteArrayOutputStream());throw new AssertionError("symlink rides dir accepted");}
    catch(IOException expected){ok(expected.getMessage().contains("폴더가 안전하지"),"rides symlink blocked");}
    Files.delete(rides.toPath());
    rides.mkdirs();
   }finally{linkedRides.delete();}
   System.out.println("PASS full ZIP backup: file, photo, ride, GPX, privacy README");
  }finally{
   for(File f:Objects.requireNonNull(rides.listFiles()))f.delete();rides.delete();
   File modelDir=new File(root,"moto-year-photos");
   File[] chosen=modelDir.listFiles();if(chosen!=null)for(File f:chosen)f.delete();
   modelDir.delete();
   for(File f:Objects.requireNonNull(root.listFiles()))f.delete();root.delete();
  }
 }
}
