package com.motohub.next;
import java.io.*;import java.nio.file.*;import java.util.*;import java.util.zip.*;
public final class RideFavoriteTest {
 static int checks;static void ok(boolean x,String s){checks++;if(!x)throw new AssertionError(s);}
 static void clean(File f)throws Exception{File[] items=f.listFiles();if(items!=null)for(File c:items)clean(c);Files.deleteIfExists(f.toPath());}
 public static void main(String[] args)throws Exception{
  File root=Files.createTempDirectory("moto-star").toFile();
  try{
   File dir=new File(root,"rides");dir.mkdirs();
   File ride=new File(dir,"1792122693079-12345678-1234-1234-1234-123456789abc.track");
   Files.writeString(ride.toPath(),TrackFile.point(37,127,5,1792120000000L));
   ok(!RideFavorite.isFavorite(ride),"unstarred default");
   ok(RideFavorite.toggle(ride),"star");
   ok(RideFavorite.isFavorite(ride),"starred");
   ByteArrayOutputStream zip=new ByteArrayOutputStream();
   BackupBundle.export("{}",root,zip);
   Set<String> entries=new HashSet<>();
   try(ZipInputStream in=new ZipInputStream(new ByteArrayInputStream(zip.toByteArray()))){
    ZipEntry e;while((e=in.getNextEntry())!=null)entries.add(e.getName());
   }
   ok(entries.contains("rides/"+ride.getName()+".favorite"),"favorite marker included in full ZIP");
   BackupIntegrity.verify(new ByteArrayInputStream(zip.toByteArray()));
   File dest=Files.createTempDirectory("moto-star-restored").toFile();
   try(RestoreArchive.Prepared ready=RestoreArchive.prepare(new ByteArrayInputStream(zip.toByteArray()),dest)){
    ready.install(dest,value->{});
    ok(RideFavorite.isFavorite(new File(dest,"rides/"+ride.getName())),"favorite survives ZIP roundtrip");
   }finally{clean(dest);}
   ok(!RideFavorite.toggle(ride),"unstar");ok(!RideFavorite.isFavorite(ride),"unstarred");
   File link=new File(dir,ride.getName()+".favorite");
   File external=File.createTempFile("moto-outside-star",".txt");
   try{
    Files.createSymbolicLink(link.toPath(),external.toPath());
    try{RideFavorite.toggle(ride);throw new AssertionError("unsafe marker");}
    catch(IOException expected){checks++;}
    ok(external.exists(),"do not follow symlink");
   }finally{Files.deleteIfExists(link.toPath());external.delete();}
   System.out.println("PASS ride favorite backup/safety "+checks);
  }finally{clean(root);}
 }
}
