package com.motohub.next;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.UUID;

public final class RideReportScopeTest {
 static int count;
 static void ok(boolean value,String reason){count++;if(!value)throw new AssertionError(reason);}
 public static void main(String[] args)throws Exception {
  File root=Files.createTempDirectory("moto-report-scope").toFile();
  try {
   long oldRide=1_700_000_000_000L;
   File track=new File(TrackFile.directory(root),System.currentTimeMillis()+"-"+UUID.randomUUID()+".track");
   try(FileOutputStream out=new FileOutputStream(track)){
    out.write(TrackFile.point(37,127,5,oldRide).getBytes(StandardCharsets.UTF_8));
   }
   String bike=UUID.randomUUID().toString(),other=UUID.randomUUID().toString();
   ok(RideReportScope.include(track,null),"all includes unassigned");
   ok(RideReportScope.include(track,""),"unassigned includes unassigned");
   ok(!RideReportScope.include(track,bike),"specific bike excludes unassigned");
   RideBikeBinding.write(track,bike);
   ok(RideReportScope.include(track,bike),"specific bike includes matching ride");
   ok(!RideReportScope.include(track,other),"different bike excluded");
   ok(!RideReportScope.include(track,""),"assigned ride not unassigned");
   ok(RideReportScope.include(track,null),"all includes assigned");
   ok(RideMonthlyReport.timestamp(track)==System.currentTimeMillis() || Math.abs(RideMonthlyReport.timestamp(track)-System.currentTimeMillis())<5000,"native track timestamp");
   File imported=new File(TrackFile.directory(root),System.currentTimeMillis()+"-"+UUID.randomUUID()+".track");
   Files.writeString(imported.toPath(),TrackFile.point(37,127,5,oldRide));
   new File(imported.getPath()+".imported").createNewFile();
   ok(RideMonthlyReport.timestamp(imported)==oldRide,"imported GPX uses recorded time");
   ok(!RideMonthlyReport.recent(imported,System.currentTimeMillis(),60),"old imported GPX not counted as recent");
   new File(imported.getPath()+".imported").delete();
   ok(RideMonthlyReport.recent(imported,System.currentTimeMillis(),60),"native filename date used without import marker");
   new File(track.getPath()+".bike").delete();
   Files.writeString(new File(track.getPath()+".bike").toPath(),"corrupt");
   boolean rejected=false;
   try{RideReportScope.include(track,bike);}catch(IOException expected){rejected=true;}
   ok(rejected,"corrupt binding is not silently attributed to bike");
   ok(RideReportScope.include(track,null),"all scope does not depend on bike metadata");
   rejected=false;
   try{RideReportScope.include(track,"not-a-uuid");}catch(IOException expected){rejected=true;}
   ok(rejected,"invalid selection rejected");
   System.out.println("PASS: "+count+" report scope/GPX date assertions");
  }finally{delete(root);}
 }
 static void delete(File file){if(file.isDirectory()){File[] files=file.listFiles();if(files!=null)for(File f:files)delete(f);}file.delete();}
}
