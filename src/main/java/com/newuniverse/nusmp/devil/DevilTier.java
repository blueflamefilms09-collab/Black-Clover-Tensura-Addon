package com.newuniverse.nusmp.devil;

/**
 * Why: how strong the devil of a contract is (3.48.0). Stored BY NAME, so adding a tier later can never shift a saved value; new tiers are appended only. The tier decides how fast assimilation climbs,
 * how much energy it drains and how large the stat bonuses and the markings of the Weg are ({@link DevilRules}).
 */
public enum DevilTier {
    NONE, LOW, MID, HIGH, SUPREME;

    /** The tier with this name, NONE for anything unknown (a damaged or hand-edited save never throws). */
    public static DevilTier byName(String s) {
        if (s != null) for (DevilTier t : values()) if (t.name().equals(s)) return t;
        return NONE;
    }

    /** The tier at this ordinal, NONE when out of range (the network reads a byte). */
    public static DevilTier byOrdinal(int i) { return i > 0 && i < values().length ? values()[i] : NONE; }

    public String key() { return name().toLowerCase(java.util.Locale.ROOT); }
}
