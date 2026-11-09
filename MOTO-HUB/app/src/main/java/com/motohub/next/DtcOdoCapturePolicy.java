package com.motohub.next;

/** The driver's physical dashboard observation must be close to the ECU read. */
final class DtcOdoCapturePolicy {
 static final long MAX_CAPTURE_AGE_MS=120_000L;
 private DtcOdoCapturePolicy(){}
 static boolean fresh(long capturedElapsedMs,long nowElapsedMs){
  return capturedElapsedMs>0&&nowElapsedMs>=capturedElapsedMs&&
   nowElapsedMs-capturedElapsedMs<=MAX_CAPTURE_AGE_MS;
 }
}