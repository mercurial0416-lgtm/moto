package com.motohub.next;

import java.util.List;

/** Try matching photo candidates in order, skipping failed downloads with a strict cap. */
final class PhotoRetry {
 private PhotoRetry() {}
 interface Attempt<T,R> { R load(T candidate) throws Exception; }
 static <T,R> R firstSuccess(List<T> candidates,int maxAttempts,Attempt<T,R> attempt) {
  if(candidates==null||candidates.isEmpty()||maxAttempts<=0)return null;
  int attempted=0;
  for(T candidate:candidates) {
   if(attempted++>=maxAttempts)break;
   try { R result=attempt.load(candidate);if(result!=null)return result; }
   catch(Exception ignored) { /* One broken image must not prevent trying another. */ }
  }
  return null;
 }
}
