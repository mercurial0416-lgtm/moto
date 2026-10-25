package com.motohub.next;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
/** Streams produced ZIP and detects modified payloads; no credentials or network. */
public final class BackupIntegrityTest {
 private static int assertions;
 private static void ok(boolean condition,String why){assertions++;if(!condition)throw new AssertionError(why);}
 private static void rejected(byte[] archive,String message)throws Exception{
  try{BackupIntegrity.verify(new ByteArrayInputStream(archive));throw new AssertionError("must reject: "+message);}
  catch(IOException expected){ok(true,message);}
 }
 public static void main(String[] args)throws Exception{
  File dir=Files.createTempDirectory("moto-de-backup").toFile();File rides=new File(dir,"rides");rides.mkdir();
  String name="1792122693079-12345678-1234-1234-1234-123456789abc.track";
  File track=new File(rides,name);File marker=new File(track+".done");
  try{
   Files.writeString(track.toPath(),TrackFile.point(37,127,5,1792120000000L)+TrackFile.point(37.002,127,5,1792120010000L));
   marker.createNewFile();
   ByteArrayOutputStream out=new ByteArrayOutputStream();BackupBundle.export("{\"version\":3}",dir,out);
   BackupIntegrity.Result result=BackupIntegrity.verify(new ByteArrayInputStream(out.toByteArray()));
   ok(result.tracks==1&&result.photos==0,"read-back original track count");
   boolean gpx=false;long bytes=0;
   try(ZipInputStream z=new ZipInputStream(new ByteArrayInputStream(out.toByteArray()))){ZipEntry e;while((e=z.getNextEntry())!=null){if(e.getName().endsWith(".gpx")){gpx=true;ByteArrayOutputStream content=new ByteArrayOutputStream();z.transferTo(content);bytes=content.size();ok(content.toString("UTF-8").contains("<trkpt"),"valid gpx contents");}}}
   ok(gpx&&bytes>0,"complete GPX entry");
   ByteArrayOutputStream noData=new ByteArrayOutputStream();try(ZipOutputStream zip=new ZipOutputStream(noData)){zip.putNextEntry(new ZipEntry("README-KO.txt"));zip.write("hi".getBytes());zip.closeEntry();}
   rejected(noData.toByteArray(),"missing database");
   ByteArrayOutputStream dupe=new ByteArrayOutputStream();try(ZipOutputStream zip=new ZipOutputStream(dupe)){zip.putNextEntry(new ZipEntry("MOTO-HUB-data.json"));zip.write("{}".getBytes());zip.closeEntry();zip.putNextEntry(new ZipEntry("../secret"));zip.closeEntry();}
   rejected(dupe.toByteArray(),"unsafe file name");
   // STORED test entry lets us corrupt a known byte without relying on compression.
   ByteArrayOutputStream stored=new ByteArrayOutputStream();byte[] raw="{\"version\":3}".getBytes();
   CRC32 crc=new CRC32();crc.update(raw);
   try(ZipOutputStream zip=new ZipOutputStream(stored)){
    ZipEntry json=new ZipEntry("MOTO-HUB-data.json");json.setMethod(ZipEntry.STORED);
    json.setSize(raw.length);json.setCompressedSize(raw.length);json.setCrc(crc.getValue());
    zip.putNextEntry(json);zip.write(raw);zip.closeEntry();
   }
   byte[] damaged=stored.toByteArray();int index=indexOf(damaged,raw);
   ok(index>=0,"locate ZIP payload for corruption");
   damaged[index]^=0x01;rejected(damaged,"CRC detects tampered payload");
   ok(BackupExportGate.canExport(false,false,false),"idle backup allowed");
   ok(!BackupExportGate.canExport(true,false,false),"recording prevents unsafe backup");
   ok(!BackupExportGate.canExport(false,true,false),"armed OBD prevents unsafe backup");
   ok(!BackupExportGate.canExport(false,false,true),"restore prevents conflicting backup");
   System.out.println("PASS DE export/read-back safety: "+assertions+" checks");
  }finally{marker.delete();track.delete();rides.delete();dir.delete();}
 }
 private static int indexOf(byte[] source,byte[] target){outer:for(int i=0;i<=source.length-target.length;i++){for(int j=0;j<target.length;j++)if(source[i+j]!=target[j])continue outer;return i;}return -1;}
}
