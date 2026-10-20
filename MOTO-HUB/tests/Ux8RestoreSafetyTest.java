package com.motohub.next;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

/** Restore preflight must never read/delete or overwrite out-of-app symlinked assets. */
public final class Ux8RestoreSafetyTest {
 static int checks;
 static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
 static byte[] zip()throws Exception{
  ByteArrayOutputStream out=new ByteArrayOutputStream();
  try(ZipOutputStream z=new ZipOutputStream(out,StandardCharsets.UTF_8)){
   z.putNextEntry(new ZipEntry("MOTO-HUB-data.json"));
   z.write("{}".getBytes(StandardCharsets.UTF_8));z.closeEntry();
  }
  return out.toByteArray();
 }
 static void clean(File dir)throws IOException{
  if(Files.isSymbolicLink(dir.toPath())){Files.deleteIfExists(dir.toPath());return;}
  if(Files.isDirectory(dir.toPath(),LinkOption.NOFOLLOW_LINKS)){
   File[] files=dir.listFiles();if(files!=null)for(File f:files)clean(f);
  }
  Files.deleteIfExists(dir.toPath());
 }
 public static void main(String[] args)throws Exception{
  File root=Files.createTempDirectory("moto-ux8-internal-").toFile();
  File other=Files.createTempDirectory("moto-ux8-external-").toFile();
  File external=new File(other,"important.jpg");
  byte[] important="private, must not be touched".getBytes(StandardCharsets.UTF_8);
  Files.write(external.toPath(),important);
  try{
   try(RestoreArchive.Prepared prepared=RestoreArchive.prepare(new ByteArrayInputStream(zip()),root)){
    // Attackers cannot make a recognized in-app photo point outside private storage.
    File fake=new File(root,"garage-photo-12345678-1234-1234-1234-123456789abc.jpg");
    try{
     Files.createSymbolicLink(fake.toPath(),external.toPath());
     try{prepared.requiredSpace(root);throw new AssertionError("linked photo passed preflight");}
     catch(IOException expected){checks++;}
     try{prepared.install(root,json->{throw new AssertionError("must not write JSON");});throw new AssertionError("linked photo installed");}
     catch(IOException expected){checks++;}
     check(Arrays.equals(Files.readAllBytes(external.toPath()),important),"outside file preserved after refused install");
     Files.delete(fake.toPath());
    }catch(UnsupportedOperationException|SecurityException ignored){}
    // Normal empty-asset ZIP restore still works, including rollback-space preflight.
    check(prepared.requiredSpace(root)>0,"legitimate capacity calculated");
    final int[] writes={0};
    prepared.install(root,json->{check("{}".equals(json),"writer gets intended JSON");writes[0]++;});
    check(writes[0]==1,"data writer called once");
   }
   File root2=Files.createTempDirectory("moto-ux8-dir-link-").toFile();
   try(RestoreArchive.Prepared prepared=RestoreArchive.prepare(new ByteArrayInputStream(zip()),root2)){
    Path link=new File(root2,"rides").toPath();
    try{
     Files.createSymbolicLink(link,other.toPath());
     try{prepared.requiredSpace(root2);throw new AssertionError("linked GPS folder passed");}
     catch(IOException expected){checks++;}
     Files.delete(link);
    }catch(UnsupportedOperationException|SecurityException ignored){}
   }finally{clean(root2);}
  }finally{clean(root);clean(other);}
  System.out.println("PASS UX8 restore link-safety: "+checks+" checks");
 }
}
