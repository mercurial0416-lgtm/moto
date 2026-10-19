package com.motohub.next;

/** Prevent an older gallery search or a dismissed dialog from updating the current UI. */
final class PhotoGalleryLoadGate {
 private int generation;
 private boolean closed;
 synchronized int begin(){return closed?-1:++generation;}
 synchronized boolean accepts(int token){return !closed&&token>0&&token==generation;}
 synchronized void close(){closed=true;generation++;}
}
