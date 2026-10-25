package com.motohub.next;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
public final class RestoreRideGateTest {
 static int assertions=0;
 static void check(boolean ok,String message){assertions++;if(!ok)throw new AssertionError(message);}
 public static void main(String[] args)throws Exception {
  RestoreRideGate.end();
  check(!RestoreRideGate.blocked(false,false),"idle restore allowed");
  check(RestoreRideGate.blocked(true,false),"active gps blocks restore");
  check(RestoreRideGate.blocked(false,true),"armed obd blocks restore");
  check(RestoreRideGate.blocked(true,true),"dual state blocks restore");
  check(!RestoreRideGate.begin(true,false),"gps blocked at commit");
  check(!RestoreRideGate.begin(false,true),"obd blocked at commit");
  check(!RestoreRideGate.isRestoring(),"denied restore never latches");
  check(RestoreRideGate.begin(false,false),"idle restore transaction starts");
  check(RestoreRideGate.blocked(false,false),"concurrent restore blocked");
  check(!RestoreRideGate.begin(false,false),"second restore denied");
  RestoreRideGate.end();
  check(!RestoreRideGate.isRestoring(),"gate released after finish");
  check(RestoreRideGate.beginRideStart(),"ride start reserved when idle");
  check(!RestoreRideGate.begin(false,false),"restore denied while ride initialization is in progress");
  check(RestoreRideGate.blocked(false,false),"ride start reserves gate against restore");
  RestoreRideGate.endRideStart();
  check(!RestoreRideGate.blocked(false,false),"start reservation released");
  check(RestoreRideGate.begin(false,false),"restore transaction can start");
  check(!RestoreRideGate.beginRideStart(),"new ride rejected during restore");
  RestoreRideGate.end();
  check(RestoreRideGate.beginRideStart(),"future ride can start after restore");
  RestoreRideGate.endRideStart();
  // Two simultaneous restore attempts may never both enter.
  CountDownLatch start=new CountDownLatch(1),finish=new CountDownLatch(1);
  AtomicInteger successes=new AtomicInteger();
  Runnable race=()->{try{start.await();if(RestoreRideGate.begin(false,false)){successes.incrementAndGet();finish.await();RestoreRideGate.end();}}catch(InterruptedException e){Thread.currentThread().interrupt();}};
  Thread first=new Thread(race),second=new Thread(race);first.start();second.start();start.countDown();
  for(int i=0;i<100&&successes.get()==0;i++)Thread.sleep(2);
  check(successes.get()==1,"one of two concurrent restores entered");
  check(RestoreRideGate.blocked(false,false),"no starts during restore transaction");
  finish.countDown();first.join(2000);second.join(2000);
  check(!first.isAlive()&&!second.isAlive(),"workers finished");
  check(!RestoreRideGate.isRestoring(),"no stale gate after concurrent test");
  check(RestoreRideGate.begin(false,false),"future restore can start");
  RestoreRideGate.end();
  System.out.println(assertions+" restore ride gate assertions passed");
 }
}
