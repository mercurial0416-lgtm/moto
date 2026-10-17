package com.motohub.next;

import java.util.WeakHashMap;

/**
 * Prevent an earlier asynchronous photo lookup from overwriting a newer choice
 * rendered in the same ImageView. Weak keys avoid retaining dismissed views.
 * All calls are thread-safe; callers must still update Android views on the UI thread.
 */
final class PhotoRequestGuard {
 private static final WeakHashMap<Object, Long> active = new WeakHashMap<>();
 private static long nextToken = 0;
 private PhotoRequestGuard(){}

 static synchronized long begin(Object target) {
  if(target==null)throw new IllegalArgumentException("Photo target is required");
  long token = ++nextToken;
  active.put(target, token);
  return token;
 }

 static synchronized boolean isCurrent(Object target, long token) {
  Long current = active.get(target);
  return current != null && current == token;
 }

 static synchronized void cancel(Object target) { active.remove(target); }
}
