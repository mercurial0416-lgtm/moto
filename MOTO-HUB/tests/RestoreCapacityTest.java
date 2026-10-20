package com.motohub.next;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
/** Fails before modifying the garage if there is inadequate rollback headroom. */
public final class RestoreCapacityTest {
 private static int checks;
 private static void check(boolean pass,String label){checks++;if(!pass)throw new AssertionError(label);}
 private static void expectLow(long free,long required)throws Exception {
  try {RestoreCapacity.ensure(free,required);throw new AssertionError("low storage accepted");}
  catch(IOException expected){check(expected.getMessage().contains("부족")||expected.getMessage().contains("확인"),"low space refused");}
 }
 private static void clean(File dir)throws IOException {
  if(dir.isDirectory()){File[] files=dir.listFiles();if(files!=null)for(File f:files)clean(f);}
  Files.deleteIfExists(dir.toPath());
 }
 public static void main(String[] args)throws Exception {
  long required=RestoreCapacity.required(9_000_000L,10_000_000L,3_000L);
  check(required==RestoreCapacity.RESERVE_BYTES+19_003_000L,"both snapshots and margin included");
  expectLow(required-1,required);RestoreCapacity.ensure(required,required);checks++;
  expectLow(0,required);
  for(long[] bad:new long[][]{{-1,0,0},{0,-1,0},{0,0,-1}}){
   try {RestoreCapacity.required(bad[0],bad[1],bad[2]);throw new AssertionError("negative size");}
   catch(IOException expected){checks++;}
  }
  try{RestoreCapacity.required(Long.MAX_VALUE,10,0);throw new AssertionError("overflow");}
  catch(IOException expected){checks++;}
  File root=Files.createTempDirectory("moto-space-check").toFile();
  try {
   File original=new File(root,"before.dat");File fresh=new File(root,"after.dat");
   Files.write(original.toPath(),new byte[100]);Files.write(fresh.toPath(),new byte[250]);
   check(RestoreCapacity.sum(root,Arrays.asList("before.dat","after.dat"))==350,"sizes from actual files");
   check(RestoreCapacity.available(root)>0,"disk reports free bytes");
   try {RestoreCapacity.sum(root,Collections.singletonList("missing.dat"));throw new AssertionError("missing asset");}
   catch(IOException expected){checks++;}
  }finally{clean(root);}
  check(RestoreCapacity.mb(Long.MAX_VALUE)>0,"large free-space size formats without overflow");
  File linkedRoot=Files.createTempDirectory("moto-restore-safe-root").toFile();
  File outside=Files.createTempFile("moto-restore-external", ".jpg").toFile();
  try {
   try {
    Path link=new File(linkedRoot,"photo.jpg").toPath();
    Files.createSymbolicLink(link,outside.toPath());
    try{RestoreCapacity.sum(linkedRoot,Collections.singletonList("photo.jpg"));throw new AssertionError("symlinked restore target accepted");}
    catch(IOException expected){checks++;}
    Files.deleteIfExists(link);
   }catch(UnsupportedOperationException|SecurityException ignored){}
  }finally{Files.deleteIfExists(outside.toPath());clean(linkedRoot);}
  System.out.println("PASS DG restore free-space safety: "+checks+" assertions");
 }
}
