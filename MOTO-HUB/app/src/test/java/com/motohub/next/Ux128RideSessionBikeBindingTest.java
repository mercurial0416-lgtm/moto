package com.motohub.next;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import static org.junit.Assert.*;

/** ODO-safe and rollback-safe whole-session GPS bike binding. */
public final class Ux128RideSessionBikeBindingTest {
 static File create(File rides,long time,String uuid)throws Exception{
  File file=new File(rides,time+"-"+uuid+".track");
  Files.write(file.toPath(),(TrackFile.point(37,127,4,time)+
   TrackFile.point(37.001,127.001,4,time+1000)).getBytes(StandardCharsets.UTF_8));
  TrackCompletion.mark(file,2);return file;
 }
 static void clean(File dir)throws Exception{
  if(dir.isDirectory()){File[] list=dir.listFiles();if(list!=null)for(File f:list)clean(f);}
  Files.deleteIfExists(dir.toPath());
 }
 @Test public void allSectionsChangeAndOtherSessionRemainsUntouched()throws Exception{
  File root=Files.createTempDirectory("moto-v581-binding").toFile();
  try{
   File rides=TrackFile.directory(root);
   String id=UUID.randomUUID().toString(),oldBike=UUID.randomUUID().toString(),newBike=UUID.randomUUID().toString();
   long t=System.currentTimeMillis()-100_000;
   File a=create(rides,t,id),b=create(rides,t+3000,id);
   File other=create(rides,t+6000,UUID.randomUUID().toString());
   RideBikeBinding.write(a,oldBike);
   RideBikeBinding.write(other,oldBike);
   byte[] originalGps=Files.readAllBytes(a.toPath());
   RideSessionBikeBinding.write(root,b,newBike,Collections.emptySet());
   assertEquals(newBike,RideBikeBinding.readStrict(a));
   assertEquals(newBike,RideBikeBinding.readStrict(b));
   assertEquals(oldBike,RideBikeBinding.readStrict(other));
   assertArrayEquals(originalGps,Files.readAllBytes(a.toPath()));
  }finally{clean(root);}
 }
 @Test public void appliedSiblingBlocksAllChanges()throws Exception{
  File root=Files.createTempDirectory("moto-v581-applied").toFile();
  try{
   File rides=TrackFile.directory(root);String id=UUID.randomUUID().toString();
   long t=System.currentTimeMillis()-100_000;
   File a=create(rides,t,id),b=create(rides,t+3000,id);
   String oldBike=UUID.randomUUID().toString();
   RideBikeBinding.write(a,oldBike);
   try{RideSessionBikeBinding.write(root,b,UUID.randomUUID().toString(),Collections.singleton(a.getName()));
    fail("applied sibling must block rebind");}
   catch(IOException expected){assertTrue(expected.getMessage().contains("반영"));}
   assertEquals(oldBike,RideBikeBinding.readStrict(a));
   assertEquals("",RideBikeBinding.readStrict(b));
  }finally{clean(root);}
 }
 @Test public void rollbackRestoresPresentAndMissingSidecars()throws Exception{
  File root=Files.createTempDirectory("moto-v581-rollback").toFile();
  try{
   File rides=TrackFile.directory(root);String id=UUID.randomUUID().toString();
   long t=System.currentTimeMillis()-100_000;
   File a=create(rides,t,id),b=create(rides,t+3000,id);
   String previous=UUID.randomUUID().toString();
   RideBikeBinding.write(a,previous);
   byte[] saved=Files.readAllBytes(new File(a+".bike").toPath());
   AtomicInteger writes=new AtomicInteger();
   try{
    RideSessionBikeBinding.write(root,a,UUID.randomUUID().toString(),Collections.emptySet(),()->{
     if(writes.incrementAndGet()==1)throw new IllegalStateException("simulate failure");
    });fail("must roll back");
   }catch(IOException expected){assertTrue(expected.getMessage().contains("복원"));}
   assertArrayEquals(saved,Files.readAllBytes(new File(a+".bike").toPath()));
   assertFalse(new File(b+".bike").exists());
  }finally{clean(root);}
 }
 @Test public void unfinishedSiblingPreventsRebindAndLateArrivalRollsBack()throws Exception{
  File root=Files.createTempDirectory("moto-v581-interrupted").toFile();
  try{
   File rides=TrackFile.directory(root);String id=UUID.randomUUID().toString();
   long t=System.currentTimeMillis()-100_000;
   File a=create(rides,t,id),b=create(rides,t+3000,id);
   File unfinished=new File(rides,(t+6000)+"-"+id+".track");
   Files.write(unfinished.toPath(),TrackFile.point(37,127,5,t+6000).getBytes(StandardCharsets.UTF_8));
   try{RideSessionBikeBinding.write(root,a,UUID.randomUUID().toString(),Collections.emptySet());
    fail("unfinished must block");}catch(IOException expected){}
   assertFalse(new File(a+".bike").exists());
   Files.deleteIfExists(unfinished.toPath());
   AtomicInteger writes=new AtomicInteger();
   try{
    RideSessionBikeBinding.write(root,a,UUID.randomUUID().toString(),Collections.emptySet(),()->{
     if(writes.incrementAndGet()==1)
      try{Files.write(unfinished.toPath(),TrackFile.point(37,127,5,t+6000).getBytes(StandardCharsets.UTF_8));}
      catch(IOException e){throw new IllegalStateException(e);}
    });fail("late arrival must invalidate");
   }catch(IOException expected){assertTrue(expected.getMessage().contains("복원"));}
   assertEquals("",RideBikeBinding.readStrict(a));
   assertEquals("",RideBikeBinding.readStrict(b));
  }finally{clean(root);}
 }
}
