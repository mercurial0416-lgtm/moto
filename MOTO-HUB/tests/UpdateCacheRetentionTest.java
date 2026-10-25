package com.motohub.next;
import java.io.*;import java.nio.file.*;
public final class UpdateCacheRetentionTest {
 static int assertions;
 static void check(boolean ok){assertions++;if(!ok)throw new AssertionError("cache test #"+assertions);}
 public static void main(String[] args)throws Exception{
  File root=Files.createTempDirectory("moto-update-test-").toFile();
  String a="a".repeat(64),b="b".repeat(64);
  File old=new File(root,a+".apk"),current=new File(root,b+".apk"),record=new File(root,"rides.track"),part=new File(root,"download.part");
  Files.write(old.toPath(),new byte[]{1});Files.write(current.toPath(),new byte[]{2});
  Files.write(record.toPath(),new byte[]{3});Files.write(part.toPath(),new byte[]{4});
  check(UpdateCacheRetention.prune(root,"INVALID")==0);check(old.isFile());
  check(UpdateCacheRetention.prune(root,b)==1);
  check(!old.exists());check(current.isFile());check(record.isFile());check(part.isFile());
  check(UpdateCacheRetention.prune(root,b)==0);
  File external=Files.createTempFile("moto-other-",".apk").toFile();
  File linked=new File(root,"c".repeat(64)+".apk");
  try{Files.createSymbolicLink(linked.toPath(),external.toPath());check(UpdateCacheRetention.prune(root,b)==0);check(external.isFile());}
  catch(UnsupportedOperationException|java.nio.file.FileSystemException|SecurityException ignored){}
  Files.deleteIfExists(linked.toPath());Files.deleteIfExists(old.toPath());Files.deleteIfExists(current.toPath());
  Files.deleteIfExists(record.toPath());Files.deleteIfExists(part.toPath());Files.deleteIfExists(external.toPath());Files.delete(root.toPath());
  check(!root.exists());
  System.out.println("PASS update cache safe retention: "+assertions);
 }
}
