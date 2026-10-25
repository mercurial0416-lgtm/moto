package com.motohub.next;
import java.io.*;
import java.nio.file.*;
import java.util.*;
public final class AutoPhotoCacheStrictTest {
 static int checks=0;
 static final String PREFIX="{\"imageUrl\":\"https://upload.wikimedia.org/wikipedia/commons/test.jpg\",\"pageUrl\":\"https://commons.wikimedia.org/wiki/File:Test\",\"exactYearCandidate\":false";
 static void check(String metadata,boolean expected,String reason){
  checks++;
  if(BackupFormat.automaticallyCachedModelMetadata(metadata)!=expected)throw new AssertionError(reason);
 }
 static void yes(boolean expression,String reason){checks++;if(!expression)throw new AssertionError(reason);}
 static void sparse(File photo)throws IOException {
  try(RandomAccessFile f=new RandomAccessFile(photo,"rw")){f.setLength(40L*1024*1024);}
 }
 static void recursiveDelete(Path root)throws Exception {
  if(!Files.exists(root,LinkOption.NOFOLLOW_LINKS))return;
  try(java.util.stream.Stream<Path> paths=Files.walk(root)){
   for(Path p:paths.sorted(Comparator.reverseOrder()).toArray(Path[]::new))
    Files.deleteIfExists(p);
  }
 }
 public static void main(String[] args)throws Exception{
  check(PREFIX+"}",true,"strict plain automatic candidate");
  check(PREFIX+",\"userSelected\":false}",true,"explicit false auto");
  check(PREFIX+",\"userSelected\":true}",false,"user-selected metadata never deleted");
  check(PREFIX+",\"u\\u0073erSelected\":true}",false,"escaped userSelected key");
  check(PREFIX+",\"extra\":{\"userSelected\":true}}",true,"nested flag is not user's top-level choice");
  check(PREFIX+",\"userSelected\":true,\"userSelected\":false}",false,"duplicate selection rejected");
  check(PREFIX+",\"imageUrl\":\"https://upload.wikimedia.org/fake\"}",false,"duplicate URL rejected");
  check(PREFIX+",\"userSelected\":\"false\"}",false,"ambiguous selection type");
  check(PREFIX+",\"userSelected\":null}",false,"unknown selection type");
  check("{\"note\":"+quote(PREFIX+"}")+"}",false,"metadata in quoted note cannot grant eviction");
  check("{\"imageUrl\":\"https://malicious.invalid/a\",\"pageUrl\":\"https://commons.wikimedia.org/x\",\"exactYearCandidate\":true}",false,"untrusted image domain");
  check("{\"imageUrl\":\"https://upload.wikimedia.org/a\",\"pageUrl\":\"\",\"exactYearCandidate\":true}",false,"missing provenance");
  check("{\"imageUrl\":3,\"pageUrl\":\"https://commons.wikimedia.org/a\",\"exactYearCandidate\":true}",false,"wrong URL type");
  check("{\"imageUrl\":\"https://upload.wikimedia.org/a\",\"pageUrl\":\"https://commons.wikimedia.org/a\",\"exactYearCandidate\":\"true\"}",false,"wrong exact-year type");
  check(PREFIX+",}",false,"malformed metadata cannot be trimmed");
  check(PREFIX+"} trailing",false,"trailing data cannot be trimmed");
  File root=Files.createTempDirectory("moto-strict-cache").toFile();
  try{
   long now=System.currentTimeMillis();
   File auto1=new File(root,"111111111111111111111111.jpg");
   File auto2=new File(root,"222222222222222222222222.jpg");
   File manual=new File(root,"333333333333333333333333.jpg");
   File broken=new File(root,"444444444444444444444444.jpg");
   for(File p:new File[]{auto1,auto2,manual,broken})sparse(p);
   Files.writeString(Path.of(auto1+".json"),PREFIX+"}");
   Files.writeString(Path.of(auto2+".json"),PREFIX+"}");
   Files.writeString(Path.of(manual+".json"),PREFIX+",\"userSelected\":true}");
   Files.writeString(Path.of(broken+".json"),PREFIX+",\"userSelected\":\"false\"}");
   for(File p:new File[]{auto1,auto2,manual,broken})p.setLastModified(now-8L*24*60*60*1000);
   auto1.setLastModified(now-9L*24*60*60*1000);
   int deleted=AutoPhotoCache.trim(root,now);
   yes(deleted==1,"cache trimmed by bounded size");
   yes(!auto1.exists(),"oldest provably auto downloaded photo evicted");
   yes(!new File(auto1+".json").exists(),"evicted file attribution removed");
   yes(auto2.exists(),"recent automatic photo stays under cap");
   yes(manual.exists(),"manually chosen photo always kept");
   yes(broken.exists(),"ambiguous metadata photo always kept");
   yes(AutoPhotoCache.trim(root,now)==0,"second trim is idempotent");
   File outside=File.createTempFile("moto-cache-outside",".jpg");
   try{
    Files.createSymbolicLink(new File(root,"555555555555555555555555.jpg").toPath(),outside.toPath());
    yes(AutoPhotoCache.trim(root,now)==0,"symlink cannot be followed");
    yes(outside.exists(),"external file untouched");
   }finally{outside.delete();}
  }finally{recursiveDelete(root.toPath());}
  System.out.println("PASS strict model photo cache: "+checks+" checks");
 }
 static String quote(String value){return "\""+value.replace("\\","\\\\").replace("\"","\\\"")+"\"";}
}
