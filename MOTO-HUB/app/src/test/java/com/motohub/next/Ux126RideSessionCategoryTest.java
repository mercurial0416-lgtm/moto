package com.motohub.next;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import static org.junit.Assert.*;

/** One multi-part GPS ride has one deliberate label, regardless of segment opened. */
public final class Ux126RideSessionCategoryTest {
 static File create(File rides,long now,String uuid)throws Exception{
  File track=new File(rides,now+"-"+uuid+".track");
  Files.write(track.toPath(),(TrackFile.point(37.0,127.0,3,now)+
   TrackFile.point(37.001,127.001,3,now+2000)).getBytes(StandardCharsets.UTF_8));
  TrackCompletion.mark(track,2);
  return track;
 }
 static void cleanup(File dir)throws Exception{
  if(dir.isDirectory()){
   File[] children=dir.listFiles();
   if(children!=null)for(File child:children)cleanup(child);
  }
  Files.deleteIfExists(dir.toPath());
 }
 @Test public void everyCompletedSegmentGetsSameCategoryAndAutoClears()throws Exception{
  File base=Files.createTempDirectory("moto-category-session").toFile();
  try{
   File rides=TrackFile.directory(base);
   String session=UUID.randomUUID().toString();
   long now=System.currentTimeMillis()-600_000;
   File a=create(rides,now,session),b=create(rides,now+4000,session);
   File other=create(rides,now+8000,UUID.randomUUID().toString());
   RideCategory.write(other,RideCategory.TOUR);
   assertEquals(2,RideSegmentGroup.members(base,a.getName()).size());
   RideSessionCategory.write(base,b,RideCategory.COMMUTE);
   assertEquals(RideCategory.COMMUTE,RideCategory.read(a));
   assertEquals(RideCategory.COMMUTE,RideCategory.read(b));
   assertEquals(RideCategory.TOUR,RideCategory.read(other));
   RideSessionCategory.write(base,a,RideCategory.AUTO);
   assertEquals(RideCategory.AUTO,RideCategory.read(a));
   assertEquals(RideCategory.AUTO,RideCategory.read(b));
   assertFalse(new File(a+".category").exists());
   assertFalse(new File(b+".category").exists());
  }finally{cleanup(base);}
 }
 @Test public void injectedFailureRollsBackExactPreviousSidecars()throws Exception{
  File base=Files.createTempDirectory("moto-category-rollback").toFile();
  try{
   File rides=TrackFile.directory(base);
   String session=UUID.randomUUID().toString();
   long now=System.currentTimeMillis()-900_000;
   File a=create(rides,now,session),b=create(rides,now+4000,session);
   RideCategory.write(a,RideCategory.DAILY);
   byte[] old=Files.readAllBytes(new File(a+".category").toPath());
   AtomicInteger count=new AtomicInteger();
   try{
    RideSessionCategory.write(base,a,RideCategory.WINDING,()->{
     if(count.incrementAndGet()==1)throw new IllegalStateException("simulated IO failure after first commit");
    });
    fail("must roll back partial session writes");
   }catch(IOException expected){assertTrue(expected.getMessage().contains("되돌립니다"));}
   assertArrayEquals(old,Files.readAllBytes(new File(a+".category").toPath()));
   assertFalse("previously absent tag must stay absent",new File(b+".category").exists());
   assertEquals(RideCategory.DAILY,RideCategory.read(a));
   assertEquals(RideCategory.AUTO,RideCategory.read(b));
  }finally{cleanup(base);}
 }
 @Test public void incompleteRideNeverGetsSessionWrite()throws Exception{
  File base=Files.createTempDirectory("moto-category-incomplete").toFile();
  try{
   File rides=TrackFile.directory(base);
   long now=System.currentTimeMillis()-80_000;
   String session=UUID.randomUUID().toString();
   File complete=create(rides,now,session);
   File incomplete=new File(rides,(now+4000)+"-"+session+".track");
   Files.write(incomplete.toPath(),TrackFile.point(37,127,3,now+4000).getBytes(StandardCharsets.UTF_8));
   try{RideSessionCategory.write(base,incomplete,RideCategory.COMMUTE);fail("incomplete must fail");}
   catch(IOException expected){}
   assertEquals(RideCategory.AUTO,RideCategory.read(complete));
  }finally{cleanup(base);}
 }
}
