package com.motohub.next;
import java.io.*;
import java.nio.file.*;
import java.util.*;
public final class BackupReadinessTest {
 static int n;
 static void ok(boolean v,String msg){n++;if(!v)throw new AssertionError(msg);}
 public static void main(String[] args)throws Exception{
  File dir=Files.createTempDirectory("moto-readiness").toFile();File rides=new File(dir,"rides");rides.mkdir();
  try{
   String a="1792122693079-12345678-1234-1234-1234-123456789abc.track";
   String b="1792122693080-12345678-1234-1234-1234-123456789abc.track";
   Files.writeString(new File(rides,a).toPath(),"37,127,5,1792120000000\n");
   new File(rides,b).createNewFile();
   new File(rides,a+".done").createNewFile();
   Files.writeString(new File(dir,"garage-photo-12345678-1234-1234-1234-123456789abc.jpg").toPath(),"JPGTEST");
   BackupReadiness.Summary s=BackupReadiness.inspect(dir);
   ok(s.rides==2,"counts GPS files");ok(s.photos==1,"counts images");
   ok(s.emptyRides==1,"empty GPS reported");ok(s.unfinishedRides==1,"unfinished counted");
   ok(s.withinBasicLimits(),"basic limits");ok(s.message().contains("GPS 원본 2건"),"Korean readable UI");
   ok(!new BackupReadiness.Summary(10001,0,0,0,0,0).withinBasicLimits(),"limit reported");
   ok(!new BackupReadiness.Summary(0,0,0,0,1,0).withinBasicLimits(),"oversized reported");
   ok(!new BackupReadiness.Summary(1,0,1,0,0,1,0).withinBasicLimits(),"completed empty GPS should be refused");
   new File(rides,b+".done").createNewFile();
   BackupReadiness.Summary broken=BackupReadiness.inspect(dir);
   ok(broken.completedEmptyRides==1,"completed empty GPS detected");
   ok(!broken.withinBasicLimits(),"misleading preflight success prevented");
   ok(broken.message().contains("내용 없는 GPS 기록 1개"),"message explains repair issue");
   // Symlinked external photo/track data must never silently pass a backup preflight.
   Path outside=Files.createTempFile("moto-user-outside-", ".jpg");
   try{
    Path linked=new File(dir,"garage-photo-aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa.jpg").toPath();
    try{
     Files.createSymbolicLink(linked,outside);
     BackupReadiness.Summary unsafe=BackupReadiness.inspect(dir);
     ok(!unsafe.withinBasicLimits(),"external symlink photo cannot pass");
     ok(unsafe.oversizedFiles>0,"unsafe file surfaced in warning");
     Files.delete(linked);
    }catch(UnsupportedOperationException|SecurityException e){ /* Filesystem may not permit symlinks */ }
    Path trackLink=new File(rides,"1792122693081-12345678-1234-1234-1234-123456789abc.track").toPath();
    try{
     Files.createSymbolicLink(trackLink,outside);
     ok(!BackupReadiness.inspect(dir).withinBasicLimits(),"symlinked GPS track refused");
     Files.delete(trackLink);
    }catch(UnsupportedOperationException|SecurityException e){ /* Environment limitation */ }
    Path linkedDir=new File(dir,"rides-other").toPath();
    // A symlink at the exact rides folder would make user GPS escape app storage;
    // emulate by inspecting a separate app root with linked rides folder.
    File fake=Files.createTempDirectory("moto-linked-rides-").toFile();
    try{
     Path linkedRides=new File(fake,"rides").toPath();
     try{
      Files.createSymbolicLink(linkedRides,rides.toPath());
      try{BackupReadiness.inspect(fake);throw new AssertionError("linked GPS directory accepted");}
      catch(IOException expected){n++;}
     }catch(UnsupportedOperationException|SecurityException e){ /* Environment limitation */ }
    }finally{
     Path child=new File(fake,"rides").toPath();
     Files.deleteIfExists(child);fake.delete();
    }
   }finally{Files.deleteIfExists(outside);}
   ok(!new BackupReadiness.Summary(1,0,0,0,0,-1).withinBasicLimits(),"negative byte totals invalid");
   System.out.println("PASS DF backup preview: "+n+" checks");
  }finally{
   File[] contents=rides.listFiles();if(contents!=null)for(File f:contents)f.delete();rides.delete();
   File[] remaining=dir.listFiles();if(remaining!=null)for(File f:remaining)f.delete();dir.delete();
  }
 }
}
