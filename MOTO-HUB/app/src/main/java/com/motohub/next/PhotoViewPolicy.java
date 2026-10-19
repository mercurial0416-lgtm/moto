package com.motohub.next;

/** Prevents catalog previews from using the current garage bike's manual photo. */
final class PhotoViewPolicy {
 private PhotoViewPolicy(){}
 static boolean hasGarageOwner(String bikeId){
  return bikeId!=null && bikeId.matches("[a-fA-F0-9-]{1,64}");
 }
}
