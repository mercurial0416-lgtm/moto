package com.motohub.next;
import java.io.*;
import java.nio.file.*;
import java.util.*;
/** Standalone encrypt/decrypt and tamper, truncation, password checks (no Android runtime). */
public final class BackupCryptoTest {
 static int checks;
 static void ok(boolean value,String message){if(!value)throw new AssertionError(message);checks++;}
 static void rejects(byte[] data,char[] password,File root)throws Exception{
  boolean thrown=false;try(InputStream in=new ByteArrayInputStream(data)){
   File temp=BackupCrypto.decryptToPrivateFile(in,password,root);temp.delete();
  }catch(IOException e){thrown=true;}
  ok(thrown,"invalid encrypted backup rejected");
 }
 public static void main(String[] args)throws Exception{
  File dir=Files.createTempDirectory("moto-cryptotest-").toFile();char[] password="Riding!2026-Strong-Secret".toCharArray();
  try {
   ok(!BackupCrypto.validPassword("short".toCharArray()),"too short");
   ok(BackupCrypto.validPassword(password),"valid password");
   byte[] original=new byte[210_987];new Random(42).nextBytes(original);
   ByteArrayOutputStream target=new ByteArrayOutputStream();
   try(OutputStream encrypted=BackupCrypto.encrypt(target,password)){
    encrypted.write(original,0,127);for(int i=127;i<original.length;i+=8191)encrypted.write(original,i,Math.min(8191,original.length-i));
   }
   byte[] bytes=target.toByteArray();ok(bytes.length>original.length,"tag overhead exists");
   File decoded=BackupCrypto.decryptToPrivateFile(new ByteArrayInputStream(bytes),password,dir);
   ok(Arrays.equals(original,Files.readAllBytes(decoded.toPath())),"streamed multi-chunk exact roundtrip");decoded.delete();
   rejects(bytes,"Different!Password-123".toCharArray(),dir);
   byte[] tampered=bytes.clone();tampered[80]^=1;rejects(tampered,password,dir);
   tampered=bytes.clone();tampered[tampered.length-3]^=1;rejects(tampered,password,dir);
   rejects(Arrays.copyOf(bytes,bytes.length-17),password,dir);
   tampered=Arrays.copyOf(bytes,bytes.length+2);tampered[tampered.length-1]=1;rejects(tampered,password,dir);
   tampered=bytes.clone();tampered[32]=(byte)0xff;rejects(tampered,password,dir);
   tampered=bytes.clone();tampered[0]='X';rejects(tampered,password,dir);
   ByteArrayOutputStream emptyTarget=new ByteArrayOutputStream();try(OutputStream o=BackupCrypto.encrypt(emptyTarget,password)){}
   decoded=BackupCrypto.decryptToPrivateFile(new ByteArrayInputStream(emptyTarget.toByteArray()),password,dir);
   ok(decoded.length()==0,"zero length roundtrip");decoded.delete();
   ok(Objects.requireNonNull(dir.listFiles()).length==0,"no leaked plaintext temporary files");
   System.out.println("PASS BackupCrypto "+checks+" assertions");
  }finally{Arrays.fill(password,'\0');dir.delete();}
 }
}
