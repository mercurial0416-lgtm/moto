package com.motohub.next;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/** Bound automatically retrieved Commons photos to two HTTP workers and six waiting cards. */
final class PhotoLookupQueue {
 private static final ThreadPoolExecutor EXECUTOR=new ThreadPoolExecutor(
  2,2,0L,TimeUnit.MILLISECONDS,new ArrayBlockingQueue<Runnable>(6),r->{
   Thread t=new Thread(r,"moto-model-photo");t.setDaemon(true);return t;
  },new ThreadPoolExecutor.AbortPolicy());
 private PhotoLookupQueue(){}
 /** Never block the UI or start an unlimited number of network threads. */
 static boolean submit(Runnable task){
  if(task==null)return false;
  try{EXECUTOR.execute(task);return true;}
  catch(RejectedExecutionException full){return false;}
 }
}
