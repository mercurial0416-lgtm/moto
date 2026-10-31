package com.motohub.next;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.DocumentsContract;
import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Export to the user-selected SAF document, then read back and prove identical bytes.
 * This runs strictly on a background worker, never the main/UI thread. */
final class VerifiedDocumentExport {
 private VerifiedDocumentExport(){}
 interface Writer {int write(OutputStream target)throws Exception;}
 private static MessageDigest sha256()throws IOException {
  try{return MessageDigest.getInstance("SHA-256");}
  catch(NoSuchAlgorithmException error){throw new IOException("파일 검증 기능을 시작할 수 없습니다.",error);}
 }
 private static final class CheckedOutput extends FilterOutputStream {
  final MessageDigest digest;
  final long maxBytes;
  long size=0;
  CheckedOutput(OutputStream target,MessageDigest digest,long maxBytes) {
   super(target);this.digest=digest;this.maxBytes=maxBytes;
  }
  @Override public void write(int b)throws IOException {
   if(size>=maxBytes)throw new IOException("내보내기 파일 용량 제한을 초과했습니다.");
   out.write(b);digest.update((byte)b);size++;
  }
  @Override public void write(byte[] b,int off,int len)throws IOException {
   if(off<0||len<0||off>b.length-len)throw new IndexOutOfBoundsException();
   if(len>maxBytes-size)throw new IOException("내보내기 파일 용량 제한을 초과했습니다.");
   out.write(b,off,len);digest.update(b,off,len);size+=len;
  }
 }
 static int save(ContentResolver resolver,Uri uri,long maxBytes,Writer source)throws Exception {
  if(resolver==null||uri==null||source==null||maxBytes<=0)throw new IOException("내보내기 대상이 올바르지 않습니다.");
  MessageDigest expected=sha256();
  long written=0;
  int result;
  boolean verified=false;
  try{
   OutputStream target=resolver.openOutputStream(uri,"wt");
   if(target==null)throw new IOException("파일 저장소를 열 수 없습니다.");
   CheckedOutput out=new CheckedOutput(new BufferedOutputStream(target),expected,maxBytes);
   try(out){result=source.write(out);out.flush();}
   written=out.size;
   if(written==0)throw new IOException("빈 파일은 백업으로 저장할 수 없습니다.");
   MessageDigest actual=sha256();
   long bytes=0;byte[] buffer=new byte[32768];
   try(InputStream input=resolver.openInputStream(uri)){
    if(input==null)throw new IOException("저장한 파일을 검증용으로 다시 열 수 없습니다.");
    int n;while((n=input.read(buffer))!=-1){
     bytes+=n;
     if(bytes>maxBytes)throw new IOException("저장된 파일 용량이 변했습니다.");
     actual.update(buffer,0,n);
    }
   }
   if(bytes!=written||!MessageDigest.isEqual(expected.digest(),actual.digest()))
    throw new IOException("저장한 문서와 원본 데이터가 일치하지 않습니다.");
   verified=true;
   return result;
  }catch(Exception failure){
   if(!verified){
    boolean deleted=false;
    try{deleted=DocumentsContract.deleteDocument(resolver,uri);}catch(Exception ignored){}
    if(!deleted)throw new IOException("내보내기 검증 실패. 부분 저장 파일을 삭제하지 못했습니다. 저장 폴더에서 해당 파일을 직접 확인하세요.",failure);
   }
   throw failure;
  }
 }
}
