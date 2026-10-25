package com.motohub.next;

import java.io.File;
import java.util.List;

/** Finds recoverable local rides on a worker thread; never starts GPS or changes files. */
final class InterruptedRideNotice {
 private InterruptedRideNotice() {}
 static File latest(File filesDir, String active, boolean recording) throws Exception {
  List<File> tracks=TrackFile.tracks(filesDir);
  // Skip already-finished rides without using the expensive validation budget.
  // Otherwise 40 newer completed rides can hide an older, recoverable crash.
  int checkedCandidates=0;
  for(File file:tracks) {
   if(!file.getName().matches("[0-9]{10,15}-[a-f0-9-]{36}\\.track"))continue;
   if(recording && file.getName().equals(active))continue;
   if(new File(file.getPath()+".done").exists() ||
      new File(file.getPath()+".imported").exists() ||
      new File(file.getPath()+".recovered").exists())continue;
   if(file.length()>TrackFile.MAX_BYTES)continue;
   if(++checkedCandidates>40)break;
   try {if(TrackFile.read(file,(lat,lon,acc,time)->{})>0)return file;}
   catch(Exception ignored) { /* Leave malformed originals untouched. */ }
  }
  return null;
 }
}
