package com.motohub.next;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
/** Force a post-replacement marker failure and assert byte-for-byte rollback. */
public final class TrackRecoveryRollbackTest {
 private static int checks;
 private static void ok(boolean valid,String message){checks++;if(!valid)throw new AssertionError(message);}
 public static void main(String[] args)throws Exception{
  Path dir=Files.createTempDirectory("moto-gps-rollback-");
  File track=dir.resolve("1792122693079-12345678-1234-1234-1234-123456789abc.track").toFile();
  String rows=TrackFile.point(37,127,6,1792120000000L)+TrackFile.point(37.0001,127.0001,6,1792120005000L);
  byte[] raw=(rows+"incomplete,127").getBytes(StandardCharsets.UTF_8);
  Path marker=Path.of(track.getPath()+".recovered");
  try{
   Files.write(track.toPath(),raw);
   boolean failed=false;
   try{TrackRecovery.recover(track,()->{
    try{Files.createDirectory(marker);}catch(IOException e){throw new IllegalStateException(e);}
   });}catch(IOException expected){failed=true;}
   ok(failed,"marker failure must propagate");
   ok(Arrays.equals(raw,Files.readAllBytes(track.toPath())),"failed recovery must preserve byte-exact original");
   ok(!Files.isRegularFile(marker),"must not leave successful marker");
   ok(Files.list(dir).noneMatch(p->p.getFileName().toString().startsWith(".ride-original-")),"no orphan backup on successful rollback");
   Files.delete(marker);
   TrackRecovery.Result result=TrackRecovery.recover(track);
   ok(result.points==2,"preserve committed rows");
   ok(result.discardedBytes==raw.length-rows.getBytes(StandardCharsets.UTF_8).length,"count removed suffix only");
   ok(Files.readString(track.toPath()).equals(rows),"successful recovery must not alter committed rows");
   ok(Files.isRegularFile(marker),"success has durable recovered marker");
   ok(Files.list(dir).noneMatch(p->p.getFileName().toString().startsWith(".ride-original-")),"success removes temporary original");
   System.out.println("PASS recovery rollback: "+checks+" checks");
  }finally{
   try(var paths=Files.list(dir)){paths.forEach(p->{try{Files.deleteIfExists(p);}catch(IOException e){throw new RuntimeException(e);}});}
   Files.deleteIfExists(dir);
  }
 }
}
