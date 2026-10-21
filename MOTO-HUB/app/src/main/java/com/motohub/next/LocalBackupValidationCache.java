package com.motohub.next;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;

/**
 * Short-lived, bounded hint for rendering the offline backup picker. NOT an
 * integrity check: every chosen file is fully reparsed by LocalSafetyBackup.read.
 * Timestamp/size changes invalidate entries, and links are never cached.
 */
final class LocalBackupValidationCache {
 static final int MAX_ENTRIES=64;
 static final long MAX_AGE_NANOS=15_000_000_000L;
 private static final class Entry {
  final long size,modified,atNanos;final boolean valid;
  Entry(long size,long modified,long atNanos,boolean valid){
   this.size=size;this.modified=modified;this.atNanos=atNanos;this.valid=valid;
  }
 }
 private static final Map<String,Entry> cache=new LinkedHashMap<String,Entry>(32,0.75f,true){
  @Override protected boolean removeEldestEntry(Map.Entry<String,Entry> eldest){
   return size()>MAX_ENTRIES;
  }
 };
 private LocalBackupValidationCache(){}
 static boolean verify(File file,BooleanSupplier check){
  if(check==null||!LocalBackupFilePolicy.readable(file))return false;
  String path=file.getAbsolutePath();
  long size=file.length(),modified=file.lastModified(),now=System.nanoTime();
  synchronized(cache){
   Entry previous=cache.get(path);
   if(previous!=null&&previous.size==size&&previous.modified==modified
     &&now>=previous.atNanos&&now-previous.atNanos<MAX_AGE_NANOS)
    return previous.valid;
  }
  boolean valid;
  try{valid=check.getAsBoolean();}
  catch(RuntimeException failure){valid=false;}
  // A concurrent rewrite or replacement must not cause stale metadata to be cached.
  if(LocalBackupFilePolicy.readable(file)&&size==file.length()&&modified==file.lastModified()){
   synchronized(cache){cache.put(path,new Entry(size,modified,System.nanoTime(),valid));}
  }else{
   synchronized(cache){cache.remove(path);}
  }
  return valid;
 }
 static void invalidate(File file){
  if(file!=null)synchronized(cache){cache.remove(file.getAbsolutePath());}
 }
 static int sizeForTests(){synchronized(cache){return cache.size();}}
 static void clearForTests(){synchronized(cache){cache.clear();}}
}
