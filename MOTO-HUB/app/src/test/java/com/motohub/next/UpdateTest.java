package com.motohub.next;
import org.junit.*;import static org.junit.Assert.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.*;import org.json.*;import java.io.*;import android.net.Uri;
@RunWith(RobolectricTestRunner.class) @Config(sdk=35)
public class UpdateTest {
 JSONObject metadata()throws Exception{return new JSONObject().put("packageName","com.motohub.next").put("versionCode",406).put("versionName","4.0.0-alpha6").put("sizeBytes",1000).put("minSdk",26).put("sha256",new String(new char[64]).replace('\0','a')).put("certificateSha256",UpdateManifest.PINNED_CERT_SHA256).put("apkUrl","https://example.org/moto.apk").put("notes","test");}
 UpdateManifest parse(JSONObject o)throws Exception{return new UpdateManifest(o.toString(),"com.motohub.next");}
 @Test public void validMetadata()throws Exception{assertEquals(406,parse(metadata()).versionCode);}
 @Test public void foreignPackageBlocked()throws Exception{assertThrows(IOException.class,()->parse(metadata().put("packageName","another.app")));}
 @Test public void insecureUrlBlocked(){for(String value:new String[]{"http://example.org/a.apk","file:///data/a.apk","https://name:secret@example.org/a.apk","https://example.org/a#fragment"})assertThrows(Exception.class,()->UpdateManifest.https(value));}
 @Test public void wrongSignerBlocked()throws Exception{assertThrows(IOException.class,()->parse(metadata().put("certificateSha256","b".repeat(64))));}
 @Test public void invalidHashBlocked()throws Exception{assertThrows(IOException.class,()->parse(metadata().put("sha256","abc")));}
 @Test public void oversizedDownloadBlocked()throws Exception{assertThrows(IOException.class,()->parse(metadata().put("sizeBytes",200000001)));}
 @Test public void invalidVersionBlocked()throws Exception{assertThrows(IOException.class,()->parse(metadata().put("versionCode",0)));}
 @Test public void responseLimitEnforced(){assertThrows(IOException.class,()->UpdateManifest.bounded(new ByteArrayInputStream(new byte[11]),10));}
 @Test public void sha256Format(){assertEquals("00ff10",AppUpdater.hex(new byte[]{0,(byte)255,16}));}
 @Test public void providerRejectsOtherFilesAndWrites()throws Exception{UpdateApkProvider p=Robolectric.buildContentProvider(UpdateApkProvider.class).create().get();assertThrows(FileNotFoundException.class,()->p.openFile(Uri.parse("content://com.motohub.next.updates/../../files/motohub-v3.json"),"r"));assertThrows(FileNotFoundException.class,()->p.openFile(Uri.parse("content://com.motohub.next.updates/apk/"+metadata().getString("sha256")+".apk"),"w"));}
 @Test public void unconfiguredUpdateScreenWorks(){Assume.assumeTrue(BuildConfig.UPDATE_MANIFEST_URL.isEmpty());MainActivity a=Robolectric.buildActivity(MainActivity.class).setup().get();new AppUpdater(a).show();assertTrue(a.dialog.isShowing());assertEquals("",BuildConfig.UPDATE_MANIFEST_URL);a.close();a.finish();}
 @Test public void alteredApkRejectedBeforeInstall()throws Exception{File f=File.createTempFile("update-test-",".apk");try{try(FileOutputStream out=new FileOutputStream(f)){out.write(new byte[]{1,2,3});}UpdateManifest m=parse(metadata().put("sizeBytes",3));assertThrows(IOException.class,()->AppUpdater.verifyFile(f,m));}finally{f.delete();}}
 @Test public void validFileDigestAccepted()throws Exception{File f=File.createTempFile("update-test-",".apk");try{byte[] bytes={1,2,3};try(FileOutputStream out=new FileOutputStream(f)){out.write(bytes);}String hash=AppUpdater.hex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes));AppUpdater.verifyFile(f,parse(metadata().put("sizeBytes",3).put("sha256",hash)));}finally{f.delete();}}
}
