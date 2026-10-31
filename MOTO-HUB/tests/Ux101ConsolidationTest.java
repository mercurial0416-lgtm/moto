package com.motohub.next;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

/** Split backup and quarantined GPS must fail closed rather than certify partial files. */
public final class Ux101ConsolidationTest {
 private static void check(boolean pass,String description){if(!pass)throw new AssertionError(description);}
 private static void clean(Path dir)throws Exception{
  if(!Files.exists(dir,LinkOption.NOFOLLOW_LINKS))return;
  try(java.util.stream.Stream<Path> s=Files.walk(dir)){
   for(Path file:s.sorted(Comparator.reverseOrder()).toArray(Path[]::new))Files.deleteIfExists(file);
  }
 }
 private static void expectFailure(RunnableWithIo action,String description)throws Exception{
  try{action.run();throw new AssertionError(description);}
  catch(IOException expected){check(expected.getMessage()!=null,description+" message");}
 }
 interface RunnableWithIo{void run()throws Exception;}
 public static void main(String[] args)throws Exception{
  Path root=Files.createTempDirectory("moto-ux101-");
  try{
   check(BatchArchiveExport.plan(root.toFile()).parts.isEmpty(),"new garage has no split archives");
   check(BatchArchiveExport.listIfPresent(root.resolve("missing").toFile(),(d,n)->true).length==0,"missing optional folder is empty");
   Path rides=Files.createDirectory(root.resolve("rides"));
   String track="1761899000000-"+UUID.randomUUID()+".track";
   Path ride=rides.resolve(track);
   Files.write(ride,"37,127,5,1761899000000\n37.1,127.1,5,1761899010000\n".getBytes(StandardCharsets.UTF_8));
   Files.createFile(rides.resolve(track+".done"));
   Path media=Files.createDirectory(root.resolve("moto-year-photos"));
   Path model=media.resolve("abcdef0123456789abcdef01.jpg");
   byte[] jpg={(byte)255,(byte)216,4,5,(byte)255,(byte)217};
   Files.write(model,jpg);
   Files.writeString(media.resolve(model.getFileName().toString()+".json"),"{\"userSelected\":true}");
   BatchArchiveExport.Plan plan=BatchArchiveExport.plan(root.toFile());
   check(plan.parts.size()==2,"GPS and MEDIA each get own ZIP");
   for(BatchArchiveExport.Part part:plan.parts){
    ByteArrayOutputStream out=new ByteArrayOutputStream();
    BatchArchiveExport.write(part,"{\"version\":3}",UUID.randomUUID().toString(),out);
    byte[] zip=out.toByteArray();
    BackupIntegrity.Result checked=BackupIntegrity.verify(new ByteArrayInputStream(zip));
    check(checked.entryNames.equals(part.expectedEntries()),"all entries verified");
    BatchArchiveExport.verifySources(part,new ByteArrayInputStream(zip));
    if(part.kind.equals("MEDIA")){
     Files.write(model,new byte[]{(byte)255,(byte)216,8,9,(byte)255,(byte)217});
     expectFailure(()->BatchArchiveExport.verifySources(part,new ByteArrayInputStream(zip)),
      "same-sized changed JPEG must be detected by source SHA-256");
     Files.write(model,jpg);
    }
   }
   Path trash=Files.createDirectories(root.resolve("rides-trash"));
   Files.writeString(trash.resolve("strange.txt"),"not a ride");
   expectFailure(()->RideTrash.entries(root.toFile()),"unknown trash asset cannot disappear silently");
   Files.delete(trash.resolve("strange.txt"));
   Path folder=Files.createDirectory(trash.resolve("1761899000001-"+UUID.randomUUID()+".track"));
   expectFailure(()->RideTrash.entries(root.toFile()),"missing quarantined track must fail closed");
   Files.write(folder.resolve(folder.getFileName()),new byte[]{1,2,3});
   Files.writeString(folder.resolve("unexpected.txt"),"oops");
   expectFailure(()->RideTrash.entries(root.toFile()),"unknown inner sidecar cannot be ignored");
   Files.delete(folder.resolve("unexpected.txt"));
   check(RideTrash.entries(root.toFile()).size()==1,"valid quarantine is visible");

   File unreadable=new File(root.toString()){
    @Override public File[] listFiles(java.io.FilenameFilter filter){return null;}
   };
   expectFailure(()->BatchArchiveExport.listIfPresent(unreadable,(d,n)->true),
    "unreadable existing source folder must throw IOException");
   System.out.println("PASS UX101 split ZIP readback SHA-256, source mutation, read errors and GPS quarantine");
  }finally{clean(root);}
 }
}
