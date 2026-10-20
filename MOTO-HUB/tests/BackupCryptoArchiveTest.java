package com.motohub.next;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Password-protected real ZIP: authenticated before parsing, restores byte-exact tracks. */
public final class BackupCryptoArchiveTest {
 static int checks;
 static void ok(boolean cond,String msg){if(!cond)throw new AssertionError(msg);checks++;}
 public static void main(String[] args)throws Exception{
  File root=Files.createTempDirectory("moto-cryptobundle-").toFile();
  File from=new File(root,"source"),to=new File(root,"dest"),privateDir=new File(root,"private");
  from.mkdirs();to.mkdirs();privateDir.mkdirs();
  char[] pw="Moto Ride Backup 2026!".toCharArray();
  try {
   String name="1792122693079-12345678-1234-1234-1234-123456789abc.track";
   File rides=new File(from,"rides");rides.mkdir();
   byte[] track=TrackFile.point(37.41,127.49,5,1792120000000L).getBytes(StandardCharsets.UTF_8);
   Files.write(new File(rides,name).toPath(),track);
   Files.write(new File(rides,name+".done").toPath(),new byte[0]);
   Files.write(new File(from,"garage-photo-12345678-1234-1234-1234-123456789abc.jpg").toPath(),new byte[]{-1,-40,0,0,-1,-39});
   ByteArrayOutputStream output=new ByteArrayOutputStream();
   try(OutputStream encrypted=BackupCrypto.encrypt(output,pw)){
    ok(BackupBundle.export("{}",from,encrypted)>=3,"actual export");
   }
   byte[] raw=output.toByteArray();ok(!new String(raw,StandardCharsets.ISO_8859_1).contains("MOTO-HUB-data.json"),"plaintext filename absent");
   File temp=BackupCrypto.decryptToPrivateFile(new ByteArrayInputStream(raw),pw,privateDir);
   try{
    try(InputStream check=new FileInputStream(temp)){
     BackupIntegrity.Result verified=BackupIntegrity.verify(check);
     ok(verified.tracks==1&&verified.photos==1,"encrypted readback validated");
    }
    try(InputStream check=new FileInputStream(temp);RestoreArchive.Prepared prepared=RestoreArchive.prepare(check,to)){
     ok(prepared.rides==1&&prepared.photos==1,"staged restore of authenticated archive");
     prepared.install(to,json->ok("{}".equals(json),"json unchanged"));
     ok(Arrays.equals(Files.readAllBytes(new File(to,"rides/"+name).toPath()),track),"original GPS identical");
    }
   }finally{Files.deleteIfExists(temp.toPath());}
   ok(Objects.requireNonNull(privateDir.list()).length==0,"private temporary data removed");
   byte[] broken=raw.clone();broken[broken.length/2]^=1;
   boolean failed=false;
   try{BackupCrypto.decryptToPrivateFile(new ByteArrayInputStream(broken),pw,privateDir);}
   catch(IOException expected){failed=true;}
   ok(failed,"tamper rejected before parsing ZIP");
   ok(Objects.requireNonNull(privateDir.list()).length==0,"tamper no temp leak");
   System.out.println("PASS authenticated full ZIP, "+checks+" assertions");
  } finally {Arrays.fill(pw,'\0');cleanup(root);}
 }
 static void cleanup(File f)throws IOException{if(f.isDirectory()){File[] c=f.listFiles();if(c!=null)for(File n:c)cleanup(n);}Files.deleteIfExists(f.toPath());}
}
