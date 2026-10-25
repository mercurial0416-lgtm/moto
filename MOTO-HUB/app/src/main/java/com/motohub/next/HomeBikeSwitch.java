package com.motohub.next;
/** Keep the home bike switch discoverable only when there is a choice. */
final class HomeBikeSwitch {
 private HomeBikeSwitch() {}
 static boolean visible(int bikeCount){return bikeCount>1;}
 static String label(int bikeCount){return visible(bikeCount)?"⇄ 바이크 변경 ▾":"MY BIKE";}
}
