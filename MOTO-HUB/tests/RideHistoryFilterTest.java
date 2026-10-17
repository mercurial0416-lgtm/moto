package com.motohub.next;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

public final class RideHistoryFilterTest {
 static int assertions;
 static void check(boolean condition,String message){assertions++;if(!condition)throw new AssertionError(message);}
 static File make(File dir,String name)throws Exception{File f=new File(dir,name);check(f.createNewFile(),"fixture "+name);return f;}
 public static void main(String[] args)throws Exception{
  File dir=Files.createTempDirectory("moto-ride-filter-").toFile();
  try{
   File complete=make(dir,"100-complete.track");make(dir,"100-complete.track.done");
   File imported=make(dir,"101-imported.track");make(dir,"101-imported.track.imported");
   File recovered=make(dir,"102-recovered.track");make(dir,"102-recovered.track.recovered");
   File interrupted=make(dir,"103-interrupted.track");
   File active=make(dir,"104-active.track");
   File mixed=make(dir,"105-mixed.track");make(dir,"105-mixed.track.done");make(dir,"105-mixed.track.imported");
   List<File> rides=Arrays.asList(complete,imported,recovered,interrupted,active,mixed);
   check(RideHistoryFilter.normalize(null).equals("전체"),"null fallback");
   check(RideHistoryFilter.normalize("invalid").equals("전체"),"unknown fallback");
   check(RideHistoryFilter.normalize("GPX").equals("GPX"),"valid selection");
   check(RideHistoryFilter.restore("GPX",null).equals("GPX"),"process restart retains filter");
   check(RideHistoryFilter.restore("GPX","완료").equals("완료"),"rotation overrides stored filter");
   check(RideHistoryFilter.restore("완료","전체").equals("전체"),"rotation can restore all");
   check(RideHistoryFilter.restore("bad",null).equals("전체"),"invalid stored filter shows all");
   check(RideHistoryFilter.restore("GPX","bad").equals("전체"),"invalid rotation filter shows all");
   check(RideHistoryFilter.restore(null,null).equals("전체"),"new install shows all");
   check(RideHistoryFilter.status(active,active.getName(),true).equals("기록 중"),"live status");
   check(RideHistoryFilter.status(active,active.getName(),false).equals("중단"),"inactive no false live");
   check(RideHistoryFilter.status(complete,"",false).equals("완료"),"completed status");
   check(RideHistoryFilter.status(imported,"",false).equals("GPX"),"imported status");
   check(RideHistoryFilter.status(recovered,"",false).equals("복구"),"recovered status");
   check(RideHistoryFilter.status(interrupted,"",false).equals("중단"),"interrupted status");
   check(RideHistoryFilter.status(mixed,"",false).equals("GPX"),"marker precedence");
   check(RideHistoryFilter.counts(rides,active.getName(),true).get("전체")==6,"single pass count all");
   check(RideHistoryFilter.counts(rides,active.getName(),true).get("GPX")==2,"single pass count GPX");
   check(RideHistoryFilter.count(rides,"전체",active.getName(),true)==6,"all count");
   check(RideHistoryFilter.count(rides,"기록 중",active.getName(),true)==1,"active count");
   check(RideHistoryFilter.count(rides,"완료",active.getName(),true)==1,"completed count");
   check(RideHistoryFilter.count(rides,"GPX",active.getName(),true)==2,"GPX count");
   check(RideHistoryFilter.count(rides,"복구",active.getName(),true)==1,"recovered count");
   check(RideHistoryFilter.count(rides,"중단",active.getName(),true)==1,"interrupted count");
   check(RideHistoryFilter.count(rides,"기록 중",active.getName(),false)==0,"recording ended");
   check(RideHistoryFilter.count(rides,"중단",active.getName(),false)==2,"active becomes interrupted");
   check(RideHistoryFilter.select(rides,"GPX",active.getName(),true).equals(Arrays.asList(imported,mixed)),"stable ordering");
   check(RideHistoryFilter.select(rides,"unknown",active.getName(),true).equals(rides),"unknown filter does not hide rides");
   check(complete.length()==0&&imported.length()==0&&active.length()==0,"never alters files");
   System.out.println("PASS: "+assertions+" ride-history filter assertions");
  }finally{
   File[] files=dir.listFiles();if(files!=null)for(File f:files)f.delete();dir.delete();
  }
 }
}
