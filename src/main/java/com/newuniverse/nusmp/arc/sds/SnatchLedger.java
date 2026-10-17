package com.newuniverse.nusmp.arc.sds;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Why: Ban's Snatch steals strength, and "steals" has to be a rule with a ceiling and a way back, not a number that grows without end. This pure
 * ledger holds it. One Snatch takes a share of one player's attack for a while ({@link #takeOf}: 12% at the opening up to 24% at the Gates); a
 * player can be snatched twice at most at once and never of more than {@link #TAKE_MAX_PER_VICTIM} in all; Ban can hold at most {@link #BORROW_MAX}
 * of what he has taken from everybody together, and he only profits {@link #BORROW_RATE} of it; it all ends after {@link #DURATION} ticks. The way
 * back is the point of the design: a snatched player who hits Ban {@link #RETAKE_HITS} real blows inside {@link #RETAKE_WINDOW} ticks wrenches the
 * strength back at once. The share is applied where it matters, to the damage the snatched player does to Ban ({@link #damageFactor}) and to the
 * damage Ban does ({@link #bonus}), so it works for every kind of attack, whatever the player's output is made of.
 *
 * <p>Pure: players are identified by {@link UUID}, time is a tick count passed in.
 */
public final class SnatchLedger {
    /** One Snatch takes this much of a player's attack at the opening, and this much more for each power stage (0 to 4). */
    public static final double TAKE_BASE = 0.12, TAKE_PER_STAGE = 0.03;
    /** The most that can be taken from one player, and the most Ban holds of all of them together. */
    public static final double TAKE_MAX_PER_VICTIM = 0.30, BORROW_MAX = 0.40;
    /** The share of what he holds that turns into his own extra damage. */
    public static final double BORROW_RATE = 0.8;
    /** Ticks a Snatch lasts (8 seconds), how many Snatches can stack on one player, the blows that wrench it back, and the ticks they must come within. */
    public static final int DURATION = 160, MAX_STACKS = 2, RETAKE_HITS = 3, RETAKE_WINDOW = 80;
    /** A blow counts toward the retake only if it does at least this share of Ban's maximum health (a tick of poison does not). */
    public static final double RETAKE_MIN_HIT = 0.004;

    private static final class Entry {
        double taken;
        int stacks, hits;
        long until, windowStart;
    }

    private final Map<UUID, Entry> byVictim = new HashMap<>();
    private final double capPerVictim;

    public SnatchLedger() { this(TAKE_MAX_PER_VICTIM); }

    /** A ledger with another ceiling per player (the server config), 0.05 to 0.60. */
    public SnatchLedger(double capPerVictim) { this.capPerVictim = Math.max(0.05, Math.min(0.60, capPerVictim)); }

    /** The share one Snatch takes at a power stage (0 to 4): 0.12 up to 0.24. */
    public static double takeOf(int stage) { return TAKE_BASE + TAKE_PER_STAGE * Math.max(0, Math.min(4, stage)); }

    /**
     * Takes (up to) {@code want} of a player's attack. Returns the share actually taken: less than asked when the player is near their ceiling or Ban
     * holds nearly all he can, and 0 when the player already carries {@link #MAX_STACKS} Snatches (it fizzles).
     */
    public double steal(UUID victim, long now, double want) {
        purge(now);
        Entry e = byVictim.get(victim);
        if (e != null && e.stacks >= MAX_STACKS) return 0.0;
        double have = e == null ? 0.0 : e.taken;
        double room = Math.min(capPerVictim - have, BORROW_MAX - borrowed(now));
        double add = Math.max(0.0, Math.min(want, room));
        if (add <= 0.0) return 0.0;
        if (e == null) {
            e = new Entry();
            byVictim.put(victim, e);
        }
        e.taken += add;
        e.stacks++;
        e.until = now + DURATION;
        e.hits = 0;
        return add;
    }

    /** The share of this player's attack that is taken right now (0 once it has run out). */
    public double taken(UUID victim, long now) {
        Entry e = byVictim.get(victim);
        return e == null || e.until <= now ? 0.0 : e.taken;
    }

    /** What Ban holds of all of them together, at most {@link #BORROW_MAX}. */
    public double borrowed(long now) {
        double sum = 0.0;
        for (Entry e : byVictim.values()) if (e.until > now) sum += e.taken;
        return Math.min(BORROW_MAX, sum);
    }

    /** Ban's extra damage as a fraction (0 to 0.32): what he holds, times {@link #BORROW_RATE}. */
    public double bonus(long now) { return borrowed(now) * BORROW_RATE; }

    /** The factor on the damage this player does to Ban: 1 minus what is taken from them. */
    public double damageFactor(UUID attacker, long now) { return 1.0 - taken(attacker, now); }

    /**
     * A snatched player hit Ban for {@code damage}. True when this blow was the one that wrenched their strength back (the entry is gone), false
     * otherwise, including when the player carries nothing.
     */
    public boolean hitBack(UUID victim, long now, float damage, float banMaxHealth) {
        Entry e = byVictim.get(victim);
        if (e == null || e.until <= now || !(banMaxHealth > 0f) || damage < banMaxHealth * RETAKE_MIN_HIT) return false;
        if (e.hits == 0 || now - e.windowStart > RETAKE_WINDOW) {
            e.hits = 1;
            e.windowStart = now;
            return false;
        }
        e.hits++;
        if (e.hits < RETAKE_HITS) return false;
        byVictim.remove(victim);
        return true;
    }

    /** Gives one player's strength back now (they died, left, or the fight ended). */
    public void release(UUID victim) { byVictim.remove(victim); }

    /** Forgets every Snatch (a new fight, or the boss is gone). */
    public void clear() { byVictim.clear(); }

    /** How many players carry a live Snatch. */
    public int victims(long now) {
        int n = 0;
        for (Entry e : byVictim.values()) if (e.until > now) n++;
        return n;
    }

    private void purge(long now) {
        Iterator<Map.Entry<UUID, Entry>> it = byVictim.entrySet().iterator();
        while (it.hasNext()) if (it.next().getValue().until <= now) it.remove();
    }
}
