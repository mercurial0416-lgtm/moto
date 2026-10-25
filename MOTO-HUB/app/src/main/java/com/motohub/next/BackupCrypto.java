package com.motohub.next;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/** Password-protected, chunk-authenticated full backups. Never persists the passphrase. */
final class BackupCrypto {
 private BackupCrypto(){}
 static final byte[] MAGIC="MOTOENC2".getBytes(StandardCharsets.US_ASCII);
 private static final int SALT_LEN=16, PREFIX_LEN=8, TAG_LEN=16, CHUNK=64*1024, ITERATIONS=210_000;
 private static final int MAX_ZIP_BYTES=300_000_000;

 static boolean validPassword(char[] password){return password!=null&&password.length>=12&&password.length<=256;}
 private static SecretKeySpec derive(char[] password,byte[] salt)throws GeneralSecurityException{
  if(!validPassword(password))throw new GeneralSecurityException("비밀번호는 12~256자여야 합니다.");
  PBEKeySpec spec=new PBEKeySpec(password,salt,ITERATIONS,256);
  try {return new SecretKeySpec(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded(),"AES");}
  finally{spec.clearPassword();}
 }
 private static byte[] iv(byte[] prefix,int index){byte[] iv=new byte[12];System.arraycopy(prefix,0,iv,0,PREFIX_LEN);iv[8]=(byte)(index>>>24);iv[9]=(byte)(index>>>16);iv[10]=(byte)(index>>>8);iv[11]=(byte)index;return iv;}
 private static void aad(Cipher cipher,byte[] salt,byte[] prefix,int index,int plainLength){
  cipher.updateAAD(MAGIC);cipher.updateAAD(salt);cipher.updateAAD(prefix);
  cipher.updateAAD(new byte[]{(byte)(index>>>24),(byte)(index>>>16),(byte)(index>>>8),(byte)index,
   (byte)(plainLength>>>24),(byte)(plainLength>>>16),(byte)(plainLength>>>8),(byte)plainLength});
 }
 private static Cipher cipher(int mode,SecretKeySpec key,byte[] salt,byte[] prefix,int index,int length)throws GeneralSecurityException{
  Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(mode,key,new GCMParameterSpec(TAG_LEN*8,iv(prefix,index)));
  aad(c,salt,prefix,index,length);return c;
 }
 static OutputStream encrypt(OutputStream destination,char[] password)throws IOException{
  if(destination==null||!validPassword(password))throw new IOException("암호는 12~256자로 입력해 주세요.");
  byte[] salt=new byte[SALT_LEN],prefix=new byte[PREFIX_LEN];SecureRandom random=new SecureRandom();random.nextBytes(salt);random.nextBytes(prefix);
  final SecretKeySpec key;
  try{key=derive(password,salt);}catch(GeneralSecurityException e){throw new IOException("암호화 키 생성 실패",e);}
  DataOutputStream sink=new DataOutputStream(new BufferedOutputStream(destination));
  try{sink.write(MAGIC);sink.write(salt);sink.write(prefix);}catch(IOException e){try{sink.close();}catch(IOException ignored){}throw e;}
  return new OutputStream(){
   final byte[] buffer=new byte[CHUNK];int pending=0,index=0;boolean closed=false;
   private void ensureOpen()throws IOException {if(closed)throw new IOException("암호화 스트림이 종료됐습니다.");}
   private void record(int size)throws IOException{
    if(index<0)throw new IOException("백업 암호화 크기를 초과했습니다.");
    try{
     byte[] encrypted=cipher(Cipher.ENCRYPT_MODE,key,salt,prefix,index,size).doFinal(buffer,0,size);
     sink.writeInt(size);sink.write(encrypted);index++;
    }catch(GeneralSecurityException e){throw new IOException("백업 암호화 오류",e);}
   }
   @Override public void write(int b)throws IOException{ensureOpen();buffer[pending++]=(byte)b;if(pending==CHUNK){record(pending);pending=0;}}
   @Override public void write(byte[] source,int off,int len)throws IOException{
    if(source==null||off<0||len<0||off>source.length-len)throw new IndexOutOfBoundsException();ensureOpen();
    while(len>0){int n=Math.min(len,CHUNK-pending);System.arraycopy(source,off,buffer,pending,n);pending+=n;off+=n;len-=n;if(pending==CHUNK){record(pending);pending=0;}}
   }
   @Override public void flush()throws IOException{ensureOpen();sink.flush();}
   @Override public void close()throws IOException{
    if(closed)return;closed=true;
    try {if(pending>0){record(pending);pending=0;}record(0);sink.flush();}
    finally{Arrays.fill(buffer,(byte)0);sink.close();}
   }
  };
 }
 private static int readFully(InputStream in,byte[] into)throws IOException{
  int read=0;while(read<into.length){int n=in.read(into,read,into.length-read);if(n<0)throw new IOException("암호화 백업이 잘렸습니다.");read+=n;}return read;
 }
 /** Complete authentication BEFORE passing any untrusted ZIP to the restore pipeline. */
 static File decryptToPrivateFile(InputStream encrypted,char[] password,File privateDir)throws IOException{
  if(encrypted==null||!validPassword(password))throw new IOException("비밀번호는 12~256자로 입력해 주세요.");
  if(privateDir==null||!privateDir.isDirectory())throw new IOException("임시 복원 폴더가 없습니다.");
  DataInputStream in=new DataInputStream(new BufferedInputStream(encrypted));
  byte[] magic=new byte[MAGIC.length],salt=new byte[SALT_LEN],prefix=new byte[PREFIX_LEN];
  readFully(in,magic);if(!Arrays.equals(magic,MAGIC))throw new IOException("MOTO HUB 암호화 백업 파일이 아닙니다.");
  readFully(in,salt);readFully(in,prefix);
  final SecretKeySpec key;
  try{key=derive(password,salt);}catch(GeneralSecurityException e){throw new IOException("암호화 키 생성 실패",e);}
  File temp=File.createTempFile("moto-secure-", ".zip",privateDir);
  boolean valid=false;long total=0;int index=0;
  try(OutputStream out=new BufferedOutputStream(new FileOutputStream(temp))){
   while(true){
    int size;
    try{size=in.readInt();}catch(EOFException e){throw new IOException("암호화 백업의 종료 인증값이 없습니다.",e);}
    if(size<0||size>CHUNK||index<0)throw new IOException("암호화 백업 데이터가 손상됐습니다.");
    if(total>MAX_ZIP_BYTES-size)throw new IOException("암호화 백업 크기 제한을 초과했습니다.");
    byte[] record=new byte[size+TAG_LEN];readFully(in,record);
    byte[] plain;
    try{plain=cipher(Cipher.DECRYPT_MODE,key,salt,prefix,index,size).doFinal(record);}
    catch(AEADBadTagException e){throw new IOException("암호가 틀렸거나 백업이 손상됐습니다.",e);}
    catch(GeneralSecurityException e){throw new IOException("암호화 백업 검증 실패",e);}
    if(size==0){if(plain.length!=0||in.read()!=-1)throw new IOException("암호화 백업 뒤에 알 수 없는 데이터가 있습니다.");valid=true;break;}
    if(plain.length!=size)throw new IOException("암호화 백업 길이가 다릅니다.");
    out.write(plain);total+=size;index++;
   }
  }finally{if(!valid)Files.deleteIfExists(temp.toPath());}
  return temp;
 }
}
