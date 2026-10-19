package com.motohub.next;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
/** Deterministic offline 10K ride stress test: render metadata, do not mutate GPS. */
public final class RideHistoryTenThousandTest {
 private static int checks;
 private static void ok(boolean valid,String why){checks++;if(!valid)throw new AssertionError(why);}
 private static byte[] digest(List<File> files)throws Exception{
  MessageDigest md=MessageDigest.getInstance("SHA-256");
  for(File f:files){md.update(f.getName().getBytes(StandardCharsets.UTF_8));md.update(Files.readAllBytes(f.toPath()));}
  return md.digest();
 }
 public static void main(String[] args)throws Exception{
  final int total=10_000;
  Path root=Files.createTempDirectory("moto-10k-rides");
  Path rides=root.resolve("rides");Files.createDirectories(rides);
  String bike="a231c9d0-2f0b-4d91-a73b-12034f4e20bc";
  long now=System.currentTimeMillis(),start=System.nanoTime();
  try{
   for(int i=0;i<total;i++){
    long time=now-i*60_000L;String uuid=String.format(Locale.ROOT,"00000000-0000-4000-8000-%012d",i);
    File f=rides.resolve(time+"-"+uuid+".track").toFile();
    long record=i%100==0?time-95L*86_400_000L:time;
    Files.writeString(f.toPath(),TrackFile.point(37,127,6,record));
    if(i%100==0)Files.createFile(Path.of(f.getPath()+".imported"));
    if(i%10==0)RideBikeBinding.write(f,bike);
   }
   long created=System.nanoTime();
   List<File> list=TrackFile.tracks(root.toFile());
   ok(list.size()==total,"list all 10k GPS rides");
   byte[] before=digest(list);
   List<File> ordered=RideHistorySort.order(list,RideHistorySort.NEWEST);
   ok(ordered.size()==total,"sort without loss");
   ok(!ordered.get(0).getName().contains("000000000000.track"),"first imported old ride excluded from newest");
   int recent=0,imported=0;
   for(File f:ordered){
    if(RideHistoryPeriod.include(f,now,RideHistoryPeriod.MONTH))recent++;
    if(RideHistoryFilter.IMPORTED.equals(RideHistoryFilter.status(f,"",false)))imported++;
   }
   ok(recent==total-total/100,"import dates based on GPS fixes");
   ok(imported==total/100,"import markers classified");
   ok(RideHistoryBikeFilter.select(list,bike).size()==total/10,"bike selection integrity");
   List<String> labels=new ArrayList<>();
   for(File f:ordered)labels.add(RideHistorySearch.index(RideHistoryLabel.title(f,java.time.ZoneId.of("Asia/Seoul")),RideHistoryFilter.status(f,"",false)));
   String[] terms=RideHistorySearch.terms("GPX");
   int matched=0;for(String label:labels)if(RideHistorySearch.matchesIndex(label,terms))matched++;
   ok(matched==total/100,"search 10k indexed labels");
   ok(Arrays.equals(before,digest(list)),"byte exact GPS preservation after search/sort/filter");
   long end=System.nanoTime();
   System.out.printf(Locale.ROOT,"PASS 10k ride metadata: %d checks, generate %.2fs, index/filter %.2fs, files=%d%n",checks,(created-start)/1e9,(end-created)/1e9,total);
  }finally{
   try(var files=Files.list(rides)){files.forEach(p->{try{Files.deleteIfExists(p);}catch(IOException e){throw new UncheckedIOException(e);}});}
   Files.deleteIfExists(rides);Files.deleteIfExists(root);
  }
 }
}
