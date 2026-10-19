package com.motohub.next;
import java.io.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;

/** Regression: 10K rides must round-trip through backup, verifier and restore. */
public final class BackupTenThousandTest {
 private static int checks;
 private static void ok(boolean yes,String message){checks++;if(!yes)throw new AssertionError(message);}
 private static byte[] zip(Map<String,byte[]> entries)throws Exception{
  ByteArrayOutputStream out=new ByteArrayOutputStream();try(ZipOutputStream z=new ZipOutputStream(out)){
   for(Map.Entry<String,byte[]> e:entries.entrySet()){
    z.putNextEntry(new ZipEntry(e.getKey()));z.write(e.getValue());z.closeEntry();
   }
  }return out.toByteArray();
 }
 private static void reject(byte[] archive,String why)throws Exception{
  try{BackupIntegrity.verify(new ByteArrayInputStream(archive));throw new AssertionError("Accepted: "+why);}
  catch(IOException expected){checks++;}
 }
 public static void main(String[] args)throws Exception{
  File from=Files.createTempDirectory("moto-df-10k-from").toFile();File rides=new File(from,"rides");rides.mkdir();
  File restore=Files.createTempDirectory("moto-df-restore").toFile();
  try {
   String trackName="1792122693079-12345678-1234-1234-1234-123456789abc.track";
   String point=TrackFile.point(37,127,5,1792120000000L);
   Map<String,byte[]> bad=new LinkedHashMap<>();bad.put("MOTO-HUB-data.json","{}".getBytes(StandardCharsets.UTF_8));
   bad.put("rides/"+trackName+".bike","x".getBytes(StandardCharsets.UTF_8));reject(zip(bad),"orphan sidecar");
   bad.clear();bad.put("MOTO-HUB-data.json","{}".getBytes(StandardCharsets.UTF_8));
   bad.put("random.txt","x".getBytes());reject(zip(bad),"unrecognized path");
   bad.clear();bad.put("MOTO-HUB-data.json","{}".getBytes(StandardCharsets.UTF_8));
   bad.put("rides/"+trackName,point.getBytes(StandardCharsets.UTF_8));
   bad.put("rides/"+trackName+".bike","bogus".getBytes());reject(zip(bad),"invalid bike binding");
   bad.put("rides/"+trackName+".bike","12345678-1234-1234-1234-123456789abc".getBytes());
   bad.put("rides/"+trackName+".category","secret".getBytes());reject(zip(bad),"invalid ride category");
   bad.remove("rides/"+trackName+".category");
   bad.put("rides/"+trackName+".done","x".getBytes());reject(zip(bad),"nonempty state marker");
   bad.remove("rides/"+trackName+".done");
   bad.put("rides/"+trackName+".unmerged",new byte[0]);
   reject(zip(bad),"unmerged requires completed ride");
   bad.remove("rides/"+trackName+".unmerged");
   ok(BackupIntegrity.verify(new ByteArrayInputStream(zip(bad))).tracks==1,"valid single archive");
   // Genuine interrupted recording with no complete line must survive restore.
   bad.put("rides/"+trackName,"37.0,127.0,5,17921200000".getBytes(StandardCharsets.UTF_8));
   byte[] interrupted=zip(bad);
   ok(BackupIntegrity.verify(new ByteArrayInputStream(interrupted)).tracks==1,"interrupted bytes verified");
   try(RestoreArchive.Prepared prepared=RestoreArchive.prepare(new ByteArrayInputStream(interrupted),restore)){
    ok(prepared.rides==1,"interrupted ride accepted for preservation");
    prepared.install(restore,json->{});
    ok(Arrays.equals(Files.readAllBytes(new File(restore,"rides/"+trackName).toPath()),bad.get("rides/"+trackName)),"partial original bytes preserved");
   }
   bad.put("rides/"+trackName+".done",new byte[0]);
   try{RestoreArchive.prepare(new ByteArrayInputStream(zip(bad)),restore);throw new AssertionError("completed partial accepted");}
   catch(IOException expected){checks++;}
   bad.clear();bad.put("MOTO-HUB-data.json","{}".getBytes(StandardCharsets.UTF_8));
   for(int i=0;i<BackupFormat.MAX_TRACKS;i++){
    String base=String.format(Locale.ROOT,"%013d-%08x-1234-1234-1234-%012x.track",1792000000000L+i,i,i);
    bad.put("rides/"+base,point.getBytes(StandardCharsets.UTF_8));
    bad.put("rides/"+base+".done",new byte[0]);
   }
   byte[] full=zip(bad);BackupIntegrity.Result result=BackupIntegrity.verify(new ByteArrayInputStream(full));
   ok(result.tracks==10_000,"10,000 track entries accepted in verifier");
   // Full prepare stages all files. This verifies the entry limit and content validators.
   try(RestoreArchive.Prepared prepared=RestoreArchive.prepare(new ByteArrayInputStream(full),restore)){
    ok(prepared.rides==10_000,"10,000 ride backups accepted for restore");
   }
   // Export the actual file-tree with >2500 rides (previous hard stop).
   for(int i=0;i<3001;i++){
    String base=String.format(Locale.ROOT,"%013d-%08x-1234-1234-1234-%012x.track",1792000000000L+i,i,i);
    Files.write(new File(rides,base).toPath(),point.getBytes(StandardCharsets.UTF_8));
   }
   ByteArrayOutputStream exported=new ByteArrayOutputStream();BackupBundle.export("{}",from,exported);
   ok(BackupIntegrity.verify(new ByteArrayInputStream(exported.toByteArray())).tracks==3001,"real file export above 2500 rides");
   System.out.println("PASS DF 10k archive capacity and safety: "+checks+" checks");
  }finally {cleanup(from);cleanup(restore);}
 }
 static void cleanup(File f)throws IOException{if(f.isDirectory()){File[] children=f.listFiles();if(children!=null)for(File c:children)cleanup(c);}Files.deleteIfExists(f.toPath());}
}
