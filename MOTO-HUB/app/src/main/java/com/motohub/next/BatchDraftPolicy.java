package com.motohub.next;

/** Reject stale or cross-bike drafts before applying values to the input form. */
final class BatchDraftPolicy {
 private BatchDraftPolicy(){}
 static final long MAX_AGE_MS=30L*24*60*60*1000;
 static boolean recoverable(String currentBike,String savedBike,long createdMs,long nowMs){
  return currentBike!=null&&currentBike.equals(savedBike)&&ReceiptSlots.validId(currentBike)
   &&createdMs>0&&nowMs>=createdMs&&nowMs-createdMs<=MAX_AGE_MS;
 }
}
