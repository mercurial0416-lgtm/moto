package com.motohub.next;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

/** Streaming GPS-sidecar scan for home; no sorting, no track contents, no UI-thread disk I/O. */
final class RideHomeStatus {
 private static final Pattern NAME=Pattern.compile("[0-9]{10,15}-[a-f0-9-]{36}\\.track");
 private RideHomeStatus(){}
 static int interrupted(File filesDir)throws IOException{
  File dir=TrackFile.directory(filesDir);
  int count=0;
  try(DirectoryStream<Path> items=Files.newDirectoryStream(dir.toPath())){
   for(Path track:items){
    if(!NAME.matcher(track.getFileName().toString()).matches()||
       !Files.isRegularFile(track,LinkOption.NOFOLLOW_LINKS))continue;
    String prefix=track.toString();
    if(Files.isRegularFile(Paths.get(prefix+".done"),LinkOption.NOFOLLOW_LINKS)||
       Files.isRegularFile(Paths.get(prefix+".recovered"),LinkOption.NOFOLLOW_LINKS)||
       Files.isRegularFile(Paths.get(prefix+".imported"),LinkOption.NOFOLLOW_LINKS))continue;
    count++;
   }
  }
  return count;
 }
}
