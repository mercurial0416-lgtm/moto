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
 static final byte[] JPG={(byte)0xff,(byte)0xd8,0x01,0x02,(byte)0xff,(byte)0xd9};
 static byte[] archive(String[][] entries)throws Exception {ByteArrayOutputStream b=new ByteArrayOutputStream();try(ZipOutputStream z=new ZipOutputStream(b)){for(String[] pair:entries){z.putNextEntry(new ZipEntry(pair[0]));z.write(pair[1].getBytes(java.nio.charset.StandardCharsets.UTF_8));z.closeEntry();}}return b.toByteArray();}
 static byte[] good()throws Exception{
  ByteArrayOutputStream b=new ByteArrayOutputStream();try(ZipOutputStream z=new ZipOutputStream(b)){
   put(z,"MOTO-HUB-data.json","{\"version\":3,\"vehicles\":[],\"listings\":[],\"services\":[],\"expenses\":[]}".getBytes());
   put(z,"photos/"+P,JPG);
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
   try(RestoreArchive.Prepared p=RestoreArchive.prepare(new ByteArrayInputStream(zip),root)){
    ok(p.photos==1,"photo count");ok(p.rides==1,"ride count");ok(p.json.contains("vehicles"),"json");
    p.install(root,value->ok(value.contains("version"),"writer JSON"));
   }
   ok(Arrays.equals(JPG,Files.readAllBytes(new File(root,P).toPath())),"photo copied");
   ok(TrackFile.read(new File(rides,T),(a,b,c,d)->{})==2,"GPS restored");
   ok(new File(rides,T+".recovered").exists(),"recovery marker copied");
   byte[] original=Files.readAllBytes(new File(rides,T).toPath());
   try(RestoreArchive.Prepared p=RestoreArchive.prepare(new ByteArrayInputStream(zip),root)){
    try{p.install(root,data->{throw new IOException("test writer failure");});throw new AssertionError("write failed but no exception");}catch(IOException expected){checks++;}
   }
   ok(Arrays.equals(original,Files.readAllBytes(new File(rides,T).toPath())),"failed commit must restore old track");
   try(RestoreArchive.Prepared p=RestoreArchive.prepare(new ByteArrayInputStream(zip),root)){
    try{p.install(root,data->{
     File blocked=new File(root,P);Files.delete(blocked.toPath());blocked.mkdir();
     Files.write(new File(blocked,"blocker").toPath(),new byte[]{1});
     throw new IOException("simulate rollback destination blocked");
    });throw new AssertionError("expected rollback failure");}catch(IOException expected){
     ok(expected.getMessage().contains("안전 사본"),"recovery location reported");
    }
   }
   File[] retained=root.listFiles((d,n)->n.startsWith("moto-rollback-"));
   ok(retained!=null&&retained.length==1,"failed rollback preserves backup");
   ok(Arrays.equals(JPG,Files.readAllBytes(new File(retained[0],P).toPath())),"original photo retained");
   ok(Arrays.equals(original,Files.readAllBytes(new File(retained[0],"rides/"+T).toPath())),"original track retained");
   reject(root,archive(new String[][]{{"../bad","oops"},{"MOTO-HUB-data.json","{}"}}));
   reject(root,archive(new String[][]{{"MOTO-HUB-data.json","{}"},{"other.txt","x"}}));
   reject(root,archive(new String[][]{{"MOTO-HUB-data.json","{}"},{"rides/"+T,"badGPS"}}));
   reject(root,archive(new String[][]{{"MOTO-HUB-data.json","{}"},{"rides/"+T+".done",""}}));
   reject(root,archive(new String[][]{{"MOTO-HUB-data.json","{}"},{"photos/"+P,"not JPEG"}}));
   reject(root,archive(new String[][]{{"README-KO.txt","missing json"}}));
   reject(root,archive(new String[][]{{"MOTO-HUB-data.json","{}"},{"rides/"+T+".done","evil"}}));
   reject(root,archive(new String[][]{{"MOTO-HUB-data.json","{}"},{"rides/"+T+".recovered",""}}));
   ok(new File(root,"bad").exists()==false,"zip slip blocked");
   System.out.println("PASS ZIP restore safety and rollback checks: "+checks);
  } finally {clean(root);}
 }
 static void clean(File f){File[] files=f.listFiles();if(files!=null)for(File sub:files)clean(sub);f.delete();}
}
