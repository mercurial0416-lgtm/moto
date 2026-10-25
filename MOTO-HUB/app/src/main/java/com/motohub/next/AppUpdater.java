package com.motohub.next;
import android.app.*;import android.content.*;import android.content.pm.*;import android.net.Uri;import android.os.*;import android.provider.Settings;import android.widget.*;import java.io.*;import java.net.*;import java.nio.charset.StandardCharsets;import java.security.*;import java.util.*;import java.util.concurrent.*;import java.util.concurrent.atomic.AtomicBoolean;
final class AppUpdater {
 static final ExecutorService WORKER=Executors.newSingleThreadExecutor();static final AtomicBoolean BUSY=new AtomicBoolean();
 final MainActivity a;Dialog owner;TextView progressLabel,cacheLabel;ProgressBar progressBar;
 Button cachedInstallButton,cancelDownloadButton;
 final AtomicBoolean cancelRequested=new AtomicBoolean(false);
 static final class DownloadCancelledException extends IOException {
  DownloadCancelledException(){super("다운로드가 중단됐습니다.");}
 }
 void checkCancelled()throws DownloadCancelledException {
  if(cancelRequested.get())throw new DownloadCancelledException();
 }
 AppUpdater(MainActivity a){this.a=a;}

 void show(){
  LinearLayout p=a.page("앱 업데이트");owner=a.dialog;
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
  progressLabel=a.text("업데이트 대기 중",13,a.muted,false);
  LinearLayout.LayoutParams statusLp=new LinearLayout.LayoutParams(-1,-2);statusLp.topMargin=a.dp(12);
  p.addView(progressLabel,statusLp);
  progressBar=new ProgressBar(a,null,android.R.attr.progressBarStyleHorizontal);
  progressBar.setMax(100);
  progressBar.setProgressTintList(android.content.res.ColorStateList.valueOf(a.accent));
  progressBar.setVisibility(android.view.View.GONE);
  LinearLayout.LayoutParams meterLp=new LinearLayout.LayoutParams(-1,a.dp(4));
  meterLp.topMargin=a.dp(7);meterLp.bottomMargin=a.dp(10);p.addView(progressBar,meterLp);
  cancelDownloadButton=a.button("다운로드 중단",false,()->{
   cancelRequested.set(true);
   if(progressLabel!=null)progressLabel.setText("다운로드 중단 요청 중");
   cancelDownloadButton.setEnabled(false);
  });
  cancelDownloadButton.setVisibility(android.view.View.GONE);
  p.addView(cancelDownloadButton,new LinearLayout.LayoutParams(-1,-2));
  a.action(p,"배포 버전 확인",true,this::check);
  cacheLabel=a.text("",13,a.muted,false);
  LinearLayout.LayoutParams cacheLp=new LinearLayout.LayoutParams(-1,-2);cacheLp.bottomMargin=a.dp(8);
  p.addView(cacheLabel,cacheLp);
  cachedInstallButton=a.button("다운로드한 업데이트 설치",false,this::requestInstall);
  LinearLayout.LayoutParams installLp=new LinearLayout.LayoutParams(-1,-2);installLp.bottomMargin=a.dp(8);
  p.addView(cachedInstallButton,installLp);
  refreshCachedControls();
  a.show();
 }
 /** Update labels and buttons together after a download or invalid-cache recovery. */
 void refreshCachedControls(){
  boolean ready=cachedUpdateReady();
  if(cacheLabel!=null)cacheLabel.setText(ready?
   "다운로드한 파일 있음 · 설치 전에 해시와 APK 서명을 다시 확인합니다.":
   "설치할 파일이 없습니다. 배포 버전 확인에서 내려받으세요.");
  if(cachedInstallButton!=null)cachedInstallButton.setVisibility(ready?
   android.view.View.VISIBLE:android.view.View.GONE);
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
 void updateProgress(String action,int percent){
  ui(()->{
   if(progressLabel!=null&&progressLabel.isAttachedToWindow())progressLabel.setText(UpdateProgressPolicy.label(action,percent));
   if(progressBar!=null&&progressBar.isAttachedToWindow()){
    progressBar.setVisibility(android.view.View.VISIBLE);
    if(percent<0)progressBar.setIndeterminate(true);
    else {progressBar.setIndeterminate(false);progressBar.setProgress(UpdateProgressPolicy.percentage(percent,100));}
   }
  });
 }
 void work(String action,Job job){
  if(!BUSY.compareAndSet(false,true)){a.toast("업데이트 작업이 진행 중입니다.");return;}
  updateProgress(action,-1);
  WORKER.execute(()->{
   try{job.run();}
   catch(DownloadCancelledException cancelled){
    ui(()->{if(progressLabel!=null)progressLabel.setText("다운로드 중단 · 다시 확인하면 이어받을 수 있습니다.");});
   }
   catch(Exception e){
    updateProgress("오류 · 다시 시도해 주세요",-1);
    ui(()->a.error(UpdateErrorText.forError(e)));
   }
   finally{
    BUSY.set(false);
    ui(()->{
     if(progressBar!=null&&progressBar.isAttachedToWindow())progressBar.setVisibility(android.view.View.GONE);
     if(cancelDownloadButton!=null){
      cancelDownloadButton.setEnabled(true);
      cancelDownloadButton.setVisibility(android.view.View.GONE);
     }
    });
   }
  });
 }
 void ui(Runnable r){
  // Downloads can finish after the user dismisses the update page.
  // Persist verified files, but never open a surprise popup on another screen.
  a.runOnUiThread(()->{
   if(!a.isFinishing()&&!a.isDestroyed()&&owner!=null&&owner==a.dialog&&owner.isShowing())r.run();
  });
 }
 static HttpURLConnection connect(String address)throws Exception{return connect(address,0);}
 /** GitHub release hosts may serve a valid range or ignore Range and return full content. */
 static HttpURLConnection connect(String address,long offset)throws Exception{
  for(int redirects=0;redirects<=UpdateDownloadPolicy.MAX_REDIRECTS;redirects++){
   URL url=UpdateManifest.https(address);
   HttpURLConnection c=(HttpURLConnection)url.openConnection();
   c.setConnectTimeout(15000);c.setReadTimeout(20000);
   c.setInstanceFollowRedirects(false);
   c.setRequestProperty("Accept-Encoding","identity");
   if(offset>0)c.setRequestProperty("Range","bytes="+offset+"-");
   int code;
   try{code=c.getResponseCode();}catch(Exception error){c.disconnect();throw error;}
   if(code==200||(offset>0&&(code==206||code==416)))return c;
   if(code==301||code==302||code==303||code==307||code==308){
    String location=c.getHeaderField("Location");c.disconnect();
    if(location==null||redirects==UpdateDownloadPolicy.MAX_REDIRECTS)
     throw new IOException("업데이트 다운로드의 이동 횟수가 너무 많습니다.");
    String next=new URL(url,location).toExternalForm();
    if(!UpdateDownloadPolicy.safeRedirect(address,next))
     throw new IOException("안전하지 않은 업데이트 다운로드 이동 주소입니다.");
    address=next;continue;
   }
   c.disconnect();throw new IOException("업데이트 서버 응답 오류 (HTTP "+code+")");
  }
  throw new IOException("업데이트 다운로드 이동 횟수 초과");
 }
 void check(){if(!UpdateReadiness.fixedIdentity(SigningIdentity.current(a),UpdateManifest.PINNED_CERT_SHA256)){a.error(UpdateReadiness.state(SigningIdentity.current(a),UpdateManifest.PINNED_CERT_SHA256));return;}work("배포 버전 확인 중",()->{HttpURLConnection c=connect(BuildConfig.UPDATE_MANIFEST_URL);UpdateManifest m;try(InputStream in=c.getInputStream()){m=new UpdateManifest(new String(UpdateManifest.bounded(in,65536),StandardCharsets.UTF_8),a.getPackageName());}finally{c.disconnect();}if(m.minSdk>Build.VERSION.SDK_INT)throw new IOException("이 업데이트는 더 높은 Android 버전이 필요합니다.");updateProgress("배포 버전 확인 완료",-1);ui(()->{if(!UpdateVersionPolicy.isUpgrade(m.versionCode,BuildConfig.VERSION_CODE)){
    new AlertDialog.Builder(a).setTitle("업데이트 없음")
     .setMessage(UpdateVersionPolicy.currentMessage(m.versionCode,BuildConfig.VERSION_CODE))
     .setPositiveButton("확인",null).show();return;}new AlertDialog.Builder(a).setTitle(m.versionName+" 업데이트").setMessage(m.notes+"\n\n다운로드: "+String.format(Locale.KOREA,"%.1f MB",m.sizeBytes/1048576.0)).setNegativeButton("나중에",null).setPositiveButton("다운로드",(d,w)->download(m)).show();});});}
 void download(UpdateManifest m){
  if(BUSY.get()){a.toast("다른 업데이트 작업이 진행 중입니다.");return;}
  cancelRequested.set(false);
  if(cancelDownloadButton!=null){
   cancelDownloadButton.setEnabled(true);
   cancelDownloadButton.setVisibility(android.view.View.VISIBLE);
  }
  work("APK 다운로드 준비 중",()->{
   checkCancelled();
   File dir=new File(a.getCacheDir(),"updates");
   if(!dir.isDirectory()&&!dir.mkdirs())throw new IOException("업데이트 폴더 생성 실패");
   if(!UpdateStoragePolicy.fits(m.sizeBytes,dir.getUsableSpace()))
    throw new IOException("업데이트에 필요한 저장공간이 부족합니다.");
   UpdateCacheRetention.pruneStalePartials(dir,m.sha256,System.currentTimeMillis());
   // The signed manifest's SHA-256 ties resumed bytes to one exact release.
   File part=new File(dir,m.sha256+".part.apk"),ready=new File(dir,m.sha256+".apk");
   if(part.exists()&&part.length()>m.sizeBytes&&!part.delete())
    throw new IOException("잘못된 임시 다운로드를 정리하지 못했습니다.");
   MessageDigest digest=MessageDigest.getInstance("SHA-256");
   long bytes=0;
   if(part.isFile()){
    try(InputStream previous=new FileInputStream(part)){
     byte[] block=new byte[32768];int n;
     while((n=previous.read(block))!=-1){
      bytes+=n;
      if(bytes>m.sizeBytes)throw new IOException("임시 다운로드의 크기가 비정상입니다.");
      digest.update(block,0,n);
     }
    }
   }
   HttpURLConnection connection=null;
   try{
    if(bytes<m.sizeBytes){
     long requestOffset=bytes;
     connection=connect(m.apkUrl,requestOffset);
     checkCancelled();
     int code=connection.getResponseCode();
     if(code==416&&requestOffset>0){
      connection.disconnect();connection=null;
      if(!part.delete())throw new IOException("중단된 파일을 초기화하지 못했습니다.");
      requestOffset=0;bytes=0;digest=MessageDigest.getInstance("SHA-256");
      connection=connect(m.apkUrl,0);code=connection.getResponseCode();
     }
     if(code==206){
      if(!UpdateResumePolicy.validPartial(code,connection.getHeaderField("Content-Range"),
        requestOffset,m.sizeBytes)){
       if(part.exists())part.delete();
       throw new IOException("서버의 이어받기 범위가 일치하지 않습니다.");
      }
     }else if(code==200){
      // A server that ignores Range requires a clean restart, not appending.
      requestOffset=0;bytes=0;digest=MessageDigest.getInstance("SHA-256");
     }else throw new IOException("지원하지 않는 업데이트 서버 응답입니다.");
     if(requestOffset>0)updateProgress("중단된 다운로드 이어받기",UpdateProgressPolicy.percentage(bytes,m.sizeBytes));
     long deadline=SystemClock.elapsedRealtime()+180000;
     int nextProgress=(UpdateProgressPolicy.percentage(bytes,m.sizeBytes)/25+1)*25;
     try(InputStream in=connection.getInputStream();
         FileOutputStream out=new FileOutputStream(part,requestOffset>0)){
      byte[] block=new byte[32768];int n;
      while((n=in.read(block))!=-1){
       checkCancelled();
       if(SystemClock.elapsedRealtime()>deadline)
        throw new IOException("다운로드 시간이 초과됐습니다. 다시 시도해 주세요.");
       if(n>m.sizeBytes-bytes){
        part.delete();throw new IOException("APK 다운로드 크기 초과");
       }
       bytes+=n;digest.update(block,0,n);out.write(block,0,n);
       if(nextProgress<=100&&UpdateProgressPolicy.percentage(bytes,m.sizeBytes)>=nextProgress){
        updateProgress("APK 다운로드 중",nextProgress);nextProgress+=25;
       }
      }
      out.getFD().sync();
     }
    }
    checkCancelled();
    updateProgress("파일 무결성 확인 중",100);
    if(bytes!=m.sizeBytes||!hex(digest.digest()).equals(m.sha256)){
     part.delete();throw new IOException("APK 검사값 불일치. 다시 내려받아 주세요.");
    }
    updateProgress("APK 서명 확인 중",100);
    validateArchive(part,m);
    if(ready.exists()&&!ready.delete())throw new IOException("기존 다운로드 정리 실패");
    if(!part.renameTo(ready))throw new IOException("검증된 APK 저장 실패");
    if(!a.getSharedPreferences("updates",0).edit().putString("ready",m.raw).commit())
     throw new IOException("업데이트 상태 저장 실패");
    UpdateCacheRetention.prune(dir,m.sha256);
    updateProgress("검사 완료 · 설치 준비됨",100);
    ui(()->{
     refreshCachedControls();
     new AlertDialog.Builder(a).setTitle("업데이트 준비 완료")
      .setMessage("APK 검사와 서명이 확인됐습니다. 설치 전 현재 데이터의 검증된 복구 지점을 추가로 생성합니다.")
      .setNegativeButton("나중에",null)
      .setPositiveButton("설치 준비",(d,w)->requestInstall()).show();
    });
   }finally{if(connection!=null)connection.disconnect();}
  });
 }
 static String hex(byte[] bytes){StringBuilder s=new StringBuilder();for(byte b:bytes)s.append(String.format(Locale.ROOT,"%02x",b&255));return s.toString();}
 static Set<String> signers(PackageInfo p){android.content.pm.Signature[] signatures=Build.VERSION.SDK_INT>=28?(p.signingInfo==null?null:p.signingInfo.getApkContentsSigners()):p.signatures;Set<String> set=new HashSet<>();if(signatures!=null)for(android.content.pm.Signature s:signatures)set.add(s.toCharsString());return set;}
 void validateArchive(File file,UpdateManifest m)throws Exception{PackageManager pm=a.getPackageManager();int flags=Build.VERSION.SDK_INT>=28?PackageManager.GET_SIGNING_CERTIFICATES:PackageManager.GET_SIGNATURES;PackageInfo candidate=pm.getPackageArchiveInfo(file.getPath(),flags),current=pm.getPackageInfo(a.getPackageName(),flags);if(candidate==null||!a.getPackageName().equals(candidate.packageName))throw new IOException("MOTO HUB APK가 아닙니다.");long version=Build.VERSION.SDK_INT>=28?candidate.getLongVersionCode():candidate.versionCode;if(version!=m.versionCode||version<=BuildConfig.VERSION_CODE)throw new IOException("APK 버전이 올바르지 않습니다.");Set<String> old=signers(current),next=signers(candidate);if(old.isEmpty()||!old.equals(next))throw new IOException("서명키가 다른 APK입니다. 데이터 보호를 위해 설치를 차단했습니다.");if(!UpdateManifest.PINNED_CERT_SHA256.equals(SigningIdentity.current(a)))throw new IOException("현재 설치된 앱은 고정 서명 버전이 아닙니다. 백업 후 고정 서명판으로 전환해야 합니다.");}
 void requestInstall(){
  if(a.readOnly){a.error("데이터 보호 모드에서는 업데이트를 설치할 수 없습니다. 먼저 백업 상태를 확인해 주세요.");return;}
  if(!UpdateSafety.canInstall(RideService.running,RideService.autoArmed,
    RestoreRideGate.isExporting(),RestoreRideGate.isRestoring())){
   a.error(UpdateSafety.MESSAGE);return;
  }
  new AlertDialog.Builder(a).setTitle("업데이트 전 데이터 보호")
   .setMessage("설치 전에 주유·정비 등 현재 앱 데이터를 검증된 내부 복구 지점으로 저장합니다. "
    +"GPS 경로와 사진은 이 복구 지점에 포함되지 않습니다. 별도로 전체 ZIP 또는 암호화 백업을 권장합니다.")
   .setNegativeButton("취소",null)
   .setNeutralButton("암호화 전체 백업", (d,w)->a.exportEncryptedBundle())
   .setPositiveButton("복구 지점 만들고 설치",(d,w)->install()).show();
 }
 void install(){work("설치 전 APK 재검사 중",()->{
  if(a.readOnly)throw new IOException("저장 데이터 보호 상태에서는 설치를 진행하지 않습니다.");
  if(!UpdateSafety.canInstall(RideService.running,RideService.autoArmed,
     RestoreRideGate.isExporting(),RestoreRideGate.isRestoring()))
   throw new IOException(UpdateSafety.MESSAGE);
  android.content.SharedPreferences prefs=a.getSharedPreferences("updates",0);
  String raw=prefs.getString("ready","");
  if(raw==null||raw.isEmpty())throw new IOException("먼저 최신 버전을 확인하고 다운로드하세요.");
  UpdateManifest m=new UpdateManifest(raw,a.getPackageName());
  File f=new File(new File(a.getCacheDir(),"updates"),m.sha256+".apk");
  try{
   verifyFile(f,m);
  }catch(IOException damaged){
   if(raw.equals(prefs.getString("ready","")))prefs.edit().remove("ready").commit();
   ui(this::refreshCachedControls);
   throw new IOException("저장된 업데이트 파일 검사가 실패했습니다. 배포 버전 확인에서 다시 내려받으세요.",damaged);
  }
  updateProgress("APK 서명 다시 확인 중",100);
  validateArchive(f,m);
  updateProgress("앱 데이터 복구 지점 생성 중",100);
  File recovery=UpdateInstallSnapshot.create(a,a.disk);
  UpdateInstallSnapshot.recordInstaller(a,(int)m.versionCode,recovery);
  updateProgress("설치 화면 준비됨",100);
  ui(()->a.attempt(()->launchInstaller(f)));
 });}
 static void verifyFile(File f,UpdateManifest m)throws Exception{if(!f.isFile()||f.length()!=m.sizeBytes)throw new IOException("다운로드 파일이 없거나 크기가 다릅니다. 다시 다운로드하세요.");MessageDigest digest=MessageDigest.getInstance("SHA-256");try(InputStream in=new FileInputStream(f)){byte[] buffer=new byte[32768];int n;while((n=in.read(buffer))!=-1)digest.update(buffer,0,n);}if(!hex(digest.digest()).equals(m.sha256))throw new IOException("APK 검사값이 달라 설치를 차단했습니다.");}
 void launchInstaller(File f)throws Exception{if(!UpdateSafety.canInstall(RideService.running,RideService.autoArmed,RestoreRideGate.isExporting(),RestoreRideGate.isRestoring()))throw new IOException(UpdateSafety.MESSAGE);if(!a.getPackageManager().canRequestPackageInstalls()){new AlertDialog.Builder(a).setTitle("설치 허용이 필요합니다").setMessage("Android 설정에서 이 앱의 설치 요청을 허용한 뒤 돌아와 설치 버튼을 다시 눌러 주세요.").setNegativeButton("취소",null).setPositiveButton("설정 열기",(d,w)->a.attempt(()->a.startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:"+a.getPackageName()))))).show();return;}Uri uri=Uri.parse("content://"+a.getPackageName()+".updates/apk/"+f.getName());Intent i=new Intent(Intent.ACTION_INSTALL_PACKAGE).setData(uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION).putExtra(Intent.EXTRA_RETURN_RESULT,true);i.setClipData(ClipData.newRawUri("MOTO HUB update",uri));a.startActivityForResult(i,90);}
}
