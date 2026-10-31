package com.motohub.next;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

/** Files appearing to be GPS tracks must not escape the app's private ride directory. */
public final class Ux7TrackSafetyTest {
 static int checked=0;
 static void ok(boolean yes,String message){checked++;if(!yes)throw new AssertionError(message);}
 static boolean throwsIo(Checked action){
  try{action.run();return false;}catch(IOException expected){return true;}catch(Exception unexpected){throw new AssertionError(unexpected);}
 }
 interface Checked {void run()throws Exception;}
 public static void main(String[] args)throws Exception {
  File root=Files.createTempDirectory("moto-ride-safe-").toFile();
  File external=File.createTempFile("moto-outside-track-", ".track");
  String name="1792490000000-12345678-1234-1234-1234-123456789abc.track";
  try {
   File rides=TrackFile.directory(root);
   File normal=new File(rides,name);
   Files.writeString(normal.toPath(),"37.1,127.1,5.0,1792490000000\n",StandardCharsets.UTF_8);
   ok(TrackFile.read(normal,(lat,lon,acc,time)->{})==1,"ordinary track remains readable");
   ok(TrackFile.tracks(root).size()==1,"ordinary track remains listed");
   ok(throwsIo(()->TrackFile.read(null,(lat,lon,acc,time)->{})),"null track rejected");
   File empty=new File(rides,"1792490000001-12345678-1234-1234-1234-123456789abc.track");
   ok(empty.createNewFile(),"create empty track");
   ok(TrackFile.discardEmptySession(empty),"discard only real empty track");
   ok(normal.isFile(),"real GPS record must survive test");
   try {
    File linked=new File(rides,"1792490000002-12345678-1234-1234-1234-123456789abc.track");
    Files.createSymbolicLink(linked.toPath(),external.toPath());
    ok(throwsIo(()->TrackFile.tracks(root)),"linked external track makes history scan fail safely");
    ok(throwsIo(()->TrackFile.read(linked,(lat,lon,acc,time)->{})),"linked external track never parsed");
    ok(!TrackFile.discardEmptySession(linked),"linked external file not removed");
    ok(external.exists(),"external data preserved");
    Files.delete(linked.toPath());
    ok(TrackFile.tracks(root).size()==1,"ordinary GPS still visible after unsafe link removed");
   }catch(UnsupportedOperationException|SecurityException e){ /* Skip unsupported filesystem links. */ }
   File linkedRoot=Files.createTempDirectory("moto-ride-linked-").toFile();
   try{
    Path link=new File(linkedRoot,"rides").toPath();
    try{
     Files.createSymbolicLink(link,rides.toPath());
     ok(throwsIo(()->TrackFile.directory(linkedRoot)),"linked rides directory rejected");
    }catch(UnsupportedOperationException|SecurityException e){ /* Not supported on all hosts. */ }
   }finally{
    Files.deleteIfExists(new File(linkedRoot,"rides").toPath());
    linkedRoot.delete();
   }
  }finally{
   external.delete();
   File rides=new File(root,"rides");
   File[] contents=rides.listFiles();
   if(contents!=null)for(File f:contents)f.delete();
   rides.delete();root.delete();
  }
  System.out.println("PASS UX7 GPS original-file protection: "+checked+" checks");
 }
}
