package com.motohub.next;
import org.json.*;import java.net.*;import java.io.*;
final class UpdateManifest {
 static final String PINNED_CERT_SHA256="212c5b39d0082ab4bed78f8da2b719819cd80a20bc116c84f8bdfabca507021b";
 final long versionCode,sizeBytes;final int minSdk;final String versionName,sha256,notes,apkUrl,raw,certificateSha256;
 UpdateManifest(String json,String expectedPackage)throws Exception{JSONObject o=new JSONObject(json);if(!expectedPackage.equals(o.getString("packageName")))throw new IOException("다른 앱의 업데이트입니다.");versionCode=o.getLong("versionCode");versionName=o.getString("versionName");sizeBytes=o.getLong("sizeBytes");sha256=o.getString("sha256").toLowerCase(java.util.Locale.ROOT);certificateSha256=o.getString("certificateSha256").toLowerCase(java.util.Locale.ROOT).replace(":","");apkUrl=o.getString("apkUrl");minSdk=o.getInt("minSdk");notes=o.optString("notes","");https(apkUrl);
String prefix="https://github.com/mercurial0416-lgtm/moto/releases/download/";
if(apkUrl.startsWith(prefix) && !apkUrl.startsWith(prefix+"moto-hub-v"+versionCode+"/"))
 throw new IOException("업데이트 파일 주소와 버전 정보가 일치하지 않습니다.");
if(versionCode<=0||sizeBytes<=0||sizeBytes>200_000_000||minSdk<26||versionName.length()>100||notes.length()>12000||!sha256.matches("[a-f0-9]{64}")||!PINNED_CERT_SHA256.equals(certificateSha256))throw new IOException("업데이트 정보 형식이 올바르지 않습니다.");raw=json;}
 static URL https(String value)throws Exception{URI u=new URI(value);if(value==null||value.length()>2048||value.chars().anyMatch(ch->ch<=32||ch==127)||!"https".equalsIgnoreCase(u.getScheme())||u.getHost()==null||u.getUserInfo()!=null||u.getFragment()!=null||u.getPort()==0||u.getHost().indexOf('.')<1||u.getHost().endsWith("."))throw new IOException("인증 정보가 없는 HTTPS 주소만 허용합니다.");return u.toURL();}
 static byte[] bounded(InputStream in,int limit)throws IOException{ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1){if(out.size()+n>limit)throw new IOException("응답 크기가 제한을 초과했습니다.");out.write(b,0,n);}return out.toByteArray();}
}
