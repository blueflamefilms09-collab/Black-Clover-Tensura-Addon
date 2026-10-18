package com.newuniverse.nusmp.heroweapon.curse;

import com.newuniverse.nusmp.heroweapon.Sin;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Why: the whole curse of one wielder is a handful of numbers and a list of sins, and the rules that move them ({@link CurseMachine}) must be testable without a
 * game. This is that record, with no Minecraft type in it: the despair and the corruption (0 to 1000), the series unlocked (sin ids), the offer that is open
 * (which sin and since when), when the next offer may open, the Overrun and the weakness (as game times), the permanent tax in hearts, how many times the
 * Purification Charm has given hearts back, the clock readings the rules need (the last despair gain for the calm timer, the last rest already honoured) and
 * the time each cause was last counted (the rate limits). {@link CurseStore} reads and writes it in {@code HeroBond.extra("curse")}, where the forms system
 * finds the {@code series} list. Server thread only; the fields are package-private because only the machine and the store change them.
 */
public final class CurseState {
    int despair;
    int corruption;
    final Set<String> series = new LinkedHashSet<>();
    String offered = "";
    long offeredAt;
    long nextOfferAt;
    long overrunUntil;
    long weakUntil;
    int tax;
    int refunds;
    long lastGain;
    long lastRest;
    final Map<String, Long> causeAt = new HashMap<>();

    public int despair() { return despair; }

    public int corruption() { return corruption; }

    /** The sin ids whose Curse Series this wielder has embraced, in the order they were embraced. */
    public Set<String> series() { return series; }

    public boolean hasSeries(Sin sin) { return sin != null && series.contains(sin.id()); }

    /** The sin of the open offer, or null (the offer may have run out: ask {@link CurseMachine#offerOpen}). */
    public Sin offered() { return offered.isEmpty() ? null : Sin.byId(offered); }

    /** The hearts of maximum health the series have taken for good. */
    public int tax() { return tax; }

    public long overrunUntil() { return overrunUntil; }

    public long weakUntil() { return weakUntil; }

    /** True when nothing is set, so there is nothing worth saving. */
    public boolean isDefault() {
        return despair == 0 && corruption == 0 && series.isEmpty() && offered.isEmpty() && offeredAt == 0 && nextOfferAt == 0 && overrunUntil == 0 && weakUntil == 0
                && tax == 0 && refunds == 0 && lastGain == 0 && lastRest == 0 && causeAt.isEmpty();
    }
}
