package com.newuniverse.nusmp.elite.nations.rep;

import java.util.Locale;

/**
 * Why: the reputation tiers ("reputation tiers, hated ... revered") of a player with a nation, pure. A value runs from -1000 to +1000; each tier starts at its {@code min}. The effects are plain numbers the
 * rest of the system reads: the percent a nation's shops charge this player, the highest contract tier they may take, whether the nation's guards treat them as an enemy and whether they may use the nation's
 * board at all.
 */
public enum ReputationTier {
    HATED(-1000, 160, 0, true, false),
    HOSTILE(-750, 135, 0, true, false),
    UNFRIENDLY(-300, 115, 1, false, false),
    NEUTRAL(-50, 100, 2, false, true),
    FRIENDLY(100, 95, 3, false, true),
    HONORED(350, 90, 4, false, true),
    REVERED(700, 80, 5, false, true);

    public static final int MIN = -1000, MAX = 1000;

    public final int min;
    public final int shopPercent;
    public final int maxContractTier;
    public final boolean guardsHostile;
    public final boolean mayUseBoard;

    ReputationTier(int min, int shopPercent, int maxContractTier, boolean guardsHostile, boolean mayUseBoard) {
        this.min = min;
        this.shopPercent = shopPercent;
        this.maxContractTier = maxContractTier;
        this.guardsHostile = guardsHostile;
        this.mayUseBoard = mayUseBoard;
    }

    public static ReputationTier of(int value) {
        ReputationTier out = HATED;
        for (ReputationTier t : values()) if (value >= t.min) out = t;
        return out;
    }

    public static int clamp(int value) { return Math.max(MIN, Math.min(MAX, value)); }

    /** Value needed for the next tier, or -1 at the top. */
    public int nextMin() { return ordinal() + 1 < values().length ? values()[ordinal() + 1].min : -1; }

    public String id() { return name().toLowerCase(Locale.ROOT); }

    public static ReputationTier parse(String s) {
        if (s == null) return NEUTRAL;
        try { return valueOf(s.trim().toUpperCase(Locale.ROOT)); } catch (IllegalArgumentException e) { return NEUTRAL; }
    }

    /** One step of decay toward zero: {@code amount} per call, never crossing zero. */
    public static int decay(int value, int amount) {
        if (amount <= 0) return value;
        if (value > 0) return Math.max(0, value - amount);
        if (value < 0) return Math.min(0, value + amount);
        return 0;
    }
}
