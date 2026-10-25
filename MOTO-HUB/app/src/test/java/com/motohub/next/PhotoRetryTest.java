package com.motohub.next;

import org.junit.Test;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;

public class PhotoRetryTest {
 @Test public void skipsFailedCandidate() {
  AtomicInteger tries=new AtomicInteger();
  String found=PhotoRetry.firstSuccess(Arrays.asList("broken","valid"),3,item->{
   tries.incrementAndGet();return "valid".equals(item)?item:null;
  });
  assertEquals("valid",found);
  assertEquals(2,tries.get());
 }
 @Test public void recoversFromDownloadException() {
  String found=PhotoRetry.firstSuccess(Arrays.asList("throws","valid"),3,item->{
   if("throws".equals(item))throw new java.io.IOException("download failed");return item;
  });
  assertEquals("valid",found);
 }
 @Test public void limitsNetworkAttempts() {
  AtomicInteger tries=new AtomicInteger();
  assertNull(PhotoRetry.firstSuccess(Arrays.asList("1","2","3"),2,item->{tries.incrementAndGet();return null;}));
  assertEquals(2,tries.get());
 }
}
