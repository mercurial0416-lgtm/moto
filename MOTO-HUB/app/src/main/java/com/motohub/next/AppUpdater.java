package com.motohub.next;
import android.app.*;import android.content.*;import android.content.pm.*;import android.net.Uri;import android.os.*;import android.provider.Settings;import android.widget.*;import java.io.*;import java.net.*;import java.nio.charset.StandardCharsets;import java.security.*;import java.util.*;import java.util.concurrent.*;import java.util.concurrent.atomic.AtomicBoolean;
final class AppUpdater {
 static final ExecutorService WORKER=Executors.newSingleThreadExecutor();static final AtomicBoolean BUSY=new AtomicBoolean();
 final MainActivity a;AppUpdater(MainActivity a){this.a=a;}
 void show(){
  LinearLayout p=a.page("앱 업데이트");
  a.label(p,"현재 버전  "+BuildConfig.VERSION_NAME+" ("+BuildConfig.VERSION_CODE+")",20,a.fg,true);
  String current=SigningIdentity.current(a);
  boolean fixed=UpdateReadiness.fixedIdentity(current,UpdateManifest.PINNED_CERT_SHA256);
  a.label(p,fixed?"현재 설치본: 고정 서명 인증서 일치":"현재 설치본: 서명 불일치 또는 확인 불가",
   14,fixed?a.muted:a.accent,true);
  if(!fixed){
   a.label(p,UpdateReadiness.state(current,UpdateManifest.PINNED_CERT_SHA256),14,a.muted,false);
   a.label(p,"기존 앱을 삭제하면 기기에 저장된 기록이 사라질 수 있습니다. 먼저 전체 ZIP 백업을 보관하세요.",14,a.fg,false);
   a.show();return;
  }
  if(BuildConfig.UPDATE_MANIFEST_URL.isEmpty()){
   a.label(p,"업데이트 서버가 설정되지 않았습니다.",14,a.muted,false);
   a.show();return;
  }
  a.label(p,"배포된 APK가 현재 버전보다 높을 때만 내려받습니다. 파일 검사와 서명 비교 후 Android 설치 화면에서 직접 승인합니다.",14,a.muted,false);
  a.action(p,"배포 버전 확인",true,this::check);
  if(cachedUpdateReady()){
   a.label(p,"내려받은 파일 있음 · 설치 전에 해시와 APK 서명을 다시 확인합니다.",13,a.muted,false);
   a.action(p,"다운로드한 업데이트 설치",false,()->a.attempt(this::install));
  }else{
   a.label(p,"설치할 업데이트 파일이 없습니다. 배포 버전부터 확인하세요.",13,a.muted,false);
  }
  a.show();
 }
 /** Cheap display-only test; install() always repeats SHA-256, package and signer checks. */
 boolean cachedUpdateReady(){
  try{
   String raw=a.getSharedPreferences("updates",0).getString("ready","");
   if(raw==null||raw.isEmpty())return false;
   UpdateManifest m=new UpdateManifest(raw,a.getPackageName());
   if(!UpdateVersionPolicy.isUpgrade(m.versionCode,BuildConfig.VERSION_CODE))return false;
   File ready=new File(new File(a.getCacheDir(),"updates"),m.sha256+".apk");
   return ready.isFile()&&ready.length()==m.sizeBytes;
  }catch(Exception ignored){return false;}
 }

