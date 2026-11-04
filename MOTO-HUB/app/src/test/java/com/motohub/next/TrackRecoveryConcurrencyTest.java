package com.motohub.next;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.UUID;
import java.util.concurrent.*;
import org.junit.Test;
import static org.junit.Assert.*;

public final class TrackRecoveryConcurrencyTest {
 File create(File dir)throws Exception{
  File f=new File(dir,"1700000000000-"+UUID.randomUUID()+".track");
  Files.write(f.toPath(),(TrackFile.point(37,127,4,1700000000000L)+TrackFile.point(37.001,127,4,1700000010000L)+"partial").getBytes(StandardCharsets.UTF_8));return f;
 }
 void clean(File dir)throws Exception{
  File[] entries=dir.listFiles();if(entries!=null)for(File f:entries)Files.deleteIfExists(f.toPath());Files.deleteIfExists(dir.toPath());
 }
 @Test public void failedRecoveryCannotDeleteACompletionMarkerCreatedByAnotherOperation()throws Exception{
  File dir=Files.createTempDirectory("moto-recovery-owner").toFile();
  try{
   File f=create(dir),marker=new File(f+".recovered");byte[] raw=Files.readAllBytes(f.toPath());
   try{TrackRecovery.recover(f,()->{try{Files.write(marker.toPath(),"other operation".getBytes(StandardCharsets.UTF_8));}catch(IOException e){throw new IllegalStateException(e);}});fail();}
   catch(IOException expected){}
   assertTrue(marker.isFile());assertEquals("other operation",new String(Files.readAllBytes(marker.toPath()),StandardCharsets.UTF_8));
   assertArrayEquals(raw,Files.readAllBytes(f.toPath()));
  }finally{clean(dir);}
 }
 @Test public void simultaneousRecoveryOfTheSameFileCannotUndoACompletedRecovery()throws Exception{
  File dir=Files.createTempDirectory("moto-recovery-concurrent").toFile();
  ExecutorService threads=Executors.newFixedThreadPool(2);CountDownLatch entered=new CountDownLatch(1),resume=new CountDownLatch(1);
  try{
   File f=create(dir);
   Future<TrackRecovery.Result> first=threads.submit(()->TrackRecovery.recover(f,()->{
    entered.countDown();try{if(!resume.await(8,TimeUnit.SECONDS))throw new IllegalStateException("test stalled");}catch(InterruptedException e){throw new IllegalStateException(e);}
   }));
   assertTrue(entered.await(2,TimeUnit.SECONDS));
   Future<TrackRecovery.Result> second=threads.submit(()->TrackRecovery.recover(f));
   try{second.get(2,TimeUnit.SECONDS);fail("second recovery must wait for the first transaction");}catch(TimeoutException expected){}
   resume.countDown();assertEquals(2,first.get(3,TimeUnit.SECONDS).points);
   try{second.get(3,TimeUnit.SECONDS);fail("completed track must not be recovered twice");}catch(ExecutionException expected){assertTrue(expected.getCause() instanceof IOException);}
   assertTrue(new File(f+".recovered").isFile());assertEquals(2,TrackFile.read(f,(a,b,c,d)->{}));
   assertTrue(new String(Files.readAllBytes(f.toPath()),StandardCharsets.UTF_8).endsWith("\n"));
  }finally{resume.countDown();threads.shutdownNow();threads.awaitTermination(3,TimeUnit.SECONDS);clean(dir);}
 }
}
