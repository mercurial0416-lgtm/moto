package com.motohub.next;
import org.json.JSONObject;
/** Contents are fully parsed and validated by RestoreArchive.prepare first. */
final class RestorePreflightDetail {
 private RestorePreflightDetail(){}
 static String text(JSONObject imported,JSONObject current,int tracks,int photos,int receipts){
  StringBuilder result=new StringBuilder("\n\nZIP 사전검사 결과: JSON·GPS·사진 검증 완료")
   .append("\nGPS ").append(tracks).append("건 · 바이크·모델 사진 ")
   .append(photos).append("장 · 영수증 ").append(receipts).append("장");
  for(String key:new String[]{"vehicles","expenses","services"}){
   int old=RestorePreview.count(current,key),fresh=RestorePreview.count(imported,key);
   if(old>fresh)result.append("\n주의: ").append(label(key)).append(" 기존 ")
    .append(old).append("건 → 복원 후 ").append(fresh).append("건");
  }
  if(tracks==0)result.append("\nGPS 기록 파일이 없습니다. 다른 백업을 확인하세요.");
  if(photos==0)result.append("\n사진 파일 0장: 바이크 사진이 필요한지 확인하세요.");
  return result.append("\n백업 외부에 별도 저장한 파일이 있는지는 확인할 수 없습니다. 복원 전 ZIP을 꼭 확인하세요.").toString();
 }
 private static String label(String k){return "vehicles".equals(k)?"바이크":"services".equals(k)?"정비":"주유·비용";}
}
