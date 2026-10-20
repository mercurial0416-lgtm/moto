package com.motohub.next;
import java.io.*;
import java.nio.file.*;
import java.util.*;
final class DiQualityPolicyTest {
 static int n=0;
 static void ok(boolean yes,String why){if(!yes)throw new AssertionError(why);n++;}
 public static void main(String[] args)throws Exception{
  String fixed="212c5b39d0082ab4bed78f8da2b719819cd80a20bc116c84f8bdfabca507021b";
  ok(UpdateReadiness.fixedIdentity(fixed,fixed),"matches fixed signing certificate");
  ok(!UpdateReadiness.fixedIdentity("",fixed),"empty signer blocked");
  ok(!UpdateReadiness.fixedIdentity(null,fixed),"unknown signer blocked");
  ok(!UpdateReadiness.fixedIdentity("a".repeat(64),fixed),"debug signer blocked");
  ok(UpdateReadiness.state("",fixed).contains("차단"),"explain unknown signer");
  ok(UpdateReadiness.state("b".repeat(64),fixed).contains("백업"),"explain migration risk");
  ok(UpdateStoragePolicy.required(100_000_000)==2*100_000_000+32L*1024*1024,"installer copy allowance");
  ok(UpdateStoragePolicy.fits(123,UpdateStoragePolicy.required(123)),"exact space allowed");
  ok(!UpdateStoragePolicy.fits(123,UpdateStoragePolicy.required(123)-1),"insufficient space blocked");
  ok(!UpdateStoragePolicy.fits(200_000_000,0),"huge apk insufficient");
  try{UpdateStoragePolicy.required(0);throw new AssertionError("zero size not blocked");}catch(IllegalArgumentException expected){n++;}
  try{UpdateStoragePolicy.required(200_000_001);throw new AssertionError("oversized not blocked");}catch(IllegalArgumentException expected){n++;}
  String u="4cab4f63-02b2-40d4-8a43-9fd89f7c086c";
  ok(SharingPrivacyPolicy.validUuid(u),"valid uuid accepted");
  ok(SharingPrivacyPolicy.validUuid(u.toUpperCase()),"uppercase uuid accepted");
  ok(!SharingPrivacyPolicy.validUuid("-".repeat(36)),"malformed uuid rejected");
  ok(!SharingPrivacyPolicy.validUuid(null),"null uuid rejected");
  ok(!SharingPrivacyPolicy.validUuid("00000000-0000-0000-0000-000000000000/trap"),"extra path rejected");
  List<String> all=RideReadinessReport.summary(false,false,false,false,false);
  ok(all.size()==7,"all checks and privacy note");
  ok(all.get(0).contains("필요")&&all.get(1).contains("꺼짐")&&all.get(2).contains("차단"),"bad settings diagnostic");
  ok(all.get(6).contains("동의")&&all.get(5).contains("자동"),"privacy and reboot guidance");
  all=RideReadinessReport.summary(true,true,true,true,true);
  ok(all.get(0).contains("허용")&&all.get(3).contains("제외됨"),"good settings diagnostic");
  File root=Files.createTempDirectory("moto-cache-test-").toFile();
  try{
   File dir=new File(root,"photos");ok(dir.mkdirs(),"setup");
   long now=System.currentTimeMillis();
   File old=new File(dir,"aaaaaaaaaaaaaaaaaaaaaaaa.jpg"),oldMeta=new File(dir,old.getName()+".json");
   File newer=new File(dir,"bbbbbbbbbbbbbbbbbbbbbbbb.jpg"),newerMeta=new File(dir,newer.getName()+".json");
   File chosen=new File(dir,"cccccccccccccccccccccccc.jpg"),chosenMeta=new File(dir,chosen.getName()+".json");
   File unknown=new File(dir,"dddddddddddddddddddddddd.jpg"),unknownMeta=new File(dir,unknown.getName()+".json");
   for(File photo:List.of(old,newer,chosen,unknown))try(RandomAccessFile f=new RandomAccessFile(photo,"rw")){f.setLength(35L*1024*1024);}
   String auto="{\"imageUrl\":\"https://upload.wikimedia.org/abc\",\"pageUrl\":\"https://commons.wikimedia.org\",\"exactYearCandidate\":true}";
   Files.writeString(oldMeta.toPath(),auto);Files.writeString(newerMeta.toPath(),auto);
   Files.writeString(chosenMeta.toPath(),auto.substring(0,auto.length()-1)+",\"userSelected\": true}");
   Files.writeString(unknownMeta.toPath(),"{\"other\":\"manual\"}");
   old.setLastModified(now-10L*24*60*60*1000);newer.setLastModified(now-9L*24*60*60*1000);
   chosen.setLastModified(now-15L*24*60*60*1000);unknown.setLastModified(now-15L*24*60*60*1000);
   int count=AutoPhotoCache.trim(dir,now);
   ok(count==1,"only oldest auto downloaded photo pruned");
   ok(!old.exists()&&!oldMeta.exists(),"oldest auto photo and metadata gone");
   ok(newer.exists()&&newerMeta.exists(),"newer auto preserved");
   ok(chosen.exists()&&chosenMeta.exists(),"user chosen photo never deleted");
   ok(unknown.exists()&&unknownMeta.exists(),"unknown photo never deleted");
   ok(AutoPhotoCache.trim(dir,now)==0,"second cleanup idempotent");
  }finally{try(var walk=Files.walk(root.toPath())){walk.sorted(Comparator.reverseOrder()).forEach(p->{try{Files.deleteIfExists(p);}catch(IOException ignored){}});}}
  System.out.println("PASS DI quality policy checks: "+n);
 }
}
