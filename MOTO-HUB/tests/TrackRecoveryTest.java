package com.motohub.next;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class TrackRecoveryTest {
 static int checks=0;
 static void ok(boolean value,String reason){checks++;if(!value)throw new AssertionError(reason);}
 static void reject(File f){boolean rejected=false;try{TrackRecovery.recover(f);}catch(Exception expected){rejected=true;}ok(rejected,"unexpectedly accepted "+f);}
 static void put(File f,String text)throws Exception{Files.writeString(f.toPath(),text,StandardCharsets.UTF_8);}
 static String point(long t){return TrackFile.point(37.0,127.0,5,t);}
 static void clean(File dir){File[] all=dir.listFiles();if(all!=null)for(File f:all)f.delete();dir.delete();}
 public static void main(String[] args)throws Exception{
  File dir=Files.createTempDirectory("ride-recovery-test").toFile();
  String name="1792122693079-12345678-1234-1234-1234-123456789abc.track";
  File f=new File(dir,name);String original=point(1792120000000L)+point(1792120010000L);
  try{
   put(f,original+"37.5,127.");
   TrackRecovery.Result recovered=TrackRecovery.recover(f);
   ok(recovered.points==2,"preserve complete points");
   ok(recovered.discardedBytes==9,"trim partial row");
   ok(Files.readString(f.toPath()).equals(original),"exact original rows");
   ok(new File(f.getPath()+".recovered").isFile(),"recovery marker");
   ok(!new File(f.getPath()+".done").exists(),"never mark interrupted as completed");
   ok(TrackFile.read(f,(a,b,c,d)->{})==2,"read recovered track");
   ByteArrayOutputStream gpx=new ByteArrayOutputStream();ok(TrackFile.gpx(f,gpx)==2,"GPX export after recovery");
   reject(f); // already recovered
   new File(f.getPath()+".recovered").delete();
   recovered=TrackRecovery.recover(f);ok(recovered.discardedBytes==0,"complete file unchanged");
   ok(Files.readString(f.toPath()).equals(original),"do not rewrite complete track");
   new File(f.getPath()+".recovered").delete();
   put(f,"37.0,127.0,5,1792120000000");reject(f);ok(Files.readString(f.toPath()).equals("37.0,127.0,5,1792120000000"),"no complete row remains untouched");
   put(f,original+"oops\n");reject(f);ok(Files.readString(f.toPath()).equals(original+"oops\n"),"malformed committed row untouched");
   put(f,original+"partial");new File(f.getPath()+".done").createNewFile();reject(f);new File(f.getPath()+".done").delete();
   new File(f.getPath()+".imported").createNewFile();reject(f);new File(f.getPath()+".imported").delete();
   File wrong=new File(dir,"../not-a-track.txt");reject(wrong);
   File empty=new File(dir,"1792122693079-aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa.track");put(empty,"");reject(empty);
   ok(!new File(f.getPath()+".recovered").exists(),"failed recovery must not mark complete");
   ok(Arrays.stream(Objects.requireNonNull(dir.listFiles())).noneMatch(x->x.getName().startsWith(".ride-recovery-")),"temporary file cleanup");
   System.out.println("PASS GPS recovery: "+checks+" checks");
  }finally{clean(dir);}
 }
}
