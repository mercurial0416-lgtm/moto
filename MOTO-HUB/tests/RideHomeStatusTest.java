package com.motohub.next;

import java.io.File;
import java.nio.file.Files;
import java.util.UUID;

public final class RideHomeStatusTest {
 static int checks;
 static void check(boolean ok,String label){checks++;if(!ok)throw new AssertionError(label);}
 static File track(File dir,long at)throws Exception{
  File f=new File(dir,at+"-"+UUID.randomUUID()+".track");
  Files.write(f.toPath(),new byte[0]);return f;
 }
 public static void main(String[] args)throws Exception{
  File root=Files.createTempDirectory("moto-home-index").toFile();
  File dir=TrackFile.directory(root);
  check(RideHomeStatus.interrupted(root)==0,"empty");
  File bad=track(dir,1760000000000L);
  File done=track(dir,1760000000001L);
  File recovered=track(dir,1760000000002L);
  File imported=track(dir,1760000000003L);
  new File(done+".done").createNewFile();
  new File(recovered+".recovered").createNewFile();
  new File(imported+".imported").createNewFile();
  check(RideHomeStatus.interrupted(root)==1,"completed sidecars excluded");
  for(int i=0;i<1200;i++)track(dir,1761000000000L+i);
  check(RideHomeStatus.interrupted(root)==1201,"streamed 1200+ records without sorting");
  File noTrack=new File(dir,"not-a-gps-file.track");
  Files.write(noTrack.toPath(),new byte[0]);
  check(RideHomeStatus.interrupted(root)==1201,"reject invalid track names");
  try{
   File link=new File(dir,1762000000000L+"-"+UUID.randomUUID()+".track");
   Files.createSymbolicLink(link.toPath(),bad.toPath());
   check(RideHomeStatus.interrupted(root)==1201,"do not follow GPS symlinks");
  }catch(UnsupportedOperationException|SecurityException ignored){}
  new File(bad+".done").createNewFile();
  check(RideHomeStatus.interrupted(root)==1200,"finished trip no longer needs recovery");
  System.out.println("PASS RideHomeStatusTest "+checks);
 }
}