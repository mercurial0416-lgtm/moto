package com.motohub.next;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import org.junit.Test;
import static org.junit.Assert.*;

public final class GpxImportIntegrityTest {
 static final String FIRST="2020-01-02T03:04:05Z",SECOND="2020-01-02T03:04:15Z";
 String point(String time){return "<trkpt lat='37' lon='127'>"+time+"</trkpt>";}
 String document(String points){return "<gpx><trk><trkseg>"+points+"</trkseg></trk></gpx>";}
 GpxImporter.Imported read(String xml,File dir)throws Exception{return GpxImporter.importTrack(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)),dir);}
 void clean(File dir)throws Exception{File[] files=dir.listFiles();if(files!=null)for(File f:files)Files.deleteIfExists(f.toPath());Files.deleteIfExists(dir.toPath());}
 void rejected(String xml)throws Exception{
  File dir=Files.createTempDirectory("moto-gpx-invalid").toFile();
  try{try{read(xml,dir);fail("invalid GPX must not be committed");}catch(IOException|org.xml.sax.SAXException expected){}assertEquals(0,dir.list().length);}
  finally{clean(dir);}
 }
 @Test public void importedHistoryContinuesToUseTheRecordedPointTime()throws Exception{
  File dir=Files.createTempDirectory("moto-gpx-date").toFile();
  try{
   GpxImporter.Imported result=read(document(point("<time>"+FIRST+"</time>")+point("<time>"+SECOND+"</time>")),dir);
   assertEquals(Instant.parse(FIRST).toEpochMilli(),RideHistoryLabel.startTime(new File(dir,result.name)).longValue());
   assertEquals(2,result.count);assertTrue(new File(dir,result.name+".imported").isFile());
  }finally{clean(dir);}
 }
 @Test public void nestedExtensionTimeDoesNotCreateAFabricatedRideTimestamp()throws Exception{
  rejected(document(point("<extensions><time>"+FIRST+"</time></extensions>")+point("<extensions><time>"+SECOND+"</time></extensions>")));
 }
 @Test public void duplicateDirectTimesAreRejectedRatherThanSilentlyOverwritten()throws Exception{
  rejected(document(point("<time>2019-01-01T00:00:00Z</time><time>"+FIRST+"</time>")+point("<time>"+SECOND+"</time>")));
 }
 @Test public void aNonGpxRootCannotBePromotedToARide()throws Exception{
  rejected("<unrelated><trk><trkseg>"+point("<time>"+FIRST+"</time>")+point("<time>"+SECOND+"</time>")+"</trkseg></trk></unrelated>");
 }
 @Test public void foreignNamespacePointsCannotBePromotedToARide()throws Exception{
  rejected("<gpx xmlns:x='urn:unrelated'><trk><trkseg><x:trkpt lat='37' lon='127'><time>"+FIRST+"</time></x:trkpt><x:trkpt lat='37' lon='127'><time>"+SECOND+"</time></x:trkpt></trkseg></trk></gpx>");
 }
 @Test public void legitimateExtensionTimeDoesNotOverrideTheDirectPointTime()throws Exception{
  File dir=Files.createTempDirectory("moto-gpx-extension").toFile();
  try{
   String xml="<gpx xmlns:x='urn:extension'><trk><trkseg>"+point("<time>"+FIRST+"</time><extensions><x:time>2099-01-01T00:00:00Z</x:time></extensions>")+point("<time>"+SECOND+"</time>")+"</trkseg></trk></gpx>";
   GpxImporter.Imported result=read(xml,dir);long[] first={0};
   TrackFile.read(new File(dir,result.name),(a,b,c,t)->{if(first[0]==0)first[0]=t;});
   assertEquals(Instant.parse(FIRST).toEpochMilli(),first[0]);
  }finally{clean(dir);}
 }
 @Test public void linkedDestinationDirectoryCannotReceiveImportedFiles()throws Exception{
  File root=Files.createTempDirectory("moto-gpx-link").toFile(),actual=new File(root,"actual"),link=new File(root,"linked");assertTrue(actual.mkdir());Files.createSymbolicLink(link.toPath(),actual.toPath());
  try{
   try{read(document(point("<time>"+FIRST+"</time>")+point("<time>"+SECOND+"</time>")),link);fail();}catch(IOException expected){}
   assertEquals(0,actual.list().length);
  }finally{Files.deleteIfExists(link.toPath());clean(actual);Files.delete(root.toPath());}
 }
 @Test public void supportedNamespacesAndRoutePointsRemainCompatible()throws Exception{
  for(String namespace:new String[]{"","http://www.topografix.com/GPX/1/0","http://www.topografix.com/GPX/1/1"}){
   File dir=Files.createTempDirectory("moto-gpx-namespace").toFile();
   try{
    String xml="<gpx xmlns='"+namespace+"'><rte><rtept lat='37' lon='127'><time>"+FIRST+"</time></rtept><rtept lat='37.001' lon='127'><time>"+SECOND+"</time></rtept></rte></gpx>";
    GpxImporter.Imported imported=read(xml,dir);assertEquals(2,imported.count);
    assertEquals(Instant.parse(FIRST).toEpochMilli(),RideHistoryLabel.startTime(new File(dir,imported.name)).longValue());
   }finally{clean(dir);}
  }
 }
 @Test public void prefixedStandardGpxElementsRemainCompatible()throws Exception{
  File dir=Files.createTempDirectory("moto-gpx-prefixed").toFile();
  try{
   String xml="<g:gpx xmlns:g='http://www.topografix.com/GPX/1/1'><g:trk><g:trkseg><g:trkpt lat='37' lon='127'><g:time>"+FIRST+"</g:time></g:trkpt><g:trkpt lat='37.001' lon='127'><g:time>"+SECOND+"</g:time></g:trkpt></g:trkseg></g:trk></g:gpx>";
   assertEquals(2,read(xml,dir).count);
  }finally{clean(dir);}
 }
 @Test public void pathologicalNestingIsRejectedBeforeAnImportIsPublished()throws Exception{
  StringBuilder xml=new StringBuilder("<gpx><extensions>");for(int i=0;i<70;i++)xml.append("<nested>");
  for(int i=0;i<70;i++)xml.append("</nested>");xml.append("</extensions></gpx>");rejected(xml.toString());
 }
 @Test public void timeWithNestedMarkupIsNotTreatedAsARecordedTimestamp()throws Exception{
  rejected(document(point("<time>2020-01-02T03:04:05<b/>Z</time>")+point("<time>"+SECOND+"</time>")));
 }
 @Test public void aForeignDirectTimeCannotReplaceAMissingStandardPointTime()throws Exception{
  rejected("<gpx xmlns:x='urn:extension'><trk><trkseg>"+point("<x:time>"+FIRST+"</x:time>")+point("<x:time>"+SECOND+"</x:time>")+"</trkseg></trk></gpx>");
 }

}
