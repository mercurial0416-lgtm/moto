package com.motohub.next;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
public final class BackupExportInterlockTest {
 private static int checks;
 private static void ok(boolean condition,String why){checks++;if(!condition)throw new AssertionError(why);}
 public static void main(String[] args)throws Exception{
  RestoreRideGate.endExport();RestoreRideGate.end();
  ok(RestoreRideGate.beginExport(false,false),"idle reservation succeeds");
  ok(RestoreRideGate.isExporting(),"export locked");
  ok(!RestoreRideGate.beginRideStart(),"ride cannot begin during ZIP export");
  ok(!RestoreRideGate.begin(false,false),"restore cannot begin during ZIP export");
  ok(!RestoreRideGate.beginExport(false,false),"two concurrent backups rejected");
  ok(!UpdateSafety.canInstall(false,false,RestoreRideGate.isExporting(),false),"APK update blocked during archive export");
  RestoreRideGate.endExport();
  ok(RestoreRideGate.beginRideStart(),"ride can begin after backup");
  ok(!RestoreRideGate.beginExport(false,false),"initializing GPS preempts backup");
  RestoreRideGate.endRideStart();
  ok(!RestoreRideGate.beginExport(true,false),"running GPS blocks export");
  ok(!RestoreRideGate.beginExport(false,true),"OBD armed blocks export");
  ok(!UpdateSafety.canInstall(false,true,false,false),"armed OBD blocks update");
  ok(UpdateSafety.canInstall(false,false,false,false),"idle update remains possible");
  ok(RestoreRideGate.begin(false,false),"restore allowed after backup");
  ok(!RestoreRideGate.beginExport(false,false),"restoration blocks backup");
  RestoreRideGate.end();
  CountDownLatch go=new CountDownLatch(1),stop=new CountDownLatch(1);AtomicInteger wins=new AtomicInteger();
  Runnable compete=()->{try{go.await();if(RestoreRideGate.beginExport(false,false)){wins.incrementAndGet();stop.await();RestoreRideGate.endExport();}}catch(InterruptedException ex){Thread.currentThread().interrupt();}};
  Thread a=new Thread(compete),b=new Thread(compete);a.start();b.start();go.countDown();
  for(int i=0;i<500&&wins.get()==0;i++)Thread.sleep(2);
  ok(wins.get()==1,"exactly one competing export owns reservation");
  stop.countDown();a.join(2000);b.join(2000);
  ok(!RestoreRideGate.isExporting(),"no stranded export lock");
  System.out.println("PASS DE export/ride/restore interlock: "+checks+" checks");
 }
}
