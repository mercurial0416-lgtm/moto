package com.motohub.next;

/** Stable receipt slots: slot 1 is the original single-photo filename for old backups. */
final class ReceiptSlots {
 private ReceiptSlots(){}
 static final int MAX_SLOTS=4;
 static final String UUID="[a-fA-F0-9]{8}(-[a-fA-F0-9]{4}){3}-[a-fA-F0-9]{12}";
 static final String PATTERN="record-receipt-"+UUID+"(?:-[2-4])?\\.jpg";
 static boolean validId(String id){return id!=null&&id.matches(UUID);}
 static boolean validSlot(int slot){return slot>=1&&slot<=MAX_SLOTS;}
 static String filename(String id,int slot){
  if(!validId(id)||!validSlot(slot))throw new IllegalArgumentException("Invalid receipt slot");
  return "record-receipt-"+id+(slot==1?"":"-"+slot)+".jpg";
 }
}
