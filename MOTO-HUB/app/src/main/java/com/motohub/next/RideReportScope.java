package com.motohub.next;

import java.io.File;
import java.io.IOException;

/** Local ride-statistics bike scope. No track points or bike IDs leave the phone. */
final class RideReportScope {
 private RideReportScope(){}
 /** null = all bikes, empty = unassigned, UUID = one explicitly selected bike. */
 static boolean include(File track,String selectedBikeId)throws IOException {
  if(selectedBikeId==null)return true;
  if(!selectedBikeId.isEmpty()&&!RideBikeBinding.valid(selectedBikeId))
   throw new IOException("바이크 선택 정보가 올바르지 않습니다.");
  return selectedBikeId.equals(RideBikeBinding.readStrict(track));
 }
}
