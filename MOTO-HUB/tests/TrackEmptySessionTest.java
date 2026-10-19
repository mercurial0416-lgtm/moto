package com.motohub.next;
import java.io.*;
import java.nio.file.*;
public final class TrackEmptySessionTest {
 static int n=0;
 static void check(boolean yes,String why){n++;if(!yes)throw new AssertionError(why);}
 public static void main(String[] args)throws Exception{
  Path dir=Files.createTempDirectory("moto-empty-track");
  try{
   File empty=dir.resolve("empty.track").toFile();Files.createFile(empty.toPath());
   File binding=new File(empty.getPath()+".bike");Files.write(binding.toPath(),"test".getBytes("UTF-8"));
   check(TrackFile.discardEmptySession(empty),"empty session removed");
   check(!empty.exists()&&!binding.exists(),"orphan binding removed");
   File partial=dir.resolve("partial.track").toFile();Files.write(partial.toPath(),"37,127,4,100".getBytes("UTF-8"));
   check(!TrackFile.discardEmptySession(partial),"partial uncommitted GPS preserved");
   check(partial.exists(),"partial file remains for recovery");
   File completed=dir.resolve("done.track").toFile();Files.createFile(completed.toPath());
   File marker=new File(completed.getPath()+".done");Files.createFile(marker.toPath());
   check(!TrackFile.discardEmptySession(completed),"completed marker blocks deletion");
   check(completed.exists(),"completed file preserved");
   check(!TrackFile.discardEmptySession(null),"null safe");
  }finally{try(java.util.stream.Stream<Path> files=Files.list(dir)){files.forEach(p->{try{Files.deleteIfExists(p);}catch(IOException e){throw new RuntimeException(e);}});}Files.deleteIfExists(dir);}
  System.out.println("PASS: "+n+" empty-session recovery assertions");
 }
}
