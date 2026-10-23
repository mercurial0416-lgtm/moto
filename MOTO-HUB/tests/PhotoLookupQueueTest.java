package com.motohub.next;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** Two active lookups and six waiting lookups; the 9th shows a retry UI instead. */
public final class PhotoLookupQueueTest {
 static void require(boolean value,String title){if(!value)throw new AssertionError(title);}
 public static void main(String[] args)throws Exception{
  CountDownLatch twoRunning=new CountDownLatch(2),free=new CountDownLatch(1),done=new CountDownLatch(8);
  try{
   for(int i=0;i<2;i++)require(PhotoLookupQueue.submit(()->{
    twoRunning.countDown();
    try{free.await(10,TimeUnit.SECONDS);}catch(InterruptedException ignored){Thread.currentThread().interrupt();}
    done.countDown();
   }),"initial worker");
   require(twoRunning.await(5,TimeUnit.SECONDS),"two workers started");
   for(int i=0;i<6;i++)require(PhotoLookupQueue.submit(done::countDown),"waiting card "+i);
   require(!PhotoLookupQueue.submit(()->{throw new AssertionError("overflow ran");}),"over capacity must reject");
  }finally{free.countDown();}
  require(done.await(10,TimeUnit.SECONDS),"queued work drained");
  require(PhotoLookupQueue.submit(()->{}),"queue accepts new work once drained");
  System.out.println("PASS bounded Commons photo lookup worker queue");
 }
}