 interface Job{void run()throws Exception;}
 void work(Job job){if(!BUSY.compareAndSet(false,true)){a.toast("업데이트 작업이 진행 중입니다.");return;}WORKER.execute(()->{try{job.run();}catch(Exception e){ui(()->a.error(e.getMessage()==null?"업데이트 작업 실패":e.getMessage()));}finally{BUSY.set(false);}});}
 void ui(Runnable r){a.runOnUiThread(()->{if(!a.isFinishing()&&!a.isDestroyed())r.run();});}
 static HttpURLConnection connect(String address)throws Exception{
  for(int redirects=0;redirects<=UpdateDownloadPolicy.MAX_REDIRECTS;redirects++){
   URL url=UpdateManifest.https(address);
   HttpURLConnection c=(HttpURLConnection)url.openConnection();c.setConnectTimeout(15000);c.setReadTimeout(20000);c.setInstanceFollowRedirects(false);c.setRequestProperty("Accept-Encoding","identity");
   int code;try{code=c.getResponseCode();}catch(Exception error){c.disconnect();throw error;}
   if(code==200)return c;
   if(code==301||code==302||code==303||code==307||code==308){
    String location=c.getHeaderField("Location");c.disconnect();
    if(location==null||redirects==UpdateDownloadPolicy.MAX_REDIRECTS)throw new IOException("업데이트 다운로드의 이동 횟수가 너무 많습니다.");
    String next=new URL(url,location).toExternalForm();
    if(!UpdateDownloadPolicy.safeRedirect(address,next))throw new IOException("안전하지 않은 업데이트 다운로드 이동 주소입니다.");
    address=next;continue;
   }
   c.disconnect();throw new IOException("업데이트 서버 응답 오류 (HTTP "+code+")");
  }
  throw new IOException("업데이트 다운로드 이동 횟수 초과");
 }
 void check(){if(!UpdateReadiness.fixedIdentity(SigningIdentity.current(a),UpdateManifest.PINNED_CERT_SHA256)){a.error(UpdateReadiness.state(SigningIdentity.current(a),UpdateManifest.PINNED_CERT_SHA256));return;}a.toast("최신 버전을 확인합니다.");work(()->{HttpURLConnection c=connect(BuildConfig.UPDATE_MANIFEST_URL);UpdateManifest m;try(InputStream in=c.getInputStream()){m=new UpdateManifest(new String(UpdateManifest.bounded(in,65536),StandardCharsets.UTF_8),a.getPackageName());}finally{c.disconnect();}if(m.minSdk>Build.VERSION.SDK_INT)throw new IOException("이 업데이트는 더 높은 Android 버전이 필요합니다.");ui(()->{if(!UpdateVersionPolicy.isUpgrade(m.versionCode,BuildConfig.VERSION_CODE)){
    new AlertDialog.Builder(a).setTitle("업데이트 없음")
     .setMessage(UpdateVersionPolicy.currentMessage(m.versionCode,BuildConfig.VERSION_CODE))
     .setPositiveButton("확인",null).show();return;}new AlertDialog.Builder(a).setTitle(m.versionName+" 업데이트").setMessage(m.notes+"\n\n다운로드: "+String.format(Locale.KOREA,"%.1f MB",m.sizeBytes/1048576.0)).setNegativeButton("나중에",null).setPositiveButton("다운로드",(d,w)->download(m)).show();});});}
 void download(UpdateManifest m){a.toast("APK 다운로드를 시작합니다.");work(()->{File dir=new File(a.getCacheDir(),"updates");if(!dir.isDirectory()&&!dir.mkdirs())throw new IOException("업데이트 폴더 생성 실패");if(!UpdateStoragePolicy.fits(m.sizeBytes,dir.getUsableSpace()))throw new IOException("업데이트에 필요한 저장공간이 부족합니다. 최소 "+UpdateStoragePolicy.required(m.sizeBytes)/1048576+"MB를 확보해 주세요.");File part=new File(dir,"download.part"),ready=new File(dir,m.sha256+".apk");HttpURLConnection c=connect(m.apkUrl);try{long bytes=0,deadline=SystemClock.elapsedRealtime()+180000;MessageDigest digest=MessageDigest.getInstance("SHA-256");try(InputStream in=c.getInputStream();FileOutputStream out=new FileOutputStream(part)){byte[] b=new byte[32768];int n,nextProgress=25;while((n=in.read(b))!=-1){if(SystemClock.elapsedRealtime()>deadline)throw new IOException("다운로드 시간이 초과됐습니다. 다시 시도해 주세요.");bytes+=n;if(bytes>m.sizeBytes)throw new IOException("APK 크기가 서버 정보와 다릅니다.");digest.update(b,0,n);out.write(b,0,n);if(bytes*100/m.sizeBytes>=nextProgress){final int milestone=nextProgress;ui(()->a.toast("업데이트 파일 다운로드 "+milestone+"%"));nextProgress+=25;}}out.getFD().sync();}if(bytes!=m.sizeBytes||!hex(digest.digest()).equals(m.sha256))throw new IOException("APK 검사값 불일치. 다운로드를 폐기했습니다.");validateArchive(part,m);if(ready.exists()&&!ready.delete())throw new IOException("기존 다운로드 정리 실패");if(!part.renameTo(ready))throw new IOException("APK 저장 실패");if(!a.getSharedPreferences("updates",0).edit().putString("ready",m.raw).commit())throw new IOException("업데이트 상태 저장 실패");UpdateCacheRetention.prune(dir,m.sha256);ui(()->new AlertDialog.Builder(a).setTitle("업데이트 준비 완료").setMessage("설치 중 앱이 종료될 수 있습니다. 저장한 기록은 유지하는 업데이트를 요청합니다.").setNegativeButton("나중에",null).setPositiveButton("설치 화면 열기",(d,w)->a.attempt(this::install)).show());}finally{c.disconnect();part.delete();}});}
 static String hex(byte[] bytes){StringBuilder s=new StringBuilder();for(byte b:bytes)s.append(String.format(Locale.ROOT,"%02x",b&255));return s.toString();}
 static Set<String> signers(PackageInfo p){android.content.pm.Signature[] signatures=Build.VERSION.SDK_INT>=28?(p.signingInfo==null?null:p.signingInfo.getApkContentsSigners()):p.signatures;Set<String> set=new HashSet<>();if(signatures!=null)for(android.content.pm.Signature s:signatures)set.add(s.toCharsString());return set;}
 void validateArchive(File file,UpdateManifest m)throws Exception{PackageManager pm=a.getPackageManager();int flags=Build.VERSION.SDK_INT>=28?PackageManager.GET_SIGNING_CERTIFICATES:PackageManager.GET_SIGNATURES;PackageInfo candidate=pm.getPackageArchiveInfo(file.getPath(),flags),current=pm.getPackageInfo(a.getPackageName(),flags);if(candidate==null||!a.getPackageName().equals(candidate.packageName))throw new IOException("MOTO HUB APK가 아닙니다.");long version=Build.VERSION.SDK_INT>=28?candidate.getLongVersionCode():candidate.versionCode;if(version!=m.versionCode||version<=BuildConfig.VERSION_CODE)throw new IOException("APK 버전이 올바르지 않습니다.");Set<String> old=signers(current),next=signers(candidate);if(old.isEmpty()||!old.equals(next))throw new IOException("서명키가 다른 APK입니다. 데이터 보호를 위해 설치를 차단했습니다.");if(!UpdateManifest.PINNED_CERT_SHA256.equals(SigningIdentity.current(a)))throw new IOException("현재 설치된 앱은 고정 서명 버전이 아닙니다. 백업 후 고정 서명판으로 전환해야 합니다.");}
 void install(){work(()->{if(!UpdateSafety.canInstall(RideService.running,RideService.autoArmed,RestoreRideGate.isExporting(),RestoreRideGate.isRestoring()))throw new IOException(UpdateSafety.MESSAGE);String raw=a.getSharedPreferences("updates",0).getString("ready","");if(raw.isEmpty())throw new IOException("먼저 최신 버전을 확인하고 다운로드하세요.");UpdateManifest m=new UpdateManifest(raw,a.getPackageName());File f=new File(new File(a.getCacheDir(),"updates"),m.sha256+".apk");verifyFile(f,m);validateArchive(f,m);ui(()->a.attempt(()->launchInstaller(f)));});}
 static void verifyFile(File f,UpdateManifest m)throws Exception{if(!f.isFile()||f.length()!=m.sizeBytes)throw new IOException("다운로드 파일이 없거나 크기가 다릅니다. 다시 다운로드하세요.");MessageDigest digest=MessageDigest.getInstance("SHA-256");try(InputStream in=new FileInputStream(f)){byte[] buffer=new byte[32768];int n;while((n=in.read(buffer))!=-1)digest.update(buffer,0,n);}if(!hex(digest.digest()).equals(m.sha256))throw new IOException("APK 검사값이 달라 설치를 차단했습니다.");}
 void launchInstaller(File f)throws Exception{if(!UpdateSafety.canInstall(RideService.running,RideService.autoArmed,RestoreRideGate.isExporting(),RestoreRideGate.isRestoring()))throw new IOException(UpdateSafety.MESSAGE);if(!a.getPackageManager().canRequestPackageInstalls()){new AlertDialog.Builder(a).setTitle("설치 허용이 필요합니다").setMessage("Android 설정에서 이 앱의 설치 요청을 허용한 뒤 돌아와 설치 버튼을 다시 눌러 주세요.").setNegativeButton("취소",null).setPositiveButton("설정 열기",(d,w)->a.attempt(()->a.startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:"+a.getPackageName()))))).show();return;}Uri uri=Uri.parse("content://"+a.getPackageName()+".updates/apk/"+f.getName());Intent i=new Intent(Intent.ACTION_INSTALL_PACKAGE).setData(uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION).putExtra(Intent.EXTRA_RETURN_RESULT,true);i.setClipData(ClipData.newRawUri("MOTO HUB update",uri));a.startActivityForResult(i,90);}
}
