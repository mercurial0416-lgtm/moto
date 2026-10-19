package com.motohub.next;
import java.io.File;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
public final class TrackCompletionTest {
 static int checks=0;
 static void ok(boolean v,String m){checks++;if(!v)throw new AssertionError(m);}
 static boolean fails(File file,int points){try{TrackCompletion.mark(file,points);return false;}catch(Exception e){return true;}}
 public static void main(String[] args)throws Exception{
  File dir=Files.createTempDirectory("moto-track-completion").toFile();
  File f=new File(dir,"1792122693079-12345678-1234-1234-1234-123456789abc.track");
  String points=TrackFile.point(37.0,127.0,5,1792120000000L)+TrackFile.point(37.1,127.1,5,1792120010000L);
  try{
   Files.writeString(f.toPath(),points+"37.3,",StandardCharsets.UTF_8);
   ok(fails(f,2),"partial tail must fail");
   ok(!new File(f.getPath()+".done").exists(),"no false .done on partial tail");
   ok(Files.readString(f.toPath()).equals(points+"37.3,"),"original partial data preserved");
   Files.writeString(f.toPath(),points,StandardCharsets.UTF_8);
   ok(fails(f,3),"point mismatch must fail");
   ok(!new File(f.getPath()+".done").exists(),"no false .done on point mismatch");
   TrackCompletion.mark(f,2);
   ok(new File(f.getPath()+".done").exists(),"valid complete ride has marker");
   ok(Files.readString(f.toPath()).equals(points),"valid original unchanged");
   ok(fails(f,2),"duplicate marker rejected");
   System.out.println("PASS GPS completion: "+checks+" checks");
  }finally{new File(f.getPath()+".done").delete();f.delete();dir.delete();}
 }
}
