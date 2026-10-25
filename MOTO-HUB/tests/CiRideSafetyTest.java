package com.motohub.next;
import java.io.*;
import java.nio.file.*;

public final class CiRideSafetyTest {
 static int count;
 static void yes(boolean actual,String name){count++;if(!actual)throw new AssertionError(name);}
 static String point(long t){return TrackFile.point(37.1,127.2,5,t);}
 static void save(File f,String data)throws Exception{Files.writeString(f.toPath(),data);}
 public static void main(String[] args)throws Exception{
  File dir=Files.createTempDirectory("moto-ci-ride").toFile();
  try{
   File rides=TrackFile.directory(dir);
   File first=new File(rides,"1792122693079-12345678-1234-1234-1234-123456789abc.track");
   File newer=new File(rides,"1792122693080-aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa.track");
   File bad=new File(rides,"1792122693081-bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb.track");
   save(first,point(1792120000000L));
   save(newer,point(1792120001000L)+"unfinished-row");
   save(bad,"invalid\n");
   yes(InterruptedRideNotice.latest(dir,"",false).equals(newer),"newest valid interrupted ride");
   yes(InterruptedRideNotice.latest(dir,newer.getName(),true).equals(first),"exclude active ride");
   new File(newer.getPath()+".done").createNewFile();
   yes(InterruptedRideNotice.latest(dir,"",false).equals(first),"exclude completed ride");
   new File(first.getPath()+".recovered").createNewFile();
   yes(InterruptedRideNotice.latest(dir,"",false)==null,"exclude recovered and bad ride");
   yes(Files.readString(newer.toPath()).endsWith("unfinished-row"),"scanner never mutates originals");
   // Regression: 50 newer *completed* rides must not hide a real interrupted ride.
   // These files are cheap to skip: none may consume the 40-file validation budget.
   for(int i=0;i<50;i++){
    String name=String.format("179212270%04d-cccccccc-cccc-cccc-cccc-%012d.track",i,i);
    File done=new File(rides,name);
    save(done,point(1792120010000L+i));
    yes(new File(done.getPath()+".done").createNewFile(),"create completion marker "+i);
   }
   yes(InterruptedRideNotice.latest(dir,"",false)==null,"completed rides do not reveal malformed tracks");
   File older=new File(rides,"1792122693070-dddddddd-dddd-dddd-dddd-dddddddddddd.track");
   save(older,point(1792120002000L)+"partial-crash");
   yes(InterruptedRideNotice.latest(dir,"",false).equals(older),"older interrupted ride survives 50 newer completions");
   yes(Files.readString(older.toPath()).endsWith("partial-crash"),"interrupted scan never truncates source");

   RideNotificationRate rate=new RideNotificationRate();
   yes(rate.shouldNotify(0),"first alert");
   yes(!rate.shouldNotify(3000),"coalesce repeat");
   yes(!rate.shouldNotify(9999),"hold before interval");
   yes(rate.shouldNotify(10000),"notify when due");
   yes(rate.shouldNotify(500),"reset on clock reversal");
   rate.reset();yes(rate.shouldNotify(501),"reset on new ride");
   yes(ObdConnectionPolicy.MAX_RETRIES==8,"ignition-cycle retry budget");
   yes(ObdConnectionPolicy.shouldRetry(8,true,true),"last try in foreground");
   yes(!ObdConnectionPolicy.shouldRetry(9,true,true),"bounded retries");
   yes(!ObdConnectionPolicy.shouldRetry(1,false,true),"no reconnect without arm");
   yes(ObdConnectionPolicy.delayMs(1)==1500L,"short first wait");
   yes(ObdConnectionPolicy.delayMs(3)==6000L,"progressive backoff");
   yes(ObdConnectionPolicy.delayMs(8)==20000L,"capped battery-friendly wait");
   System.out.println("PASS CI ride recovery, reconnect, notification: "+count+" checks");
  } finally {for(File f:TrackFile.directory(dir).listFiles())f.delete();TrackFile.directory(dir).delete();dir.delete();}
 }
}
