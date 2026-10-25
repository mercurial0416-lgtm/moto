package com.motohub.next;

/** Human-readable labels for the local unsaved-draft reminder. No personal data leaves the device. */
final class DraftBanner {
 private DraftBanner() {}
 static String label(String key) {
  if(key==null) return "";
  return switch(key) {
   case "vehicles" -> "바이크";
   case "services" -> "정비";
   case "expenses" -> "주유·비용";
   case "listings" -> "매물";
   case "routes" -> "코스";
   case "gear" -> "보호장구";
   default -> "";
  };
 }
}
