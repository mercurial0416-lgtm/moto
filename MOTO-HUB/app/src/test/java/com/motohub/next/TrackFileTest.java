package com.motohub.next;
import org.junit.*;import static org.junit.Assert.*;import java.io.*;import java.nio.file.*;import javax.xml.parsers.*;
public class TrackFileTest {
 File f;@Before public void setup()throws Exception{f=File.createTempFile("moto-test-",".track");}@After public void cleanup(){f.delete();}
 void put(String data)throws Exception{Files.write(f.toPath(),data.getBytes(java.nio.charset.StandardCharsets.UTF_8));}String point(){return TrackFile.point(37.5,127,5,1700000000000L);}
 @Test public void coordinateBounds(){assertTrue(TrackFile.valid(90,-180,100,1));assertFalse(TrackFile.valid(91,0,1,1));assertFalse(TrackFile.valid(0,181,1,1));assertFalse(TrackFile.valid(Double.NaN,0,1,1));assertFalse(TrackFile.valid(0,0,101,1));assertFalse(TrackFile.valid(0,0,1,0));}
 @Test public void gpxIsXmlAndPreservesRealValues()throws Exception{put(point());ByteArrayOutputStream out=new ByteArrayOutputStream();assertEquals(1,TrackFile.gpx(f,out));org.w3c.dom.Document d=DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(out.toByteArray()));assertEquals("37.5",d.getElementsByTagName("trkpt").item(0).getAttributes().getNamedItem("lat").getNodeValue());assertEquals(1,d.getElementsByTagName("time").getLength());}
 @Test public void tornLastRowRecoversCommittedPoints()throws Exception{put(point()+"37.6,127");assertEquals(1,TrackFile.read(f,(a,b,c,d)->{}));}
 @Test public void corruptedCompleteRowFails()throws Exception{put(point()+"bad\n");assertThrows(Exception.class,()->TrackFile.gpx(f,new ByteArrayOutputStream()));}
 @Test public void reversedTimeFails()throws Exception{put(point()+TrackFile.point(37,127,1,1));assertThrows(IOException.class,()->TrackFile.read(f,(a,b,c,d)->{}));}
 @Test public void emptyNeverInventsCoordinates()throws Exception{assertThrows(IOException.class,()->TrackFile.gpx(f,new ByteArrayOutputStream()));}
 @Test public void invalidFinalCommittedPointFails()throws Exception{put(point()+"200,0,1,1700000000001\n");assertThrows(IOException.class,()->TrackFile.read(f,(a,b,c,d)->{}));}
}
