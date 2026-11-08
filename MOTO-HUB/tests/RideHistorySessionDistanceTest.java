package com.motohub.next;

import java.util.Arrays;
import java.util.List;

public class RideHistorySessionDistanceTest {
 private static final class FileSample {
  final String key;final double km;final String name;
  FileSample(String key,double km,String name){this.key=key;this.km=km;this.name=name;}
 }
 private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
 public static void main(String[] args){
  FileSample a=new FileSample("rideA|duke|tour",22.0,"rideA section1");
  FileSample b=new FileSample("rideB|duke|tour",60.0,"rideB");
  FileSample c=new FileSample("rideA|duke|tour",32.0,"rideA section2");
  FileSample differentBike=new FileSample("rideA|other|tour",8.0,"other bike");
  FileSample standalone=new FileSample("",55.0,"standalone");
  FileSample corruptA=new FileSample("badRide|duke|tour",Double.NaN,"unreadable");
  FileSample corruptB=new FileSample("badRide|duke|tour",65.0,"readable companion");
  List<FileSample> input=Arrays.asList(a,b,c,differentBike,standalone,corruptA,corruptB);
  List<FileSample> longRides=RideHistorySessionDistance.select(input,f->f.key,f->f.km,km->km>=50);
  check(longRides.size()==4,"two rideA sections, rideB, standalone should match");
  check(longRides.contains(a)&&longRides.contains(c),"22+32 km sections qualify together as 54km");
  check(longRides.contains(b)&&longRides.contains(standalone),"independent long rides stay visible");
  check(!longRides.contains(differentBike),"other bike may not borrow distances");
  check(!longRides.contains(corruptA)&&!longRides.contains(corruptB),
   "unreadable section invalidates the whole grouped distance");
  List<FileSample> shortRides=RideHistorySessionDistance.select(input,f->f.key,f->f.km,km->km<10);
  check(shortRides.size()==1&&shortRides.contains(differentBike),"filter 10km remains per independent bike");
  check(input.size()==7,"source files never modified");
  System.out.println("PASS RideHistorySessionDistanceTest: 7 checks");
 }
}
