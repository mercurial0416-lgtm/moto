package com.motohub.next;
import java.util.UUID;
/** Reject malformed server/group identifiers before starting a location-sharing session. */
final class SharingPrivacyPolicy {
 private SharingPrivacyPolicy(){}
 static boolean validUuid(String input){
  if(input==null||!input.matches("[a-fA-F0-9]{8}(-[a-fA-F0-9]{4}){3}-[a-fA-F0-9]{12}"))return false;
  try{return UUID.fromString(input).toString().equalsIgnoreCase(input);}catch(IllegalArgumentException invalid){return false;}
 }
}
