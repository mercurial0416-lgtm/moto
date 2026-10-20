package com.motohub.next;
public final class UpdateDownloadPolicyTest {
 static int count;
 static void check(boolean value){if(!value)throw new AssertionError("case "+count);count++;}
 public static void main(String[] args){
  String g="https://github.com/example/repo/releases/download/v1/app.apk";
  check(UpdateDownloadPolicy.MAX_REDIRECTS==4);
  check(UpdateDownloadPolicy.safeRedirect(g,"https://release-assets.githubusercontent.com/file?token=a"));
  check(UpdateDownloadPolicy.safeRedirect(g,"https://objects.githubusercontent.com/file?token=a"));
  check(UpdateDownloadPolicy.safeRedirect(g,"https://github.com/other/resource"));
  check(!UpdateDownloadPolicy.safeRedirect(g,"http://release-assets.githubusercontent.com/file"));
  check(!UpdateDownloadPolicy.safeRedirect(g,"https://attacker.net/file"));
  check(!UpdateDownloadPolicy.safeRedirect(g,"https://release-assets.githubusercontent.com:443/file"));
  check(!UpdateDownloadPolicy.safeRedirect(g,"https://user:pass@github.com/file"));
  check(!UpdateDownloadPolicy.safeRedirect(g,"https://github.com/file#fragment"));
  check(!UpdateDownloadPolicy.safeRedirect(g,"file:///etc/passwd"));
  check(!UpdateDownloadPolicy.safeRedirect(g,"https://release-assets.githubusercontent.com.evil.example/file"));
  System.out.println("PASS DI signed-update redirect policy "+count+" checks");
 }
}
