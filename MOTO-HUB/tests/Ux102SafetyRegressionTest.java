package com.motohub.next;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.Arrays;

/** Pure-Java safeguards for rotation-safe SAF state and fail-closed GPS listing. */
public final class Ux102SafetyRegressionTest {
 static void expect(boolean condition,String message){if(!condition)throw new AssertionError(message);}
 static void fails(RunnableWithIo task,String reason)throws Exception{
  try{task.run();throw new AssertionError(reason);}
  catch(IOException expected){expect(expected.getMessage()!=null,reason+" message");}
 }
 interface RunnableWithIo{void run()throws Exception;}
 public static void main(String[] args)throws Exception{
  char[] password="twelve-char-password".toCharArray();
  BackupPickerTransfer.begin(47,"{\"vehicles\":[]}",password);
  expect(Arrays.equals(password,new char[password.length]),"original password must be scrubbed");
  expect(BackupPickerTransfer.take(45)==null,"wrong picker result must not steal state");
  // Recreated Activity accesses the exact same process-only transfer.
  BackupPickerTransfer.Entry result=BackupPickerTransfer.take(47);
  expect(result!=null&&result.request==47&&result.json.contains("vehicles"),"picker transfer survives rotation");
  expect(new String(result.password).equals("twelve-char-password"),"restore password intact until handoff");
  expect(BackupPickerTransfer.take(47)==null,"picker result is consumed exactly once");
  result.clearPassword();
  expect(Arrays.equals(result.password,new char[result.password.length]),"worker can scrub password after use");
  char[] cancelled="rotate-cancelled".toCharArray();
  BackupPickerTransfer.begin(48,"",cancelled);
  BackupPickerTransfer.cancel(48);
  expect(BackupPickerTransfer.take(48)==null,"cancel must erase pending password");
  BackupPickerTransfer.begin(41,"{\"records\":1}",null);
  BackupPickerTransfer.Entry json=BackupPickerTransfer.take(41);
  expect(json!=null&&json.json.contains("records"),"JSON export retains snapshot");
  expect(json.password==null,"plaintext export does not attach a password");

  char[] abandoned="abandoned-secret-123".toCharArray();
  BackupPickerTransfer.begin(48,"",abandoned);
  BackupPickerTransfer.cancelAll();
  expect(BackupPickerTransfer.take(48)==null,"Activity finish scrubs abandoned password");

  Path root=Files.createTempDirectory("moto-ux102-");
  try{
   expect(TrackFile.tracks(root.toFile()).isEmpty(),"fresh directory returns empty safe list");
   Path rides=root.resolve("rides");
   String name="1761899000000-"+UUID.randomUUID()+".track";
   Path original=rides.resolve(name);
   Files.writeString(original,"37,127,5,1761899000000\n");
   expect(TrackFile.tracks(root.toFile()).size()==1,"valid GPS file listed");
   Path bad=rides.resolve("1761899000001-"+UUID.randomUUID()+".track");
   Files.createSymbolicLink(bad,original);
   fails(()->TrackFile.tracks(root.toFile()),"symlink cannot be silently hidden");
   Files.delete(bad);
   expect(TrackFile.tracks(root.toFile()).size()==1,"GPS file remains after refusal");
   // A non-directory is never treated as an empty history.
   Path broken=Files.createDirectory(root.resolve("broken"));
   Files.createFile(broken.resolve("rides"));
   fails(()->TrackFile.tracks(broken.toFile()),"non-directory cannot appear as 0 records");
   System.out.println("PASS UX102 picker rotation/cancellation/secret clearing and GPS fail-closed listing");
  }finally{
   try(java.util.stream.Stream<Path> walk=Files.walk(root)){
    for(Path file:walk.sorted(Comparator.reverseOrder()).toArray(Path[]::new))Files.deleteIfExists(file);
   }
  }
 }
}
