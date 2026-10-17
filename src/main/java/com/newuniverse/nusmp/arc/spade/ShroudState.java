package com.newuniverse.nusmp.arc.spade;

/**
 * Why: Lucifero's gravity "tears apart spells by increasing the gravity around them". As a rule that is a shroud round him which halves what flies or
 * is cast at him, and the counter is the thing the story used: get close and hit him with something the gravity cannot shred, a blade swung by hand or
 * anti-magic. Enough of that tears the shroud and leaves him staggered. A rule with a counter needs its bookkeeping to be exact (how long the shroud
 * stands, how much melee it takes to tear, what it does to each kind of hit), so it is pure and tested: ranged and magic are halved while it stands,
 * melee and anti-magic are not halved but wear it, and once torn it stays torn until the King raises it again.
 */
public final class ShroudState {
    /** The factor on a ranged or magic hit while the shroud stands. */
    public static final float TORN_APART = 0.5f;
    /** Ticks the staggered King takes extra damage after a tear, and the extra. */
    public static final int STAGGER_TICKS = 40;
    public static final float STAGGER_BONUS = 1.2f;

    private long until;
    private double wear, limit;

    /** Raises the shroud until tick {@code until}; it tears once melee and anti-magic have dealt {@code tearLimit} damage to it. */
    public void raise(long until, double tearLimit) {
        this.until = until;
        this.limit = Math.max(1, tearLimit);
        this.wear = 0;
    }

    public boolean up(long now) { return now < until && wear < limit; }

    public long until() { return until; }

    /** 0..1 how much of the shroud is worn away. */
    public double worn() { return limit <= 0 ? 0 : Math.max(0, Math.min(1, wear / limit)); }

    /** The factor on a hit of {@code kind} right now. */
    public float factor(DamageKind kind, long now) {
        if (!up(now) || kind == null) return 1f;
        return kind == DamageKind.RANGED || kind == DamageKind.MAGIC ? TORN_APART : 1f;
    }

    /**
     * A hit of {@code amount} landed. Melee and anti-magic wear the shroud; returns true on the hit that tears it (and it stays down). Other kinds, and
     * hits when it is not up, do nothing.
     */
    public boolean wear(DamageKind kind, double amount, long now) {
        if (!up(now) || kind == null || !(amount > 0)) return false;
        if (kind != DamageKind.MELEE && kind != DamageKind.ANTI_MAGIC) return false;
        wear += amount;
        if (wear >= limit) {
            until = 0;
            return true;
        }
        return false;
    }

    /** Takes it down (the fight ended). */
    public void drop() {
        until = 0;
        wear = 0;
    }
}
