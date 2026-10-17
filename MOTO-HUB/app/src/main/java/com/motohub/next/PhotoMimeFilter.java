package com.motohub.next;

import java.util.Locale;

/** BitmapFactory-compatible raster types for Commons photo candidates. */
final class PhotoMimeFilter {
 private PhotoMimeFilter() {}
 static boolean supported(String mime) {
  if(mime == null) return false;
  switch(mime.trim().toLowerCase(Locale.ROOT)) {
   case "image/jpeg":
   case "image/png":
   case "image/webp":
    return true;
   default:
    return false;
  }
 }
}
