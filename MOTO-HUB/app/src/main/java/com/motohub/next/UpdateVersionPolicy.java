package com.motohub.next;

/** Public release versions, not the presence of a development build, decide update status. */
final class UpdateVersionPolicy {
 private UpdateVersionPolicy(){}

 static boolean isUpgrade(long released,long installed){
  return released>installed;
 }

 static String currentMessage(long released,long installed){
  if(isUpgrade(released,installed))return "새 업데이트가 배포됐습니다.";
  if(released==installed)return "현재 공개 배포 버전과 동일합니다.";
  return "공개 배포 버전 "+released+"은 현재 설치 버전 "+installed+"보다 낮습니다. "
      +"새 업데이트가 아직 배포되지 않았습니다.";
 }
}
