package com.motohub.next;
/** Bounded, user-enabled automatic ride watch. Never an unbounded GPS scan. */
final class CardoStandbyPolicy {
 static long idleWindowMs(boolean gpsFallback){return gpsFallback?900_000L:600_000L;}
 private CardoStandbyPolicy(){}
}
