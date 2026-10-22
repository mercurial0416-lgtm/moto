package com.motohub.next;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

public class RestoreArchiveTest {
 static int checks;
 static void ok(boolean pass,String label){checks++;if(!pass)throw new AssertionError(label);}
 static final String T="1792122693079-12345678-1234-1234-1234-123456789abc.track";
 static final String P="garage-photo-12345678-1234-1234-1234-123456789abc.jpg";
 static final String MODEL="abcdef0123456789abcdef01.jpg";
 static final byte[] MODEL_JPG={(byte)0xff,(byte)0xd8,0x55,0x66,(byte)0xff,(byte)0xd9};
 static final byte[] JPG={(byte)0xff,(byte)0xd8,0x01,0x02,(byte)0xff,(byte)0xd9};
 static byte[] archive(String[][] entries)throws Exception {ByteArrayOutputStream b=new ByteArrayOutputStream();try(ZipOutputStream z=new ZipOutputStream(b)){for(String[] pair:entries){z.putNextEntry(new ZipEntry(pair[0]));z.write(pair[1].getBytes(java.nio.charset.StandardCharsets.UTF_8));z.closeEntry();}}return b.toByteArray();}
 static byte[] legacy()throws Exception{
  ByteArrayOutputStream b=new ByteArrayOutputStream();try(ZipOutputStream z=new ZipOutputStream(b)){
   put(z,"MOTO-HUB-data.json","{\"version\":3,\"vehicles\":[],\"listings\":[],\"services\":[],\"expenses\":[]}".getBytes());
   put(z,"photos/"+P,JPG);
   put(z,"rides/"+T,(TrackFile.point(37,127,5,1792120000000L)+TrackFile.point(37.001,127,5,1792120010000L)).getBytes());
   put(z,"rides/"+T+".recovered",new byte[0]);
  }return b.toByteArray();
 }
 static byte[] good()throws Exception{
  ByteArrayOutputStream b=new ByteArrayOutputStream();try(ZipOutputStream z=new ZipOutputStream(b)){
   put(z,"MOTO-HUB-data.json","{\"version\":3,\"vehicles\":[],\"listings\":[],\"services\":[],\"expenses\":[]}".getBytes());
   put(z,"photos/"+P,JPG);
   // Standard ZIP creators often include explicit directory entries.
   put(z,"model-photos/",new byte[0]);
   put(z,"model-photos/"+MODEL,MODEL_JPG);
   put(z,"model-photos/"+MODEL+".json","{\"userSelected\":true,\"label\":\"Honda Super Cub 110\"}".getBytes());
   put(z,"rides/"+T,(TrackFile.point(37,127,5,1792120000000L)+TrackFile.point(37.001,127,5,1792120010000L)).getBytes());
   put(z,"rides/"+T+".recovered",new byte[0]);
  }return b.toByteArray();
 }
 static void put(ZipOutputStream z,String name,byte[] content)throws Exception{z.putNextEntry(new ZipEntry(name));z.write(content);z.closeEntry();}
 static void reject(File root,byte[] b){try(RestoreArchive.Prepared p=RestoreArchive.prepare(new ByteArrayInputStream(b),root)){throw new AssertionError("accepted unsafe archive");}catch(AssertionError e){throw e;}catch(Exception correct){checks++;}}
 public static void main(String[]args)throws Exception{
  File root=Files.createTempDirectory("moto-restore").toFile();try{
   File rides=new File(root,"rides");rides.mkdirs();Files.write(new File(root,P).toPath(),JPG);
   Files.write(new File(rides,T).toPath(),TrackFile.point(35,127,5,1792120000000L).getBytes());
   byte[] zip=good();
   File obsolete=new File(rides,"1792122693080-12345678-1234-1234-1234-123456789abc.track");
   Files.write(obsolete.toPath(),TrackFile.point(35,127,5,1792120000000L).getBytes());
   try(RestoreArchive.Prepared p=RestoreArchive.prepare(new ByteArrayInputStream(zip),root)){
    ok(p.photos==2,"manual and gallery photos count");ok(p.rides==1,"ride count");ok(p.json.contains("vehicles"),"json");
    p.install(root,value->{
     ok(!obsolete.exists(),"obsolete ride deleted before JSON commit");
     ok(value.contains("version"),"writer JSON");
    });
   }
   ok(!obsolete.exists(),"obsolete ride removed after successful restore");
   ok(Arrays.equals(JPG,Files.readAllBytes(new File(root,P).toPath())),"photo copied");
   File galleryImage=new File(root,"moto-year-photos/"+MODEL);
   File galleryMetadata=new File(root,"moto-year-photos/"+MODEL+".json");
   ok(Arrays.equals(MODEL_JPG,Files.readAllBytes(galleryImage.toPath())),"chosen model JPG roundtrips");
   ok(Files.readString(galleryMetadata.toPath()).contains("userSelected"),"chosen image attribution roundtrips");
   try(RestoreArchive.Prepared earlier=RestoreArchive.prepare(new ByteArrayInputStream(legacy()),root)){
    earlier.install(root,value->{});
   }
   ok(Arrays.equals(MODEL_JPG,Files.readAllBytes(galleryImage.toPath())),"legacy ZIP retains current selected model image");
   ok(Files.readString(galleryMetadata.toPath()).contains("userSelected"),"legacy ZIP keeps selected attribution");
   ok(TrackFile.read(new File(rides,T),(a,b,c,d)->{})==2,"GPS restored");
   ok(new File(rides,T+".recovered").exists(),"recovery marker copied");
   byte[] original=Files.readAllBytes(new File(rides,T).toPath());
   byte[] previousModelJpg={(byte)0xff,(byte)0xd8,0x01,0x02,(byte)0xff,(byte)0xd9};
   Files.write(galleryImage.toPath(),previousModelJpg);
   Files.writeString(galleryMetadata.toPath(),"{\"userSelected\":true,\"label\":\"Local selected version\"}");
   Files.write(obsolete.toPath(),TrackFile.point(35,127,5,1792120000000L).getBytes());
   try(RestoreArchive.Prepared p=RestoreArchive.prepare(new ByteArrayInputStream(zip),root)){
    try{p.install(root,data->{
     ok(!obsolete.exists(),"obsolete ride removed before failing JSON commit");
     throw new IOException("test writer failure");
    });throw new AssertionError("write failed but no exception");}catch(IOException expected){checks++;}
   }
   ok(obsolete.exists(),"failed JSON commit restores obsolete ride");
   ok(Arrays.equals(original,Files.readAllBytes(new File(rides,T).toPath())),"failed commit must restore old track");
   ok(Arrays.equals(previousModelJpg,Files.readAllBytes(galleryImage.toPath())),"failed JSON commit restores chosen gallery image");
   ok(Files.readString(galleryMetadata.toPath()).contains("Local selected version"),"failed commit restores attribution");
   reject(root,archive(new String[][]{{"../bad","oops"},{"MOTO-HUB-data.json","{}"}}));
   reject(root,archive(new String[][]{{"MOTO-HUB-data.json","{}"},{"other.txt","x"}}));
   reject(root,archive(new String[][]{{"MOTO-HUB-data.json","{}"},{"rides/"+T,"badGPS"}}));
   reject(root,archive(new String[][]{{"MOTO-HUB-data.json","{}"},{"rides/"+T+".done",""}}));
   reject(root,archive(new String[][]{{"MOTO-HUB-data.json","{}"},{"photos/"+P,"not JPEG"}}));
   reject(root,archive(new String[][]{{"README-KO.txt","missing json"}}));
   reject(root,archive(new String[][]{{"MOTO-HUB-data.json","{}"},{"model-photos/"+MODEL+".json","{\"userSelected\":true}"}}));
   reject(root,archive(new String[][]{{"MOTO-HUB-data.json","{}"},{"model-photos/"+MODEL,"badPhoto"}}));
   reject(root,archive(new String[][]{{"MOTO-HUB-data.json","{}"},{"rides/"+T+".done","evil"}}));
   reject(root,archive(new String[][]{{"MOTO-HUB-data.json","{}"},{"rides/"+T+".recovered",""}}));
   ok(new File(root,"bad").exists()==false,"zip slip blocked");
   // A manipulated year-photo cache folder must never redirect restored files
   // outside the app's private files directory.
   File modelFolder=new File(root,"moto-year-photos"),parked=new File(root,"preserved-model-cache");
   File external=Files.createTempDirectory("moto-external-model-photo").toFile();
   try{
    ok(modelFolder.renameTo(parked),"park existing year-photo cache");
    Files.createSymbolicLink(modelFolder.toPath(),external.toPath());
    try(RestoreArchive.Prepared ready=RestoreArchive.prepare(new ByteArrayInputStream(zip),root)){
     try{ready.install(root,json->{});throw new AssertionError("unsafe model cache dir allowed");}
     catch(IOException expected){ok(expected.getMessage().contains("안전하지"),"symlink year-photo folder blocked");}
    }
    ok(Objects.requireNonNull(external.listFiles()).length==0,"no files created outside private storage");
   }finally{
    Files.deleteIfExists(modelFolder.toPath());
    ok(parked.renameTo(modelFolder),"restore original year-photo cache");
    external.delete();
   }
   System.out.println("PASS ZIP restore safety and rollback checks: "+checks);
  } finally {clean(root);}
 }
 static void clean(File f){File[] files=f.listFiles();if(files!=null)for(File sub:files)clean(sub);f.delete();}
}
