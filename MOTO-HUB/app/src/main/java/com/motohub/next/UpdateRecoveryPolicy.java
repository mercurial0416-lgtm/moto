package com.motohub.next;

/** Protect a verified pre-install checkpoint from routine local backup rotation. */
final class UpdateRecoveryPolicy {
 private UpdateRecoveryPolicy(){}
 static boolean validSnapshotName(String name){
  return name!=null&&name.matches("motohub-[0-9]{13}-[a-f0-9]{8}\\.json");
 }
 static boolean isPinned(String candidate,String pending,String lastGood){
  return validSnapshotName(candidate) &&
   (candidate.equals(pending)||candidate.equals(lastGood));
 }
}
