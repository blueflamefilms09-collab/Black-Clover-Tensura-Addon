package com.newuniverse.nusmp.shishou;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Why: Wielder Registration. A player who is not the wielder and holds Shishou is punished (Slowness X, Wither, the line "Let go of me, you aren't my wielder."), and after ten seconds he tears himself out of
 * their hands: the item leaves their inventory and goes the recovery way ({@link ShishouRecovery}), so it is never lost and the one-per-world cap never breaks. This is the pure timer: ONE check per
 * second per holder (the caller only asks every {@link ShishouRules#THEFT_CHECK} ticks, and only for a player who really holds the item), keyed by the holder's UUID; the answer says what to do now.
 */
public final class ShishouTheft {
    public enum Verdict {
        /** The holder is the wielder (or holds nothing): nothing to do. */
        NONE,
        /** First second of a thief's grip: the line, the effects. */
        PUNISH_FIRST,
        /** Still holding: refresh the effects. */
        PUNISH,
        /** Ten seconds passed: tear the sword away. */
        CONFISCATE
    }

    private final Map<UUID, Long> since = new HashMap<>();

    public Verdict check(UUID holder, boolean isWielder, long now) {
        if (holder == null) return Verdict.NONE;
        if (isWielder) {
            since.remove(holder);
            return Verdict.NONE;
        }
        Long t = since.get(holder);
        if (t == null) {
            since.put(holder, now);
            return Verdict.PUNISH_FIRST;
        }
        if (now - t >= ShishouRules.THEFT_TICKS) {
            since.remove(holder);
            return Verdict.CONFISCATE;
        }
        return Verdict.PUNISH;
    }

    /** Whole seconds left before the sword tears itself away (for the debug line), 0 when the holder is not tracked. */
    public int secondsLeft(UUID holder, long now) {
        Long t = since.get(holder);
        if (t == null) return 0;
        return (int) Math.max(0L, (ShishouRules.THEFT_TICKS - (now - t) + 19) / 20);
    }

    /** A player left, died or let go: the clock is forgotten. */
    public void forget(UUID holder) { since.remove(holder); }

    public void clear() { since.clear(); }

    public int tracked() { return since.size(); }
}
